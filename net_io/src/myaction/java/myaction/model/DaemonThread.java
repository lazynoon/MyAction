package myaction.model;

public class DaemonThread extends Thread {
	public DaemonThread() {
		this.setDaemon(true);
	}
}
