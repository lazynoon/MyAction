package myaction.action.monitor;

import myaction.action.MyMonitorBaseAction;
import myaction.utils.DateUtil;
import myaction.utils.LogUtil;
import net_io.utils.MixedUtils;

import java.util.Date;
import java.util.List;

public class LastLogAction extends MyMonitorBaseAction {
	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}

	}

	public void doIndex() throws Exception {
		LogUtil.SearchLastLogParam param = new LogUtil.SearchLastLogParam();
		List<LogUtil.LogInfo> logs = LogUtil.searchLastLog(param);
		int count = MixedUtils.parseInt(request.getParameter("count"));
		if (count > 0) {
			param.setCount(count);
		}
		int level = -1;
		if (request.hasParameter("level") && MixedUtils.isNumeric(request.getParameter("level"))) {
			level = MixedUtils.parseInt(request.getParameter("level"));
		}
		if (level >= 0) {
			param.setLogLevel(LogUtil.toLogLevel(level));
		}
		int minLevel = 0;
		minLevel = MixedUtils.parseInt(request.getParameter("min_level"));
		param.setMinLevel(minLevel);
		StringBuilder builder = new StringBuilder();
		builder.append("<table>");
		builder.append("<tr><th>No.</th><th>ID</th><th>LogLevel</th><th>LogTime</th><th>Message</th>" +
				"<th>StackTrace</th></tr>");
		for(int i=0; i<logs.size(); i++) {
			LogUtil.LogInfo logInfo = logs.get(i);
			builder.append("<tr><td>");
			builder.append(i+1);
			builder.append("</td><td>");
			builder.append(logInfo.getLogId());
			builder.append("</td><td>");
			builder.append(logInfo.getLogLevel());
			builder.append("</td><td>");
			builder.append(DateUtil.formatDateTime(new Date(logInfo.getLogTime())));
			builder.append("</td><td>");
			builder.append(logInfo.getMessage());
			builder.append("</td><td>");
			if (logInfo.getStackTrace() != null) {
				if (logInfo.getExceptionClass() != null) {
					builder.append(logInfo.getExceptionClass());
				}
				builder.append(logInfo.getStackTrace().replaceAll("\n", "<br>"));
			}
			builder.append("</td><td>");
			builder.append("</td></tr>");
		}
		response.println(builder.toString());
	}
}
