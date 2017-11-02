package com.zqh.cache.redis;

import com.zqh.cache.CacheClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component("cacheClient")
public class RedisCacheClient implements CacheClient {

    private static final String PUT_IF_ABSENT = "local a = redis.call('SETNX', ARGV[1], ARGV[2]) \n return a == 1";

    private static final String PUT_IF_ABSENT_EX = "local a = redis.call('SETNX', ARGV[1], ARGV[2])\n" +
                                                    "if (a == 0) then return false\n" +
                                                    "else \n" +
                                                    "    a = redis.call(ARGV[3], ARGV[1], ARGV[4])\n" +
                                                    "    if (a == 0) then return false end\n" +
                                                    "end \n" +
                                                    "return true";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisTemplate stringRedisTemplate;


    @Override
    public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
        if (null == key || null == value) {
            return;
        }
        try {
            if (timeToLive > 0) {
                redisTemplate.opsForValue().set(generateKey(key, namespace), value, timeToLive,timeUnit);
            } else {
                redisTemplate.opsForValue().set(generateKey(key, namespace), value);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists(String key, String namespace) {
        if (null == key) {
            return false;
        }
        try {
            return redisTemplate.hasKey(generateKey(key, namespace));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Object get(String key, String namespace) {
        if (null == key) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(generateKey(key, namespace));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object get(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
        if (null == key) {
            return null;
        }
        try {
            String actureKey = generateKey(key, namespace);
            redisTemplate.expire(actureKey, timeToIdle, timeUnit);
            return redisTemplate.opsForValue().get(actureKey);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void delete(String key, String namespace) {
        if (null == key) {
            return;
        }
        try {
            redisTemplate.delete(generateKey(key, namespace));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace) {
        if (null == map) {
            return;
        }
        try {
            final Map<byte[], byte[]> actualMap = new HashMap<byte[], byte[]>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                actualMap.put(redisTemplate.getKeySerializer().serialize(generateKey(entry.getKey(), namespace)),
                        redisTemplate.getValueSerializer().serialize(entry.getValue()));
            }
            redisTemplate.execute(new RedisCallback() {
                @Nullable
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    connection.multi();
                    try {
                        connection.mSet(actualMap);
                        if (timeToLive > 0) {
                            if (TimeUnit.SECONDS.equals(timeUnit)) {
                                actualMap.forEach(new BiConsumer<byte[], byte[]>() {
                                    @Override
                                    public void accept(byte[] bytes, byte[] bytes2) {
                                        connection.expire(bytes, timeToLive);
                                    }
                                });
                            } else {
                                actualMap.forEach(new BiConsumer<byte[], byte[]>() {
                                    @Override
                                    public void accept(byte[] bytes, byte[] bytes2) {
                                        connection.pExpire(bytes, timeToLive);
                                    }
                                });
                            }
                        }
                        connection.exec();
                    } catch (Exception e) {
                        connection.discard();
                        e.printStackTrace();
                    }
                    return null;
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> mget(Collection<String> keys, String namespace) {
        if (null == keys) {
            return null;
        }
        try {
            final List<byte[]> actureKeys = new ArrayList<>();
            keys.forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actureKeys.add(redisTemplate.getKeySerializer().serialize(generateKey(s, namespace)));
                }
            });
            List<byte[]> values = (List<byte[]>) redisTemplate.execute(new RedisCallback() {
                @Nullable
                @Override
                public List<byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
                    return connection.mGet(actureKeys.toArray(new byte[0][0]));
                }
            });
            Map<String, Object> map = new HashMap<>();
            int index = 0;
            for(String key : keys) {
                map.put(key, redisTemplate.getValueSerializer().deserialize(values.get(index)));
                index++;
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void mdelete(Collection<String> keys, String namespace) {
        if (null == keys) {
            return;
        }
        try {
            final List<byte[]> actureKeys = new ArrayList<>();
            keys.forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actureKeys.add(redisTemplate.getKeySerializer().serialize(generateKey(s, namespace)));
                }
            });
            redisTemplate.execute(new RedisCallback() {
                @Nullable
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    connection.del(actureKeys.toArray(new byte[0][0]));
                    return null;
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean containsKey(String key, String namespace) {
        if (null == key) {
            return false;
        }
        try {
            return redisTemplate.hasKey(generateKey(key, namespace));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
        if (null == key || value == key) {
            return false;
        }
        try {
            byte[] actureKey = redisTemplate.getKeySerializer().serialize(generateKey(key, namespace));
            byte[] actureValue = redisTemplate.getValueSerializer().serialize(value);
            return (Boolean) redisTemplate.execute(new RedisCallback() {
                @Nullable
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    try {
                        return connection.set(actureKey, actureValue, Expiration.from(timeToLive, timeUnit), RedisStringCommands.SetOption.SET_IF_ABSENT);
                    } catch(Exception e) {
                        connection.discard();
                        e.printStackTrace();
                        return false;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
        if (null == key) {
            return -1;
        }
        try {
            String actrualkey = generateKey(key, namespace);
            long result = redisTemplate.opsForValue().increment(actrualkey, delta);
            if (timeToLive > 0) {
                redisTemplate.expire(actrualkey, timeToLive, timeUnit);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean supportsTimeToIdle() {
        return false;
    }

    @Override
    public boolean supportsUpdateTimeToLive() {
        return true;
    }

    @Override
    public void invalidate(String namespace) {

    }

    private String generateKey(String key, String namespace) {
        if (StringUtils.isNotBlank(namespace)) {
            StringBuilder sb = new StringBuilder(namespace.length() + key.length() + 1);
            sb.append(namespace);
            sb.append(':');
            sb.append(key);
            return sb.toString();
        } else {
            return key;
        }

    }
}
