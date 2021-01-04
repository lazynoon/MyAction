package net_io.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	/** 1秒对应的毫秒数（1秒） **/
	public final static long ONE_SECOND_MS = 1000;
	/** 1分对应的毫秒数（1分钟） **/
	public final static long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
	/** 1小时对应的毫秒数（1小时） **/
	public final static long ONE_HOUR_MS = 3600 * ONE_SECOND_MS;
	/** 1天时间段的毫秒数（1天） **/
	public final static long ONE_DAY_MS = 86400 * ONE_SECOND_MS;
	/** 1年时间段的毫秒数（按366天计算） **/
	public final static long ONE_YEAR_MS = 366 * ONE_DAY_MS;
	/** 1个月时间段的毫秒数（按31天计算） **/
	public final static long ONE_MONTH_MS = 31 * ONE_DAY_MS;
	/** 1周时间段的毫秒数（7天） **/
	public final static long ONE_WEEK_MS = 7 * ONE_DAY_MS;
	/** 少1秒的时间（999毫秒） **/
	public final static long LESS_ONE_SECOND = 999;

	//七天前作为基准时间
	public static final long REFERENCE_TIME = System.currentTimeMillis() - 7 * 86400;
	
	/**
	 * 取得简短时间
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
		if(str == null || str.length() == 0) {
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
