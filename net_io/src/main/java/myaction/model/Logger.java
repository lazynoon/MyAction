package myaction.model;

public class Logger {
	protected Logger(String className) {
		
	}
	
	public boolean isDebugEnabled() {
		return false;
	}
	
	public void debug(String msg) {
		System.out.println(msg);
	}

	public void debug(String msg, Exception e) {
		System.out.println(msg);
	}

	public boolean isInfoEnabled() {
		return false;
	}
	
	public void info(String msg) {
		System.out.println(msg);
	}

	public void info(String msg, Exception e) {
		System.out.println(msg);
	}

	public boolean isWarnEnabled() {
		return false;
	}
	
	public void warn(String msg) {
		System.out.println(msg);
	}

	public void warn(String msg, Exception e) {
		System.out.println(msg);
	}

	public boolean isErrorEnabled() {
		return false;
	}
	
	public void error(String msg) {
		System.out.println(msg);
	}
	
	public void error(String msg, Exception e) {
		System.out.println(msg);
	}
	
	public void trace(String msg) {
		System.out.println(msg);
	}
}
