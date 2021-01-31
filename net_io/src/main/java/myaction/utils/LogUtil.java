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

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class LogUtil {
	private static final int LOG_LEVEL_COUNT = 6;
	public static final int TRACE_LOG_NUM = 1000;
	private static LogInfo[][] lastLogsPool = new LogInfo[LOG_LEVEL_COUNT][TRACE_LOG_NUM];
	private static int[] lastLogsPosition = new int[LOG_LEVEL_COUNT];
	public enum LogLevel {TRACE, DEBUG, INFO, WARN, ERROR, FATAL}

	/**
	 * 写入 TRACE 日志（仅通过web控制台打印）
	 * @param msg 日志内容
	 */
	public static void logTrace(String msg) {
		writeLog(0, msg, null, false);
	}

	/**
	 * 写入 TRACE 日志（仅通过web控制台打印）
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logTrace(String msg, Exception e) {
		writeLog(0, msg, e, false);
	}

	/**
	 * 写入 DEBUG 日志
	 * @param msg 日志内容
	 */
	public static void logDebug(String msg) {
		writeLog(1, msg, null, false);
	}

	/**
	 * 写入 DEBUG 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logDebug(String msg, Exception e) {
		writeLog(1, msg, e, false);
	}

	/**
	 * 写入 INFO 日志
	 * @param msg 日志内容
	 */
	public static void logInfo(String msg) {
		writeLog(2, msg, null, true);
	}

	/**
	 * 写入 INFO 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logInfo(String msg, Exception e) {
		writeLog(2, msg, e, true);
	}

	/**
	 * 写入 WARN 日志
	 * @param msg 日志内容
	 */
	public static void logWarn(String msg) {
		writeLog(3, msg, null, true);
	}

	/**
	 * 写入 WARN 日志
	 * @param e Exception对象
	 */
	public static void logWarn(Exception e) {
		writeLog(3, null, e, true);
	}

	/**
	 * 写入 WARN 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logWarn(String msg, Exception e) {
		writeLog(3, msg, e, true);
	}

	/**
	 * 写入 ERROR 日志
	 * @param msg 日志内容
	 */
	public static void logError(String msg) {
		writeLog(4, msg, null, true);
	}

	/**
	 * 写入 ERROR 日志
	 * @param e Exception对象
	 */
	public static void logError(Exception e) {
		writeLog(4, null, e, true);
	}

	/**
	 * 写入 ERROR 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logError(String msg, Exception e) {
		writeLog(4, msg, e, true);
	}

	/**
	 * 写入 ERROR 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 * @param allowSaveDB 是否保存到DB
	 */
	public static void logError(String msg, Exception e, boolean allowSaveDB) {
		writeLog(4, msg, e, allowSaveDB);
	}

	/**
	 * 写入 FATAL 日志
	 * @param msg 日志内容
	 */
	public static void logFatal(String msg) {
		writeLog(5, msg, null, true);
	}

	/**
	 * 写入 FATAL 日志
	 * @param msg 日志内容
	 * @param e Exception对象
	 */
	public static void logFatal(String msg, Exception e) {
		writeLog(5, msg, e, true);
	}

	/**
	 * 搜索进程中最后写入的日志缓存
	 * @param param SearchLastLogParam对象
	 * @return 日志对象列表（非空）
	 */
	public static List<LogInfo> searchLastLog(final SearchLastLogParam param) {
		ArrayList<LogInfo> result = new ArrayList<LogInfo>();
		for (int level=0; level<lastLogsPool.length; level++) {
			if (param.minLevel > 0 && level < param.minLevel) {
				continue;
			}
			if (param.logLevel != null && param.logLevel.ordinal() != level) {
				continue;
			}
			for (LogInfo logInfo : lastLogsPool[level]) {
				if (logInfo == null) {
					continue;
				}
				if (logInfo.logTime < param.startLogTime) {
					continue;
				}
				if (param.endLogTime > 0 && logInfo.logTime >= param.endLogTime) {
					continue;
				}
				result.add(logInfo);
			}
		}
		LogInfo[] logInfoArr = result.toArray(new LogInfo[0]);
		Arrays.sort(logInfoArr, new Comparator<LogInfo>() {
			@Override
			public int compare(LogInfo o1, LogInfo o2) {
				if (o1.logId < o2.logId) {
					return param.sortFlag;
				} else if (o1.logId > o2.logId) {
					return  - param.sortFlag;
				} else {
					return 0;
				}
			}
		});
		int count = Math.min(param.count, logInfoArr.length);
		result = new ArrayList<LogInfo>();
		for (int i=0; i<count; i++) {
			result.add(logInfoArr[i]);
		}
		return result;
	}

	private static void writeLog(int level, String msg, Exception e, boolean allowSaveDB) {
		LogLevel logLevel = toLogLevel(level);
		String exceptionMsg = "";
		if(e != null) {
			exceptionMsg = e.getClass().getName() + " - " + e.getMessage();
		}
		String str = "[" + logLevel.name() + "] " + DateUtil.getDateTime() + " ";
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
		//记录 TRACE 日志
		LogInfo traceLogInfo = new LogInfo(logLevel, msg, traceStr, e);
		synchronized (lastLogsPosition) {
			lastLogsPool[level][lastLogsPosition[level]++] = traceLogInfo;
			if (lastLogsPosition[level] >= lastLogsPool[level].length) {
				lastLogsPosition[level] = 0;
			}
		}
		//检查日志级别，未达日志级别，则返回
		if(level < AppConfig.getLogLevel()) {
			return;
		}
		if(level <= 2) {
			System.out.print(str);
		} else {
			System.err.print(str);
		}
		//检查是否记录到DB中
		if(!allowSaveDB) {
			return; //不允许保存到DB中
		}
		//保存日志到DB
		CommonParams params = ThreadSessionPool.getCommonParamsNotNull();
		Mixed logInfo = new Mixed(params.createLogParams());
		logInfo.put("id", EncodeUtils.createTimeRandId());
		logInfo.put("log_level", level);
		logInfo.put("log_name", logLevel.name());
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

	public static LogLevel toLogLevel(int level) {
		if (level == 0) {
			return LogLevel.TRACE;
		} else if (level == 1) {
			return LogLevel.DEBUG;
		} else if (level == 2) {
			return LogLevel.INFO;
		} else if (level == 3) {
			return LogLevel.WARN;
		} else if (level == 4) {
			return LogLevel.ERROR;
		} else if (level == 5) {
			return LogLevel.FATAL;
		} else {
			throw new IllegalArgumentException("Not support log level: " + level);
		}
	}

	public static class SearchLastLogParam {
		/** 返回条数（默认100条） **/
		private int count = 100;
		/** 日志级别 **/
		private LogLevel logLevel = null;
		/** 最低级别 **/
		private int minLevel;
		/** 日志开始时间（包含） **/
		private long startLogTime = 0;
		/** 日志结束时间（不包含，0表示不限，默认不限） **/
		private long endLogTime = 0;
		/** 排序标志（1降序， -1升序） **/
		private int sortFlag = 1;

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public LogLevel getLogLevel() {
			return logLevel;
		}

		public void setLogLevel(LogLevel logLevel) {
			this.logLevel = logLevel;
		}

		public int getMinLevel() {
			return minLevel;
		}

		public void setMinLevel(int minLevel) {
			this.minLevel = minLevel;
		}

		public long getStartLogTime() {
			return startLogTime;
		}

		public void setStartLogTime(long startLogTime) {
			this.startLogTime = startLogTime;
		}

		public long getEndLogTime() {
			return endLogTime;
		}

		public void setEndLogTime(long endLogTime) {
			this.endLogTime = endLogTime;
		}
	}

	/**
	 * 日志信息
	 */
	public static class LogInfo {
		public static final int MAX_MESSAGE_LENGTH = 8192;
		private static AtomicLong idGenerator = new AtomicLong(0);
		private long logId;
		private long logTime;
		private LogLevel logLevel;
		private String message;
		private String stackTrace;
		private String exceptionClass = null;

		private LogInfo(LogLevel logLevel, String msg, String traceStr, Exception e) {
			this.logId = idGenerator.incrementAndGet();
			this.logTime = System.currentTimeMillis();
			this.logLevel = logLevel;
			this.message = msg;
			if (this.message == null) {
				this.message = "";
			} else if (this.message.length() > MAX_MESSAGE_LENGTH) {
				this.message = this.message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
			}
			this.stackTrace = traceStr;
			if (this.stackTrace != null && this.stackTrace.length() > MAX_MESSAGE_LENGTH) {
				this.stackTrace = this.stackTrace.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
			}
			if (e != null) {
				this.exceptionClass = e.getClass().getName();
			}
		}

		/**
		 * 获取日志ID
		 * @return 当前进程唯一的日志ID
		 */
		public long getLogId() {
			return logId;
		}

		/**
		 * 获取日志时间
		 * @return 记录日志的时间（非空）
		 */
		public long getLogTime() {
			return logTime;
		}

		/**
		 * 获取日志级别
		 * @return LogLevel对象（非空）
		 */
		public LogLevel getLogLevel() {
			return logLevel;
		}

		/**
		 * 获取记录的日志消息（非空）
		 * @return
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * 获取异常堆栈信息
		 * @return 不存在返回 Exception 返回 null
		 */
		public String getStackTrace() {
			return stackTrace;
		}

		/**
		 * 获取异常类的名称
		 * @return 不存在返回 Exception 返回 null
		 */
		public String getExceptionClass() {
			return exceptionClass;
		}
	}

}
