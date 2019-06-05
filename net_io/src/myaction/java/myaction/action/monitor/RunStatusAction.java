package myaction.action.monitor;

import myaction.action.MyMonitorBaseAction;
import myaction.extend.AppConfig;
import myaction.extend.BaseServiceAction;
import myaction.utils.ByteUnitUtil;
import net_io.utils.DateUtils;
import net_io.utils.MixedUtils;

public class RunStatusAction extends MyMonitorBaseAction {

	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}
		
	}

	public void doIndex() throws Exception {
		Runtime runtime = Runtime.getRuntime();
		response.println("<PRE>");
		response.println("Status: <font color='green'><strong>RUNNING</strong></font>");
		response.println("StartTime: "+DateUtils.getDateTime(AppConfig.getStartTime()));
		response.println("CurrentTime: "+DateUtils.getDateTime());
		response.println("TotalMemory: "+ByteUnitUtil.format(runtime.totalMemory()));
		response.println("FreeMemory: "+ByteUnitUtil.format(runtime.freeMemory()));
		response.println("UsedMemory: "+ByteUnitUtil.format(runtime.totalMemory()-runtime.freeMemory()));
		response.println("MaxMemory: "+ByteUnitUtil.format(runtime.maxMemory()));
		response.println("RunCount: "+BaseServiceAction.getTotalRunCount());
		response.println("RunTime: "+BaseServiceAction.getTotalRunTime()+"ms");
		response.println("ManageRunCount: "+MyMonitorBaseAction.getRunCount());
		response.println("ManageRunTime: "+MyMonitorBaseAction.getRunTime()+"ms");
		String usTime = request.getParameter("usTime");
		if(MixedUtils.isNumeric(usTime)) {
			long t2 = Long.parseLong(usTime);
			long d = (request.getStartTime() - t2);// / 1000.0;
			response.println("OffsetTime: "+d+"ms");
		}
		response.println("Version: <strong>"+AppConfig.VERSION+"</strong>");
		response.println("");
		response.println("</PRE>");

	}
}
