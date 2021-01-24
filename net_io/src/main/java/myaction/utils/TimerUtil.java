package myaction.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerUtil {
	private final static TimerUtil instance = new TimerUtil();
	private Timer timer;

	private TimerUtil() {
		this.timer = new Timer("TimerUtil", true);
	}

	public void cancelAll() {
		Timer obj = instance.timer;
		instance.timer = new Timer("TimerUtil", true);
		obj.cancel();
	}

	/**
	 * Schedules the specified task for execution at the specified time.
	 */
	public void schedule(TimerTask task, Date time) {
		timer.schedule(task, time);
	}
	/**
	 * Schedules the specified task for repeated fixed-delay execution, beginning at the specified time.
	 */
	public void schedule(TimerTask task, Date firstTime, long period) {
		timer.schedule(task, firstTime, period);
	}
	/**
	 * Schedules the specified task for execution after the specified delay.
	 */
	public void schedule(TimerTask task, long delay) {
		timer.schedule(task, delay);
	}
	/**
	 * Schedules the specified task for repeated fixed-delay execution, beginning after the specified delay.
	 */
	public void schedule(TimerTask task, long delay, long period) {
		timer.schedule(task, delay, period);
	}
	/**
	 * Schedules the specified task for repeated fixed-rate execution, beginning at the specified time.
	 */
	public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period)  {
		timer.scheduleAtFixedRate(task, firstTime, period);
	}
	/**
	 * Schedules the specified task for repeated fixed-rate execution, beginning after the specified delay.
	 */
	public void scheduleAtFixedRate(TimerTask task, long delay, long period)  {
		timer.scheduleAtFixedRate(task, delay, period);
	}

}
