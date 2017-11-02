package com.zqh;

import com.zqh.base.SpringContextHolder;
import com.zqh.lock.redis.RedisLock;
import com.zqh.test.BaseTest;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class MainTest extends BaseTest {

    public static void main(String[] args) {
//        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:spring/applicationContext.xml");
//        RedisLock redisLock = (RedisLock) SpringContextHolder.getBean(RedisLock.class);
////        System.out.println(redisLock.tryLock("try"));
////        System.out.println(redisLock.tryLock("try"));
//
//        CyclicBarrier cyclicBarrier = new CyclicBarrier(5);
//        for (int i=0; i<5; i++) {
//            new LockTestThread(cyclicBarrier, redisLock).start();
//        }
        System.out.println(TimeUnit.SECONDS.toSeconds(10));
    }

    @Test
    public void testPut() throws InterruptedException {
        RedisLock redisLock = (RedisLock) SpringContextHolder.getBean(RedisLock.class);
        System.out.println(redisLock.tryLock("try"));
//        redisLock.lock("try");
//        Long start = System.currentTimeMillis();
//        System.out.println(redisLock.tryLock("try", 40, TimeUnit.SECONDS));
//        System.out.println(System.currentTimeMillis()-start);

        CyclicBarrier cyclicBarrier = new CyclicBarrier(5);
        for (int i=0; i<5; i++) {
            new LockTestThread(cyclicBarrier, redisLock).start();
        }
        TimeUnit.SECONDS.sleep(2);

    }

    public static class LockTestThread extends Thread {

        private static final String LOCK_NAME = "lockTest";
        private CyclicBarrier cyclicBarrier;
        private RedisLock redisLock;

        public LockTestThread(CyclicBarrier cyclicBarrier, RedisLock redisLock) {
            this.cyclicBarrier = cyclicBarrier;
            this.redisLock = redisLock;
        }

        @Override
        public void run() {
            String name =Thread.currentThread().getName();
            try {
                cyclicBarrier.await();
                System.out.println(name + "=====开始");
                while (!redisLock.tryLock(LOCK_NAME)) {
                    redisLock.unlock(LOCK_NAME);
                    TimeUnit.SECONDS.sleep(1);
                }
                System.out.println(name + "=====获取到锁");
                TimeUnit.SECONDS.sleep(5);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                System.out.println(name + "=====释放锁");
                redisLock.unlock(LOCK_NAME);
            }

        }

    }

}
