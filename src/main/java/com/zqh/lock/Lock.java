package com.zqh.lock;

import java.util.concurrent.TimeUnit;

public interface Lock {

    boolean tryLock(String name);

    boolean tryLock(String name, long timeout, TimeUnit unit);

    void lock(String name);

    void unlock(String name);

}
