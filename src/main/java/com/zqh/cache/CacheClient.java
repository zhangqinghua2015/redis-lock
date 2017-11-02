package com.zqh.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CacheClient {

	String DEFAULT_TIME_TO_LIVE = "3600";

	String DEFAULT_TIME_TO_IDLE = "-1";

	void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	boolean exists(String key, String namespace);

	Object get(String key, String namespace);

	Object get(String key, String namespace, int timeToIdle, TimeUnit timeUnit);

	void delete(String key, String namespace);

	void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace);

	Map<String, Object> mget(Collection<String> keys, String namespace);

	void mdelete(Collection<String> keys, String namespace);

	boolean containsKey(String key, String namespace);

	boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace);

	boolean supportsTimeToIdle();

	boolean supportsUpdateTimeToLive();
	
	void invalidate(String namespace);

}
