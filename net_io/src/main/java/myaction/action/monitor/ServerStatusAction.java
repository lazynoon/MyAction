package myaction.action.monitor;

import java.util.List;

import myaction.action.MyMonitorBaseAction;
import myaction.extend.AppConfig;
import myaction.utils.ByteUnitUtil;
import myaction.utils.DateUtil;
import myaction.utils.MathUtil;
import net_io.core.StatNIO;
import net_io.myaction.MyActionServer.RequestInfo;


public class ServerStatusAction extends MyMonitorBaseAction {
	private static long lastCheckTime1 = 0;
	private static long lastCheckCount1 = 0;
	private static long lastCheckTime2 = 0;
	private static long lastCheckCount2 = 0;

	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}
	}

	public void doIndex() throws Exception {
		long currentTime = System.currentTimeMillis();
		List<RequestInfo> list = AppConfig.ACTION_SERVER.listRequestInfo();
		
		StringBuffer sb = new StringBuffer();
		sb.append("No.\tStatus\tRunCount\tRunTime\tRunMode\tPath\tRemote\tRequestURI\r\n");
		long totalRunCount = 0;
		for(int i=0; i<list.size(); i++) {
			RequestInfo info = list.get(i);
			totalRunCount += info.getRunCount();
			sb.append(i+1);
			sb.append("\t");
			sb.append(info.getStatus());
			sb.append("\t");
			sb.append(info.getRunCount());
			sb.append("\t");
			sb.append(MathUtil.round(info.getRunUsTime()/1000.0, 2));
			sb.append("\t");
			//sb.append(info.getRunMode());
			sb.append("\t");
			sb.append(info.getPath());
			sb.append("\t");
			sb.append(info.getRemoteAddress());
			sb.append("\t");
//			sb.append(filterURI(info.getRequestURI()));
			sb.append("\r\n");
		}
		//请求处理速度计算
		long speed = -1; //100秒请求数，显示时，再转为每秒请求数
		if(lastCheckTime1 == 0) {
			lastCheckTime2 = lastCheckTime1 = currentTime;
			lastCheckCount2 = lastCheckCount1 = totalRunCount;
		} else {
			long timeUnit = 100 * StatNIO.ONE_THOUSAND_LONG;
			long minPeriod = 5 * StatNIO.ONE_THOUSAND_LONG;
			long checkTime1 = lastCheckTime1;
			long checkTime2 = lastCheckTime2;
			long checkCount1 = lastCheckCount1;
			long checkCount2 = lastCheckCount2;
			//两个断点，都满足条件
			if(currentTime - checkTime1 >= minPeriod && currentTime - checkTime2 >= minPeriod) {
				if(checkTime1 > checkTime2) { //第2个断点更旧
					speed = (totalRunCount - checkCount1) * timeUnit / (currentTime - checkTime1);
					lastCheckTime2 = currentTime;
					lastCheckCount2 = totalRunCount;
				} else {
					speed = (totalRunCount - checkCount2) * timeUnit / (currentTime - checkTime2);
					lastCheckTime1 = currentTime;
					lastCheckCount1 = totalRunCount;
				}
			} else if(currentTime - checkTime1 >= minPeriod) { //仅第一个断点满足条件
				speed = (totalRunCount - checkCount1) * timeUnit / (currentTime - checkTime1);
			} else if(currentTime > checkTime2) {
				speed = (totalRunCount - checkCount2) * timeUnit / (currentTime - checkTime2);
			}
		}
		//摘要信息
		StringBuffer summary = new StringBuffer();
		Runtime runtime = Runtime.getRuntime();
		summary.append("CurrentTime: "+DateUtil.getDateMicroTime());
		summary.append("\r\n");
		summary.append("TotalRunCount: "+totalRunCount);
		summary.append(", ");
		summary.append("PendingCount: "+AppConfig.ACTION_SERVER.getPendingRequestCount());
		summary.append(", ");
		summary.append("RebuildThread: "+AppConfig.ACTION_SERVER.getRebuildThreadCount());
		summary.append(", ");
		String strSpeed;
		if(speed >= 0) {
			strSpeed = String.valueOf(speed/100.0);
		} else {
			strSpeed = "N/A";
		}
		summary.append("Speed: "+strSpeed+" request/s");
		summary.append("\r\n");
		summary.append("TotalMemory: "+ByteUnitUtil.format(runtime.totalMemory()));
		summary.append(", ");
		summary.append("FreeMemory: "+ByteUnitUtil.format(runtime.freeMemory()));
		summary.append(", ");
		summary.append("UsedMemory: "+ByteUnitUtil.format(runtime.totalMemory()-runtime.freeMemory()));
		summary.append(", ");
		summary.append("MaxMemory: "+ByteUnitUtil.format(runtime.maxMemory()));
		summary.append("\r\n");
		summary.append("-------------------------------------------------------\r\n");
		summary.append("\r\n");
		
		response.println("<PRE>"+summary + sb+"</PRE>");
		
	}
	
}
