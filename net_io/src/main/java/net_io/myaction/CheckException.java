package net_io.myaction;

public class CheckException extends Exception {
	private int error = 0;
	private String reason = null;

	private static final long serialVersionUID = -7674340404540683643L;
	public CheckException(int error, String reason) {
		super(reason);
		this.error = error;
		this.reason = reason;
	}
	public int getError() {
		return error;
	}
	public String getReason() {
		return reason;
	}
	
}
