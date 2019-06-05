package net_io.myaction;

import java.lang.reflect.Method;



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

	/**
	 * 在执行Action方法前调用
	 */
	protected void beforeExecute() throws Exception {
		preExecute();
	}
	
	/** @deprecated **/
	protected void preExecute() throws Exception {}

	
	protected boolean check() throws Exception {
		return true;
	}
	
	
	/**
	 * 执行默认的Action方法
	 */
	protected void defaultExecute(String methodName) throws Exception {
		if(methodName == null || methodName.length() == 0) {
			throw new CheckException(401, "Execute method is empty.");
		}
		methodName = "do" + methodName;
		for(Method methodObject : this.getClass().getMethods()) {
			if(methodObject.isVarArgs()) {
				continue; //必须是无参方法。如：doXXX()
			}
			String curName = methodObject.getName();
			if(methodName.equalsIgnoreCase(curName) == false) {
				continue; //方法名，必须以 "do" 开头
			}
			methodObject.invoke(this);
			return; //执行完成
		}
		throw new CheckException(404, "Not Found");
	}
	
	/**
	 * 在执行Action方法后调用
	 */
	protected void afterExecute() throws Exception {}
	
//	protected void display(String tplName) {
//		if(tplName != null && tplName.length() == 0) {
//			tplName = null;
//		}
//		this.tplName = tplName;
//	}
//	
	
}
