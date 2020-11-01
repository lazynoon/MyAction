package myaction.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net_io.utils.NetLog;

public class MemoryCache<T> {
	private ConcurrentHashMap<String, CacheInfo<T>> dataMap = new ConcurrentHashMap<String, CacheInfo<T>>();
	private ConcurrentLinkedQueue<QueueInfo> queueList = new ConcurrentLinkedQueue<QueueInfo>();
	private long runPeriod = 1000;
	private RunTimer timer;
	private long timeout;
	
	public MemoryCache(RunTimer timer, long timeout) {
		this.timer = timer;
		this.timeout = timeout;
		//运行定时器，清理过期数据
		runTimer();
	}
	
	public int size() {
		return dataMap.size();
	}
	
	public void set(String key, T obj) {
		long expireTime = System.currentTimeMillis() + timeout;
		dataMap.put(key, new CacheInfo<T>(obj, expireTime));
		queueList.offer(new QueueInfo(key, expireTime));
	}
	
	public T get(String key) {
		CacheInfo<T> obj = dataMap.get(key);
		if(obj == null) {
			return null;
		}
		long min = System.currentTimeMillis();
		long max = System.currentTimeMillis() + timeout;
		if(obj.expireTime < min || obj.expireTime > max) {
			remove(key);
			return null;
		} else {
			return obj.cacheData;
		}
		
	}
	
	public T remove(String key) {
		CacheInfo<T> obj = dataMap.remove(key);
		if(obj == null) {
			return null;
		}
		return obj.cacheData;
	}
	
	private static class CacheInfo<T2> {
		long expireTime;
		T2 cacheData;
		CacheInfo(T2 obj, long expireTime) {
			this.cacheData = obj;
			this.expireTime = expireTime;
		}
	}
	
	private static class QueueInfo {
		long expireTime;
		String key;
		QueueInfo(String key, long expireTime) {
			this.key = key;
			this.expireTime = expireTime;
		}
	}

	private void runTimer() {
		final MemoryCache<T> that = this;
		this.timer.schedule(new RunTask() {

			@Override
			public void run() {
				long min = System.currentTimeMillis();
				long max = System.currentTimeMillis() + timeout;
				try {
					while(true) {
						QueueInfo info = that.queueList.peek();
						if(info == null) {
							break;
						}
						if(info.expireTime < min || info.expireTime > max) {
							CacheInfo<T> cacheInfo = that.dataMap.get(info.key);
							if(cacheInfo != null && (cacheInfo.expireTime < min || cacheInfo.expireTime > max)) {
								that.dataMap.remove(info.key);								
							}
							that.queueList.remove();
						} else {
							break;
						}
					}
				} catch(Exception e) {
					NetLog.logError(e);
				}
			}
			
		}, runPeriod, runPeriod);
	}
	
}
