package myaction.action.monitor;

import myaction.action.MyMonitorBaseAction;
import net_io.utils.DateUtils;

public class ServerTimeAction extends MyMonitorBaseAction {

	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}		
	}

	public void preExecute() {
		//ignore permission check
	}
	
	public void doIndex() throws Exception {
		response.println("server time is "+DateUtils.getDateMicroTime());
		//Thread.sleep(100);
	}
}
