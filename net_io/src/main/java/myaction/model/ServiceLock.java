package myaction.model;

import myaction.utils.LogUtil;
import net_io.myaction.CheckException;

public class ServiceLock {
	/** 同步锁KEY默认连接符 **/
	private static final String LOCK_KEY_SPARATOR = "^-^";
	/** 同步锁KEY的前缀 **/
	private static final String LOCK_KEY_PREFIX = "|LOCK|";
	/** 最大等待时间：120秒 **/
	private static final long MAX_WAIT_TIME = 120 * 1000;
	/** 默认锁定时间：180秒 **/
	private static final long DEFAULT_LOCK_TIME = 180 * 1000;
	/** 默认等待时间：5秒 **/
	private static final long DEFAULT_WAIT_TIME = 5 * 1000;
	/** 检查锁周期：10毫秒 **/
	private static final long CHECK_PERIOD = 10;
	/** 锁存储器（带有唯一性约束） **/
	private SignalStorage storage;
	
	public ServiceLock(SignalStorage storage) {
		this.storage = storage;
	}
	
	public Lock lock(String prefixKey, String subKey) throws LockException {
		String key = prefixKey + LOCK_KEY_SPARATOR + subKey;
		return lock(key, DEFAULT_LOCK_TIME, DEFAULT_WAIT_TIME);
	}

	public Lock lock(String key) throws LockException {
		return lock(key, DEFAULT_LOCK_TIME, DEFAULT_WAIT_TIME);
	}
	
	public Lock lock(String key, long lockTime, long waitTime) throws LockException {
		String lockKey = LOCK_KEY_PREFIX + key;
		waitTime = Math.min(waitTime, MAX_WAIT_TIME);
		long startTime = System.currentTimeMillis();
		long tryTimes = 0;
		while(true) {
			long num = storage.increaseAndGet(lockKey, lockTime);
			if(num == 1) {
				break; //锁定成功
			}
			tryTimes++;
			long pastTime = System.currentTimeMillis() - startTime;
			if(pastTime >= waitTime) {
				throw new LockException(711, "Can not lock: " + key + "! try times: " + tryTimes + ", past time: "+pastTime + "ms.");
			}
			if(tryTimes == 1 || tryTimes == 10 || tryTimes == 100 || (tryTimes % 1000 == 0)) {
				LogUtil.logInfo("Lock Busy! KEY: " + key + ", try times: " + tryTimes + ", past time: "+pastTime + "ms.");
			}
			try {
				Thread.sleep(CHECK_PERIOD);
			} catch (InterruptedException e) {
				throw new LockException(712, "Can not lock: " + key + "! " + e.toString());
			}
		}
		return new Lock(storage, lockKey);
	}
	
	public static interface SignalStorage {
		/**
		 * 根据 key 自增后取值。若KEY不存在，可返回1.
		 * @param key 操作KEY（唯一）
		 * @param timeout 过期时间；单位：毫秒；仅当首次加入KEY时，设置过期时间
		 * @return 自增加后的数组
		 */
		public long increaseAndGet(String key, long timeout);
		
		/**
		 * 删除KEY
		 * @param key 操作KEY（唯一）
		 */
		public void remove(String key);

	}
	
	public class Lock {
		private SignalStorage storage;
		private String key;
		private Lock(SignalStorage storage, String key) {
			this.storage = storage;
			this.key = key;
		}
		public void release() {
			storage.remove(key);
		}
	}
	
	public static class LockException extends CheckException {
		private static final long serialVersionUID = -6701751231313693735L;

		public LockException(int error, String reason) {
			super(error, reason);
		}
		
	}
}
