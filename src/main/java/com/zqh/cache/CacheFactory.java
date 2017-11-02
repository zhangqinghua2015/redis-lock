package com.zqh.cache;

import com.zqh.base.SpringContextHolder;


public class CacheFactory {
    
    public static CacheClient getCacheClient(){
       return (CacheClient) SpringContextHolder.getBean("cacheClient");
    }
}
