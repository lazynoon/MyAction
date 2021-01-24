package net_io.utils;


public class NetLog {
	/** 日志等级 **/
	final public static int RECORD_ALL = 0;
	final public static int DEBUG = 1;
	final public static int INFO = 2;
	final public static int WARN = 3;
	final public static int ERROR = 4;
	
	// 日志等级
	public static int LOG_LEVEL = WARN;
	
	public static void logDebug(String str) {
		writeLog(str, DEBUG);
	}
	
	public static void logInfo(String str) {
		writeLog(str, INFO);
	}

	public static void logWarn(String str) {
		writeLog(str, WARN);
	}

	public static void logError(String str) {
		writeLog(str, ERROR);
	}

	public static void logDebug(Throwable e) {
		writeLog(e, DEBUG);
	}
	
	public static void logInfo(Throwable e) {
		writeLog(e, INFO);
	}

	public static void logWarn(Throwable e) {
		writeLog(e, WARN);
	}

	public static void logError(Throwable e) {
		writeLog(e, ERROR);
	}

	public static void logDebug(String msg, Throwable e) {
		writeLog(msg, DEBUG);
		writeLog(e, DEBUG);
	}
	
	public static void logInfo(String msg, Throwable e) {
		writeLog(msg, INFO);
		writeLog(e, INFO);
	}

	public static void logWarn(String msg, Throwable e) {
		writeLog(msg, WARN);
		writeLog(e, WARN);
	}

	public static void logError(String msg, Throwable e) {
		writeLog(msg, ERROR);
		writeLog(e, ERROR);
	}

	public static void writeLog(String str, int level) {
		if(level < LOG_LEVEL) {
			return;
		}
		str = DateUtils.getDateTime() + " - " + str;
		if(level >= WARN) {
			System.err.println(str);
		} else {
			System.out.println(str);
		}
	}

	public static void writeLog(Throwable e, int level) {
		if(level < LOG_LEVEL) {
			return;
		}
		writeLog("Exception from "+e, level);
		if(level >= WARN) {
			e.printStackTrace(System.err);
		} else {
			e.printStackTrace(System.out);
		}
	}

}
