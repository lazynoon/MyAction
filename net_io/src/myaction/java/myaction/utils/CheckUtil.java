package myaction.utils;

import java.util.List;

import net_io.myaction.CheckException;
import net_io.myaction.Request;
import net_io.utils.Mixed;
import net_io.utils.MixedUtils;

public class CheckUtil {
	
	public static boolean checkAccount(String str) {
		return true;
	}
	
	/**
	 * String - 检查必须不为空
	 * @throws CheckException 
	 */
	public static void checkNotEmpty(String value, String name) throws CheckException {
		if(MixedUtils.isEmpty(value)) {
			throw new CheckException(3611, name + "不能为空。");
		}
	}
	
	/**
	 * Mixed - 检查必须不为空
	 * @throws CheckException 
	 */
	public static void checkNotEmpty(Mixed params, String key, String name) throws CheckException {
		if(params.isEmpty(key)) {
			throw new CheckException(3612, name + "不能为空。");
		}
	}
	
	/**
	 * Request - 检查必须不为空
	 * @throws CheckException 
	 */
	public static void checkNotEmpty(Request request, String key, String name) throws CheckException {
		if(MixedUtils.isEmpty(request.getParameter(key))) {
			throw new CheckException(3613, name + "不能为空。");
		}
	}
	
	/**
	 * Mixed - 检查必须大于0
	 */
	public static void checkMoreThenZero(Mixed params, String key, String name) throws CheckException {
		if(params.getInt(key) <= 0) {
			throw new CheckException(3621, name + "必须大于0。");
		}
	}

	/**
	 * Request - 检查必须大于0
	 */
	public static void checkMoreThenZero(Request request, String key, String name) throws CheckException {
		if(MixedUtils.parseInt(request.getParameter(key)) <= 0) {
			throw new CheckException(3622, name + "必须大于0。");
		}
	}

	/**
	 * Integer - 检查必须大于0
	 */
	public static void checkMoreThenZero(int num, String name) throws CheckException {
		if(num <= 0) {
			throw new CheckException(3623, name + "必须大于0。");
		}
	}

	public static boolean isLong(String str) {
		if(str == null || str.length() == 0 || str.length() > 30) {
			return false;
		}
		for(int i=0; i<str.length(); i++) {
			if(str.charAt(i) < '0' || str.charAt(i) > '9') {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isInt(String str) {
		if(str == null || str.length() == 0 || str.length() > 10) {
			return false;
		}
		int i = 0;
		if(str.charAt(0) == '-') {
			if(str.length() == 1) {
				return false;
			}
			i = 1;
		}
		for(; i<str.length(); i++) {
			if(str.charAt(i) < '0' || str.charAt(i) > '9') {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isEmpty(String str) {
		return (str == null || str.length() == 0 ? true : false);
	}

	/**
	 * 是否为手机号
	 */
	public static boolean isMobile(String str) {
		if(str == null || str.length() != 11) {
			return false;
		}
		if(str.charAt(0) != '1' || str.charAt(1) <= '2') {
			return false;
		}
		for(int i=0; i<str.length(); i++) {
			if(str.charAt(i) < '0' || str.charAt(i) > '9') {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 是否为日期
	 * 格式：yyyy-MM-dd 或 yyyy-M-d
	 * @param str
	 * @return boolean
	 */
	public static boolean isDate(String str) {
		if(str == null || str.length() < 8 || str.length() > 10) {
			return false;
		}
		return true;
	}
	/**
	 * 是否为时间
	 * 格式：HH:mm 或 HH:mm:ss
	 * @param str
	 * @return boolean
	 */
	public static boolean isTime(String str) {
		if(str == null || (str.length() != 5 && str.length() != 8)) {
			return false;
		}
		return true;
	}
	
	/**
	 * 是否为合法的帐号
	 */
	public static boolean isAccount(String str) {
		if(str == null || str.length() < 6 || str.length() > 60) {
			return false;
		}
		return true;
	}

	/**
	 * 是否为合法的密码
	 */
	public static boolean isPassword(String str) {
		if(str == null || str.length() < 6 || str.length() > 30) {
			return false;
		}
		return true;
	}
	
	/**
	 * 是否在数组中
	 */
	public static boolean inArray(List<String> list, String str) {
		for(String existStr : list) {
			if(existStr == null) {
				if(str == null) {
					return true;
				}
			} else if(existStr.equals(str)) {
				return true;
			}
		}
		return false;
	}
}
