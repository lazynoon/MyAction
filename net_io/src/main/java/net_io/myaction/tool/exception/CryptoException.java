package net_io.myaction.tool.exception;

public class CryptoException extends Exception {
	Exception causeException; 
	/**
	 * 
	 */
	private static final long serialVersionUID = -463516304580078860L;

	public CryptoException(Exception causeException) {
		this.causeException = causeException;
	}
	
	@Override
	public String getMessage() {
		return this.causeException.toString();
	}

}
