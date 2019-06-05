package myaction.utils;

import myaction.extend.BaseDao;
import myaction.extend.CommonParams;
import myaction.extend.AppConfig;
import myaction.extend.ThreadSessionPool;
import myaction.plog.AsyncLog;
import net.sf.jsqlx.JSQLException;
import net_io.myaction.CheckException;
import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;

public class LogUtil {
	public static void logDebug(String msg) {
		writeLog(1, msg, null, true);
	}
	public static void logInfo(String msg) {
		writeLog(2, msg, null, true);
	}
	public static void logWarn(String msg) {
		writeLog(3, msg, null, true);
	}
	public static void logWarn(Exception e) {
		writeLog(3, null, e, true);
	}
	public static void logError(String msg) {
		writeLog(4, msg, null, true);
	}
	public static void logError(Exception e) {
		writeLog(4, null, e, true);
	}
	public static void logError(String msg, Exception e) {
		writeLog(4, msg, e, true);
	}
	
	public static void logError(String msg, Exception e, boolean allowSaveDB) {
		writeLog(4, msg, e, allowSaveDB);
	}
	
	private static void writeLog(int level, String msg, Exception e, boolean allowSaveDB) {
		if(level < AppConfig.getLogLevel()) {
			return;
		}
		String levelName = null;
		switch(level) {
		case 1:
			levelName = "[DEBUG]";
			break;
		case 2:
			levelName = "[INFO]";
			break;
		case 3:
			levelName = "[WARN]";
			break;
		case 4:
			levelName = "[ERROR]";
			break;
		default:
			levelName = "[LOG]";
		}
		String exceptionMsg = "";
		if(e != null) {
			exceptionMsg = e.getClass().getName() + " - " + e.getMessage();
		}
		String str = levelName + " " + DateUtil.getDateTime() + " ";
		if(msg != null) {
			if(e != null) {
				msg += "\n" + exceptionMsg;
			}
		} else if(e != null) {
			msg = exceptionMsg;
		}
		str += msg;
		str += "\n";
		String traceStr = null;
		if(e != null) {
			StackTraceElement[] traces = e.getStackTrace();
			if(traces != null) {
				StringBuffer sb = new StringBuffer();
				for(StackTraceElement trace : traces) {
					sb.append("\tat ");
					sb.append(trace.getClassName());
					sb.append('.');
					sb.append(trace.getMethodName());
					sb.append('(');
					sb.append(trace.getFileName());
					sb.append(':');
					sb.append(trace.getLineNumber());
					sb.append(')');
					sb.append('\n');
				}
				traceStr = sb.toString();
			}
		}
		if(traceStr != null) {
			str += traceStr;
		}
		if(level <= 2) {
			System.out.print(str);
		} else {
			System.err.print(str);
		}
		if(!allowSaveDB) {
			return; //不允许保存到DB中
		}
		//保存日志到DB
		CommonParams params = ThreadSessionPool.getCommonParamsNotNull();
		Mixed logInfo = new Mixed(params.createLogParams());
		logInfo.put("id", EncodeUtils.createTimeRandId());
		logInfo.put("log_level", level);
		logInfo.put("log_name", levelName);
		if(StringUtil.hasUnicodeSMP(msg)) {
			msg = StringUtil.encodeUnicodeSMP(msg);
		}
		logInfo.put("content", msg);
		logInfo.put("error", -1);
		logInfo.put("reason", "");
		if(e != null) {
			if(e instanceof JSQLException) {
				String runSQL = ((JSQLException)e).getRunSQL();
				if(StringUtil.hasUnicodeSMP(runSQL)) {
					runSQL = StringUtil.encodeUnicodeSMP(runSQL);
				}
				logInfo.put("run_sql", runSQL);
			}
			if(traceStr != null) {
				traceStr = exceptionMsg + "\n" + traceStr;
			} else {
				traceStr = exceptionMsg;
			}
		}
		if(StringUtil.hasUnicodeSMP(traceStr)) {
			traceStr = StringUtil.encodeUnicodeSMP(traceStr);
		}
		logInfo.put("trace_info", traceStr);
		try {
			AsyncLog.log(BaseDao.COMMON_LOG_DB, "zz_common_data_log", AsyncLog.MODE.SPLIT_MONTH, logInfo);
		} catch (CheckException e1) {
			System.err.println(DateUtil.getDateTime() + " - writeLog Error.");
			e1.printStackTrace();
		}

	}
}
