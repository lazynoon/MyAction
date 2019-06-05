package myaction.model;

import java.util.concurrent.atomic.AtomicLong;

abstract public class RunTask {
	private static final AtomicLong serialNo = new AtomicLong(0);
	protected long taskId = serialNo.incrementAndGet();
	protected String taskName = null;
	protected long maxExecTime = 0;
	protected String status = null;
	protected long runStartTime = 0;
	public RunTask() {}
	public RunTask(long maxExecTime) {
		this.maxExecTime = maxExecTime;
	}
	public RunTask(String taskName) {
		this.taskName = taskName;
	}
	public RunTask(String taskName, long maxExecTime) {
		this.taskName = taskName;
		this.maxExecTime = maxExecTime;
	}
	abstract public void run() throws Exception;
	protected void _execute() throws Exception {
		runStartTime = System.currentTimeMillis();
		run();
	}
	protected void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	protected void setStatus(String status) {
		this.status = status;
	}
	public long getTaskId() {
		return taskId;
	}
	public String getTaskName() {
		return taskName;
	}
	public long getMaxExecTime() {
		return maxExecTime;
	}
	public String getStatus() {
		return status;
	}
	
	
	
}
