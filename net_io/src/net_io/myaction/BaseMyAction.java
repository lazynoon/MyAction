package net_io.myaction;



public abstract class BaseMyAction {
	public Request request;
	public Response response;
	
	public String getModule() { return ""; };
	public String getAction() { return ""; };

	public void preExecute() throws Exception {
	}

	public boolean check() throws Exception {
		return true;
	}
	
	//User Method

	public void afterExecute() throws Exception {
	}
}
