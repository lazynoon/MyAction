package net_io.utils.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import net_io.utils.thread.RunnerThread.ThreadInfo;

public class MyThreadPool {
	private RunnerThread[] threads = null;
	private int minPoolSize;
	private int maxPoolSize;
	private ConcurrentLinkedQueue<RunTask> taskQueue = new ConcurrentLinkedQueue<RunTask>();
	private AtomicLong rebuildThreadCount = new AtomicLong(0);
	
	public MyThreadPool(int minPoolSize, int maxPoolSize) {
		init(minPoolSize, maxPoolSize, 60*1000);
	}
	public MyThreadPool(int minPoolSize, int maxPoolSize, long keepAliveTime) {
		init(minPoolSize, maxPoolSize, keepAliveTime);
	}

	private void init(int minPoolSize, int maxPoolSize, long keepAliveTime) {
		if(minPoolSize < 1) {
			minPoolSize = 0;
		}
		this.minPoolSize = minPoolSize;
		this.maxPoolSize = maxPoolSize;
		if(this.maxPoolSize < this.minPoolSize) {
			this.maxPoolSize = this.minPoolSize;
		}
		//创建线程对象
		threads = new RunnerThread[this.maxPoolSize];
		for(int i=0; i<threads.length; i++) {
			long keepTime = keepAliveTime;
			if(i < this.minPoolSize) {
				keepTime = -1;
			}
			threads[i] = new RunnerThread(this, i, keepTime);
		}
	}
	
	/**
	 * 替换将要回收的线程
	 * @param indexOfPool
	 * @param keepAliveTime
	 */
	protected void replaceDyingThread(int indexOfPool, long keepAliveTime, ThreadInfo threadInfo) {
		threads[indexOfPool] = new RunnerThread(this, indexOfPool, keepAliveTime, threadInfo);
		rebuildThreadCount.incrementAndGet();
	}

	public void execute(Runnable runner) {
		RunTask task = new RunTask(runner);
		taskQueue.add(task);
		int notifySize = Math.min(5, taskQueue.size()); //单次最大通知5个
		notifyThreadRunner(notifySize);
	}
	
	public void shutdownAll() {
		if(threads != null) {
			for(RunnerThread thread : threads) {
				thread.shutdown();
			}
		}
	}
	
	public static class RunTask {
		Runnable runner;
		long createTime;
		public RunTask(Runnable runner) {
			this.runner = runner;
			this.createTime = System.currentTimeMillis();
		}
		public Runnable getRunner() {
			return runner;
		}
		public long getCreateTime() {
			return createTime;
		}
		
		
	}
	
	/**
	 * 获取任务
	 * @return Retrieves and removes the head of this queue, or returns null if this queue is empty.
	 */
	public RunTask takeRunTask() {
		return taskQueue.poll();
	}
	
	/**
	 * 获取任务队列中的数量
	 * @return
	 */
	public int getTaskQueueSize() {
		return taskQueue.size();
	}
	
	/**
	 * 获取重建线程的次数
	 */
	public long getRebuildThreadCount() {
		return rebuildThreadCount.get();
	}

	private int notifyThreadRunner(int taskSize) {
		int middlePos = (int)(Math.random() * minPoolSize);
		middlePos = 0; //FIXME: 线程状态检查正常后，移除此条语句
		int i = 0;
		int assignCount = 0;
		for(i=middlePos; i<minPoolSize; i++) {
			if(threads[i].assignFree()) {
				assignCount++;
				if(assignCount >= taskSize) {
					return assignCount;
				}
			}
		}
		for(i=middlePos-1; i>=0; i--) {
			if(threads[i].assignFree()) {
				assignCount++;
				if(assignCount >= taskSize) {
					return assignCount;
				}
			}
		}
		for(i=minPoolSize; i<maxPoolSize; i++) {
			if(threads[i].assignFree()) {
				assignCount++;
				if(assignCount >= taskSize) {
					return assignCount;
				}
			}
		}
		return assignCount;
	}
	
	public List<RunnerThread.ThreadInfo> listThreadInfo() {
		List<RunnerThread.ThreadInfo> list = new ArrayList<RunnerThread.ThreadInfo>();
		for(int i=0; i<threads.length; i++) {
			list.add(threads[i].getThreadInfo());
		}
		return list;
	}
}
