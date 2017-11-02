package com.zqh.lock.redis;

import com.zqh.lock.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RedisLock implements Lock {

    private static final String NAME_SPACE = "redis-lock:";
    private static final String REENTRANT_NAME_SPACE = "reentrant-redis-lock:";
    private static final ThreadLocal<AtomicInteger> reentrantInfoContext = new ThreadLocal<>();

    @Autowired
    @Qualifier("stringRedisTemplate")
    private RedisTemplate stringRedisTemplate;

    @Value("${redis.lock.timeout:300}")
    private long timeout;

    @Value("${instanceId:}")
    private String instanceId;

    public boolean tryReentrantLock(String name) {
        return false;
    }

    @Override
    public boolean tryLock(String name) {
        return (Boolean) stringRedisTemplate.execute(new RedisCallback() {
            @Nullable
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                try {
                    return connection.set(actureName(NAME_SPACE, name), actureValue(), Expiration.seconds(timeout), RedisStringCommands.SetOption.SET_IF_ABSENT);
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    @Override
    public boolean tryLock(String name, long timeout, TimeUnit unit) {
        Long startTime = System.currentTimeMillis();
        timeout = TimeUnit.SECONDS.equals(unit) ? unit.toMillis(timeout) : timeout;
        while(!tryLock(name)) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis()-startTime >= timeout) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void lock(String name) {
        while (!tryLock(name)) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unlock(String name) {
        String generaterName = generaterName(NAME_SPACE, name);
        String value = (String) stringRedisTemplate.opsForValue().get(generaterName);
        if (null == value || !generaterValue().equals(value)) {
            return;
        }
        stringRedisTemplate.delete(generaterName);

    }

    private byte[] actureName(String name) {
        return stringRedisTemplate.getKeySerializer().serialize(name);
    }

    private byte[] actureValue() {
        return stringRedisTemplate.getValueSerializer().serialize(generaterValue());
    }

    private String generaterName(String name, String nameSpace) {
        return nameSpace + name;
    }

    private String generaterValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(instanceId)
                .append("[")
                .append(Thread.currentThread().getName())
                .append("]-")
                .append(Thread.currentThread().getId());
        return sb.toString();
    }
}
