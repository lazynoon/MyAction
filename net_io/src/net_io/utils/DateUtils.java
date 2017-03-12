package net_io.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	//七天前作为基准时间
	public static final long REFERENCE_TIME = System.currentTimeMillis() - 7 * 86400;
	
	/**
	 * 取得简短时间
	 * @param time 单位：ms
	 * @return 相对与 REFERENCE_TIME 的时间（单位秒）
	 */
	public static int getShortTime() {
		return getShortTime(System.currentTimeMillis()); 
	}
	public static int getShortTime(long time) {
		return (int)((time - REFERENCE_TIME) / 1000); 
	}
	
	/**
	 * 取得完整时间
	 * @param time 单位：s
	 * @return 单位为毫秒的完整时间
	 */
	public static long getLongTime(int time) {
		return time * 1000L + REFERENCE_TIME; 
	}
	
	/**
	 * 解析标准日期格式
	 * @param str 时间格式 yyyy-MM-dd
	 * @return 解析失败返回null
	 */
	public static Date parseDate(String str) {
		if(str == null) {
			return null;
		}
		try {
			if(str.length() == 10) {
				return new SimpleDateFormat("yyyy-MM-dd").parse(str);
			} else if(str.length() == 19) {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(str);
			} else if(str.length() > 19 && str.indexOf('.') == 19) {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(str);
			} else {
				throw new RuntimeException("DateTime length error. String: "+str);
			}
		} catch (ParseException e) {
			throw new RuntimeException("DateTime parse error. String: "+str+", Exception: "+e);
		}
	}

	/**
	 * 解析标准时间格式
	 * @param str 时间格式 yyyy-MM-dd HH:mm:ss
	 * @return 解析失败返回null
	 */
	public static Date parseDateTime(String str) {
		return parseDate(str);
	}

	public static String getDateTime() {
		return getDateTime(new Date());
	}
	public static String getDateTime(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}

	/**
	 * 获取带毫秒的时间
	 * @return String
	 */
	public static String getDateMicroTime() {
		return getDateMicroTime(new Date());
	}
	
	/**
	 * 获取带毫秒的时间
	 * @param date
	 * @return String
	 */
	public static String getDateMicroTime(Date date) {
		if(date == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date);
	}

	public static String format(Date date, String formatStr) {
		if(date == null) {
			return null;
		}
		return new SimpleDateFormat(formatStr).format(date);
	}
}
