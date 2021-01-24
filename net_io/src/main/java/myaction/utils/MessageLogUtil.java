package myaction.utils;

import java.io.PrintWriter;

import net_io.utils.DateUtils;

public class MessageLogUtil {
	private static boolean enabled = false;
	private static PrintWriter logger = null;
	private static long lastFlushSecond = 0;
	private static long lastWriteCount = 0;
	
	public static void setEnabled(boolean enabled) {
		MessageLogUtil.enabled = enabled; 
	}
	
	public static void setPrintWriter(PrintWriter writer) {
		if(logger != null && logger != writer) {
			logger.close();
		}
		logger = writer;
	}
	
	public static boolean isEnabled() {
		return enabled && logger != null;
	}
	
	public static void logStr(String str) {
		if(isEnabled() == false) {
			return;
		}
		str = DateUtils.getDateTime() + " " + str;
		logger.println(str);
		lastWriteCount++;
	}
	
	public static boolean autoFlush() {
		if(lastWriteCount > 0) {
			long sTime = System.currentTimeMillis() / 1000;
			if(sTime != lastFlushSecond) {
				lastWriteCount = 0;
				lastFlushSecond = sTime;
				if(logger != null) {
					logger.flush();
					return true;
				}
			}
		}
		return false;
	}
}
