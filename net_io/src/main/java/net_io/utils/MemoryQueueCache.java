package net_io.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MemoryQueueCache<T> {
	private ConcurrentHashMap<String, CacheInfo<T>> dataMap = new ConcurrentHashMap<String, CacheInfo<T>>();
	private ConcurrentLinkedQueue<QueueInfo> queueList = new ConcurrentLinkedQueue<QueueInfo>();
	private long runPeriod = 1000;
	private Timer timer;
	private long timeout;
	
	public MemoryQueueCache(Timer timer, long timeout) {
		this.timer = timer;
		this.timeout = timeout;
		//运行定时器，清理过期数据
		runTimer();
	}
	
	public int size() {
		return dataMap.size();
	}
	
	public void set(String key, T obj) {
		set(key, obj, null);
	}
	
	public void set(String key, T obj, RemoveMonitor callback) {
		long expireTime = System.currentTimeMillis() + timeout;		
		dataMap.put(key, new CacheInfo<T>(obj, expireTime, callback));
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
	
	private void runTimer() {
		final MemoryQueueCache<T> that = this;
		this.timer.schedule(new TimerTask() {

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
							that.queueList.remove(); //先移除
							CacheInfo<T> cacheInfo = that.dataMap.get(info.key);
							if(cacheInfo != null && (cacheInfo.expireTime < min || cacheInfo.expireTime > max)) {
								that.dataMap.remove(info.key);
								if(cacheInfo.removeMonitor != null) {
									//callback若抛出RuntimeException，则中断本次任务，等下一个定时任务中继续下一条
									cacheInfo.removeMonitor.callback(info.key);
								}
							}
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
	
	private static class QueueInfo {
		long expireTime;
		String key;
		QueueInfo(String key, long expireTime) {
			this.key = key;
			this.expireTime = expireTime;
		}
	}

	private static class CacheInfo<T2> {
		long expireTime;
		T2 cacheData;
		RemoveMonitor removeMonitor;
		CacheInfo(T2 obj, long expireTime, RemoveMonitor removeMonitor) {
			this.cacheData = obj;
			this.expireTime = expireTime;
			this.removeMonitor = removeMonitor;
		}
	}
	
	public static interface RemoveMonitor {
		public void callback(String removeKey);
	}
}
