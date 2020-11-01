package myaction.plog;

import java.util.ArrayList;

public class LogService {
	private LogThread thread = new LogThread(this);
	private ArrayList<LogRowSet> list = new ArrayList<LogRowSet>();

	public void start() {
		thread.start();
	}
	public void stop() {
		thread.runAndStop();
	}
	public void addRowSet(LogRowSet set) {
		synchronized(list) {
			list.add(set);
		}
	}
	protected int runOnce() {
		int count = 0;
		synchronized(list) {
			for(LogRowSet log : list) {
				count += log.runOnce();
			}
		}
		return count;
	}
}
