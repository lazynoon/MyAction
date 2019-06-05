package myaction.plog;

import myaction.utils.DateUtil;
import myaction.utils.LogUtil;

class LogThread extends Thread {
	private static int id = 0;
	private Object single = new Object();
	private long saveMinPeriod = 100; // 100ms
	private LogService service = null;
	private boolean runnable = false;
	
	//限制1天之后，发生的错误，最多保存100次到DB中。避免无限循环记录DB错误
	private long lastErrorOccurTime = 0;
	private int lastErrorCount = 0;
	private static final long errorCountCacheTime = 86400 * 1000;
	private static final long errorCountLimitSaveDB = 100;
	
	protected LogThread(LogService service) {
		this.service = service;
		this.setDaemon(true);
	}
	
	public void start() {
		this.setName("LogService"+(++id));
		runnable = true;
		super.start();
	}
	
	public void run() {
		long total = 0;
		do {
			boolean allowSaveDB = false;
			try {
				long startTime = System.currentTimeMillis();
				int count = service.runOnce();
				total += count;
				long costTime = (System.currentTimeMillis() - startTime);
				long waitTime = saveMinPeriod - costTime;
				boolean logInfoFlag = false;
				if(!runnable) {
					logInfoFlag = true;
				} else if(count > 0 && total % 10000 == 0) {
					logInfoFlag = true;
				} else if(costTime > 500) {
					//一段时间内，可保存到DB的次数记录（1/2）
					if(System.currentTimeMillis() - lastErrorOccurTime > errorCountCacheTime) {
						lastErrorOccurTime = System.currentTimeMillis();
						lastErrorCount = 0;
					}
					if(lastErrorCount < errorCountLimitSaveDB) {
						lastErrorCount++;
						logInfoFlag = true;
					}
				}
				if(logInfoFlag) {
					String msg = "[LOG"+id+"] "+DateUtil.getDateTime()+" - Total: "+total+", Last: "+count+", CostTime: "+costTime+"ms";
					LogUtil.logInfo(msg);
				}
				if(waitTime > 0) {
					synchronized(single) {
						single.wait(waitTime);
					}
				}
			} catch (Exception e) {
				//一段时间内，可保存到DB的次数记录（1/1）
				if(System.currentTimeMillis() - lastErrorOccurTime > errorCountCacheTime) {
					lastErrorOccurTime = System.currentTimeMillis();
					lastErrorCount = 0;
				}
				if(lastErrorCount < errorCountLimitSaveDB) {
					lastErrorCount++;
					allowSaveDB = true;
				}
				LogUtil.logError("Save Log Error", e, allowSaveDB);
			}
		} while(runnable);
	}
	
	public void runAndStop() {
		synchronized(single) {
			single.notify();
			runnable = false;
		}
	}
}
