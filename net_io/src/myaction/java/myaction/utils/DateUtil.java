package myaction.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net_io.utils.DateUtils;
import net_io.utils.MixedUtils;


public class DateUtil extends DateUtils {
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
	
	public static long toUnixTime(String datetime) {
		if(datetime == null) {
			return 0;
		}
		if(datetime.length() > 19) {
			datetime = datetime.substring(0, 19);
		}
		return parseDateTime(datetime).getTime();
	}
	
	/**
	 * 转化成标准的字符型日期(不含时间)yyyy-MM-dd
	 * @param date
	 * @return
	 */
	public static String formatDate(Date date) {
		if(date == null) {
			return null;
		}
		return format(date, "yyyy-MM-dd");
	}
	
	/**
	 * 转化成标准的字符型日期(含时间)yyyy-MM-dd HH:mm:ss
	 * @param date
	 * @return
	 */
	public static String formatDateTime(Date date) {
		if(date == null) {
			return null;
		}
		return format(date, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 采用PHP date函数的参数，转换日期格式
	 * @param date
	 * @param format
	 * @return
	 */
	public static String formatLikePHP(Date date, String format) {
		if(date == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<format.length(); i++) {
			char ch = format.charAt(i);
			switch(ch) {
				case 'Y': sb.append("yyyy"); break;
				case 'y': sb.append("yy"); break;
				case 'm': sb.append("MM"); break;
				case 'n': sb.append("M"); break;
				case 'd': sb.append("dd"); break;
				case 'H': sb.append("HH"); break;
				case 'i': sb.append("mm"); break;
				case 's': sb.append("ss"); break;
				default: sb.append(ch); break;
			}
		}
		return format(date, sb.toString());
	}
	
	
	public static String getDateTime(long time) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
	}
	
	/**
	 * 转换日期对象中的时间
	 * @param date
	 * @param time 值可为：HH:II:SS或HH:II或HH。空值表示“00:00:00”
	 */
	public static Date convertSpecialTime(Date date, String time) {
		if(date == null) {
			return null;
		}
		String str = format(date, "yyyy-MM-dd") + " " + formatHIS(parseHIS(time));
		return parseDateTime(str);
	}
	
	/**
	 * 转换时间格式为一天中的第N秒数
	 * @param time 格式：“HH:II:SS或HH:II或HH”
	 * @return 一天中的第N秒数
	 */
	public static int parseHIS(String time) {
		if(time == null) {
			return 0;
		}
		time = time.trim();
		if(time.length() == 0) {
			return 0;
		}
		String[] arr = StringUtil.split(time, ':');
		if(arr.length > 3) {
			throw new RuntimeException("time("+time+") format error.");
		}
		int hour = MixedUtils.parseInt(arr[0]);
		int minute = 0;
		if(arr.length >= 2) {
			minute = MixedUtils.parseInt(arr[1]);
		}
		int second = 0;
		if(arr.length >= 3) {
			second = MixedUtils.parseInt(arr[2]);
		}
		if(hour > 23 || minute > 59 || second > 59) {
			throw new RuntimeException("time("+time+") format error.");
		}
		return hour * 3600 + minute * 60 + second;
	}
	
	/**
	 * 一天中的第N秒，转换为格式可读时间格式
	 * @param secondAtDay 一天中的第N秒数
	 * @return 返回时间格式：HH:II:SS
	 */
	public static String formatHIS(int secondAtDay) {
		return _formatHIS(secondAtDay, true);
	}
	
	/**
	 * 一天中的第N秒，转换为格式可读时间格式
	 * @param secondAtDay 一天中的第N秒数
	 * @return 返回时间格式：HH:II
	 */
	public static String formatHI(int secondAtDay) {
		return _formatHIS(secondAtDay, false);
	}
	
	/**
	 * 时间格式化
	 */
	private static String _formatHIS(int time, boolean hasSecond) {
		int hour = time / 3600;
		time = time % 3600;
		int minute = time / 60;
		int second = time % 60;
		String str = "";
		if(hour < 10) {
			str += "0"+hour+":";
		} else {
			str += hour+":";
		}
		if(minute < 10) {
			str += "0"+minute;
		} else {
			str += minute;
		}
		if(hasSecond) {
			if(second < 10) {
				str += ":0"+second;
			} else {
				str += ":"+second;
			}
		}
		return str;
	}
	public static int getYear(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.YEAR);
	}
	public static int getMonth(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MONTH) + 1;
	}
	public static int getHour(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.HOUR);
	}
	public static int getMinute(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MINUTE);
	}
	public static int getSecond(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.SECOND);
	}
	public static int getDayOfMonth(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.DAY_OF_MONTH);
	}
	/**
	 * 计算星期几。周一为“1”，周日为“7”
	 * @param date
	 */
	public static int getDayOfWeek(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if(day == 0) {
			day = 7;
		}
		return day;
	}
	/**
	 * 计算星期几。周一为“1”，周日为“7”
	 * @param date
	 */
	public static int getWeekOfYear(Date date) {
		if(date == null) {
			return 0;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		return calendar.get(Calendar.WEEK_OF_YEAR);
	}
	
	/**
	 * 对DB返回的带毫秒的datetime，进行截断
	 */
	public static String cutDateTime(String datetime) {
		if(datetime == null || datetime.length() <= 19) {
			return datetime;
		}
		return datetime.substring(0, 19);
	}
	
	/**
	 * 是否为零年零月零日零时零分零秒（判断依据为不含有大于0的数字）
	 */
	public static boolean isZeroTime(String time) {
		if(time == null) {
			return true;
		}
		int len = time.length();
		for(int i=0; i<len; i++) {
			char ch = time.charAt(i);
			if(ch >= '1' && ch <= '9') {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 获取今天0点0分0秒的Date对象
	 * @return Date
	 */
	public static Date getToday() {
		return parseDate(format(new Date(), "yyyy-MM-dd"));
	}
	
	/**
	 * 转换日期为格式良好的字符串
	 * @param datetime
	 * @return 
	 */
	public static String toFormalTime(String datetime) {
		if(datetime == null || datetime.length() == 19 || datetime.length() == 10) {
			return datetime;
		}
		datetime = datetime.trim();
		if(datetime.length() < 6) {
			return null;
		}
		if(datetime.length() <= 10) {
			return datetime;
		}
		String str1 = datetime.substring(0, 10);
		String str2 = datetime.substring(10).trim();
		String[] arr1 = StringUtil.split(str2, ':');
		
		String newDatetime = str1 + " ";
		newDatetime += StringUtil.addPrefix(arr1[0], '0', 2) + ":";
		if(arr1.length > 1) {
			newDatetime += StringUtil.addPrefix(arr1[1], '0', 2) + ":";
			if(arr1.length > 2) {
				newDatetime += StringUtil.addPrefix(arr1[2], '0', 2);
			} else {
				newDatetime += "00";				
			}
		} else {
			newDatetime += "00:00";	
		}
		return newDatetime;
	}
	
	public static Date parseDate(String str, boolean endOfDay) {
		Date time = DateUtils.parseDate(str);
		if(time == null || endOfDay == false) {
			return time;
		}
		return new Date(time.getTime() + 86400*1000-1);
	}
}
