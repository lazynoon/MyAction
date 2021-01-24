package myaction.model;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import myaction.utils.LogUtil;

public class RunTimer {
	private Timer timer;
	public RunTimer() {
		timer = new Timer();
	}
	public RunTimer(boolean isDaemon) {
		timer = new Timer(isDaemon);
	}
	
	public void schedule(RunTask task, long delay) {
		timer.schedule(new MyTimerTask(task), delay);
	}
	
	public void schedule(RunTask task, Date time) {
		timer.schedule(new MyTimerTask(task), time);
	}
	
	public void schedule(RunTask task, long delay, long period) {
		timer.schedule(new MyTimerTask(task), delay, period);
	}
	
	public void schedule(RunTask task, Date firstTime, long period) {
		timer.schedule(new MyTimerTask(task), firstTime, period);		
	}
	
	public void scheduleAtFixedRate(RunTask task, long delay, long period) {
		timer.scheduleAtFixedRate(new MyTimerTask(task), delay, period);		
	}
	
	public void scheduleAtFixedRate(RunTask task, Date firstTime, long period) {
		timer.scheduleAtFixedRate(new MyTimerTask(task), firstTime, period);		
	}
	
	private static class MyTimerTask extends TimerTask {
		private RunTask runTask;
		private MyTimerTask(RunTask runTask) {
			this.runTask = runTask;
		}

		@Override
		public void run() {
			try {
				runTask._execute();
			} catch (Exception e) {
				LogUtil.logWarn(e);
			}
		}
		
	}
}
