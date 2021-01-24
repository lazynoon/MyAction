package myaction.action;

import java.net.InetSocketAddress;
import java.util.Date;

import myaction.extend.AppConfig;
import myaction.extend.BaseServiceAction;
import myaction.plog.LogCsvFile;
import myaction.utils.MathUtil;
import net_io.core.StatNIO;
import net_io.myaction.CheckException;
import net_io.myaction.Request;
import net_io.utils.DateUtils;
import net_io.utils.Mixed;

abstract public class MyMonitorBaseAction extends BaseServiceAction {

	@Override
	public void beforeExecute() throws Exception {
		super.beforeExecute();;
		//IP检查
		InetSocketAddress address = request.getRemoteAddress();
		if(address == null) {
			throw new CheckException(421, "IP Limit - No IP.");
		}
		String ip = request.getRemoteAddress().getAddress().getHostAddress();
		if(!ip.equals("127.0.0.1") && !ip.startsWith("10.") && !ip.startsWith("192.168.")
				&& !ip.startsWith("172.16.")) {
			throw new CheckException(422, "IP Limit");
		}
	}
	
	@Override
	final public void afterExecute() throws Exception {
		//MessageLogFileBean.updateWriter(); //更新消息系统消息记录文件地址
		super.afterExecute();
		this.logStat(request, response.getError());
	}
	
	private void logStat(Request request, int error) {
		Date requestDate = new Date(request.getStartTime());
		long costNsTime = System.nanoTime() - request.getStartNanoTime();
		//long waitTime = glbStartDate.getTime() - requestDate.getTime();
		String ip = request.getClientIP();
		if(ip == null) {
			ip = request.getRemoteIP();
		} else 	if(!ip.equals(request.getRemoteIP())) {
			ip += "/" + request.getRemoteIP();
		}
		String logFile = AppConfig.getLogDir()+"manage/visitb_.log";
		Mixed logInfo = new Mixed();
		logInfo.put("VisitTime", DateUtils.getDateTime(requestDate));
		logInfo.put("CostTime", MathUtil.round(costNsTime / StatNIO.ONE_MILLION_DOUBLE, 2));
		logInfo.put("ErrorCode", error);
		logInfo.put("IP", ip);
		logInfo.put("URI", request.getPath());
		try {
			LogCsvFile.log(logFile, LogCsvFile.MODE.SPLIT_DAY, logInfo);
		} catch(Exception e) {
			System.err.println("Write Manager Visit Log Error: "+e+". "+logInfo.toJSON());
		}
	}


}
