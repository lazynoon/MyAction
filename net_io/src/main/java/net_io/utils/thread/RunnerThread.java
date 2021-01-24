package net_io.utils.thread;

import net_io.utils.NetLog;
import net_io.utils.thread.MyThreadPool.RunTask;

public class RunnerThread extends Thread {
	public enum Status{INIT, FREE, ASSIGNED, RUNNING, SHUTDOWN};
	private boolean shutdown = false;
	
	private MyThreadPool threadPool;
	private int indexOfPool;
	private long keepAliveTime;
	private ThreadInfo threadInfo;
	
	private long maxWaitTime = 120 * 1000;
	private long lastActiveTime = 0;
	
	protected RunnerThread(MyThreadPool threadPool, int indexOfPool, long keepAliveTime) {
		this.threadPool = threadPool;
		this.indexOfPool = indexOfPool;
		this.keepAliveTime = keepAliveTime;
		this.threadInfo = new ThreadInfo();
	}
	
	protected RunnerThread(MyThreadPool threadPool, int indexOfPool, long keepAliveTime, ThreadInfo threadInfo) {
		this.threadPool = threadPool;
		this.indexOfPool = indexOfPool;
		this.keepAliveTime = keepAliveTime;
		threadInfo.status = Status.INIT;
		this.threadInfo = threadInfo;
	}
	
	protected void shutdown() {
		shutdown = true;
		synchronized(this) {
			this.notify();
		}
	}
	
	public boolean assignFree() {
		boolean free = threadInfo.status == Status.INIT || threadInfo.status == Status.FREE;
		if(free) {
			threadInfo.assignNsTime = System.nanoTime();
			threadInfo.status = Status.ASSIGNED;
			synchronized(this) {
				this.lastActiveTime = System.currentTimeMillis();
				if(this.isAlive() == false) {
					this.start();
				} else {
					this.notify();
				}
			}
		}
		return free;
	}
	
	public ThreadInfo getThreadInfo() {
		return threadInfo;
	}
	
	public void run() {
		while(!shutdown) {
			try {
				RunTask task = threadPool.takeRunTask();
				if(task == null) {
					//检查是否保持线程活跃状态
					if(keepAliveTime >= 0 && (System.currentTimeMillis()-lastActiveTime) > keepAliveTime) {
						threadInfo.status = Status.SHUTDOWN;
						break;
					}
					long waitTime = Math.round(maxWaitTime * Math.random()) + 1000;
					synchronized(this) {
						threadInfo.resetFree();
						wait(waitTime);
					}
					continue;
				}
				threadInfo.runTask = task;
				threadInfo.runCount++;
				threadInfo.status = Status.RUNNING;
				//执行线程任务
				task.runner.run();
				this.lastActiveTime = System.currentTimeMillis();
			} catch(Exception e) {
				this.lastActiveTime = System.currentTimeMillis();
				NetLog.logError(e);
			} finally {
				threadInfo.runTask = null;
			}
		}
		//非关闭模式，则新开线程
		if(!shutdown) {
			try {
				threadPool.replaceDyingThread(indexOfPool, keepAliveTime, threadInfo);
			} catch(Exception e) {
				NetLog.logError(e);
			}
		}
	}

	

	public static class ThreadInfo {
		private Status status = Status.INIT;
		private long runCount = 0;
		private long assignNsTime = 0;
		private RunTask runTask = null;
		
		private void resetFree() {
			status = Status.FREE;
			assignNsTime = 0;
		}

		public String getStatus() {
			return status.toString();
		}

		/**
		 * 运行时间，单位毫秒
		 */
		public long getRunNsTime() {
			if(assignNsTime == 0) {
				return 0;
			}
			return System.nanoTime() - assignNsTime;
		}
		
		/**
		 * 获取运行次数
		 */
		public long getRunCount() {
			return runCount;
		}

		public RunTask getRunTask() {
			return runTask;
		}
	}
}