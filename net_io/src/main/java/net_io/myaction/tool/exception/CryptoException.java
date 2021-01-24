package net_io.myaction.tool.exception;

public class CryptoException extends Exception {
	private String message = null;
	private Exception causeException = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = -463516304580078860L;

	public CryptoException(String message) {
		this.message = message;
	}

	public CryptoException(Exception causeException) {
		this.causeException = causeException;
	}
	
	@Override
	public String getMessage() {
		if(causeException != null) {
			return causeException.toString();
		} else {
			return message;
		}

	}

}
