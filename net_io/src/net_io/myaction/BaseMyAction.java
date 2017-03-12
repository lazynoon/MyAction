package net_io.myaction;



public abstract class BaseMyAction {
	protected Request request;
	protected Response response;
	
	private Exception lastActionException = null;
	/** 模板文件名 **/
	String tplName = null;

	
	//public String getModule() { return ""; }
	//public String getAction() { return ""; }
	
	protected Exception getLastActionException() { return lastActionException; }
	protected void setLastActionException(Exception lastActionException) { this.lastActionException = lastActionException; }

	protected void preExecute() throws Exception {}

	protected boolean check() throws Exception {
		return true;
	}
	
	//User Method
	protected void afterExecute() throws Exception {}
	
	protected void display(String tplName) {
		if(tplName != null && tplName.length() == 0) {
			tplName = null;
		}
		this.tplName = tplName;
	}
	
	
}
