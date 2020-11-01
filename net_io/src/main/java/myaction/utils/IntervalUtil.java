package myaction.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/*
 * @describe 根据时间间隔获取时间
 * @author minglei.xie
 */
public final class IntervalUtil {

	/*
	 * @describe 按间隔数值获取时间
	 * 
	 * @param minute 分钟
	 * 
	 * @author minglei.xie
	 * 
	 * @return  string yyyy-MM-dd HH:mm:ss
	 */
	public static String getTime(int minute) {
		Calendar calendar = Calendar.getInstance();// 获取的是系统当前时间
		calendar.add(Calendar.MINUTE, minute);// 设置时间间隔
		String resulte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(calendar.getTime()); // 获取返回格式
		return resulte;
	}

	/*
	 * @describe 获取当前时间
	 * 
	 * @author minglei.xie
	 * 
	 * @return  string yyyy-MM-dd HH:mm:ss
	 */
	public static String getNowTime() {
		Calendar calendar = Calendar.getInstance();// 获取的是系统当前时间
		String resulte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(calendar.getTime()); // 获取返回格式
		return resulte;
	}
}
