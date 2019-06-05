package net_io.utils;

public class MixedUtils {
	public static String trim(String str) {
		if(str == null) return "";
		return str.trim();
	}
	
	public static boolean isEmpty(Object obj) {
		if(obj == null) return true;
		if(obj instanceof String && ((String)obj).length() == 0) return true;
		return false;
	}
	
	/**
	 * 解析一个短整数
	 * @param str
	 * @return short
	 */
	public static short parseShort(String str) {
		if(str == null || str.length() == 0  || str.length() > 6) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Short.parseShort(str);
	}

	/**
	 * 解析一个整数
	 * @param str
	 * @return intval
	 */
	public static int parseInt(String str) {
		if(str == null || str.length() == 0  || str.length() > 11) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Integer.parseInt(str);
	}

	/**
	 * 解析一个长整数
	 * @param str
	 * @return longval
	 */
	public static long parseLong(String str) {
		if(str == null || str.length() == 0  || str.length() > 21) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Long.parseLong(str);
	}

	/**
	 * 解析一个双精度数
	 * @param str
	 * @return doubleval
	 */
	public static double parseDouble(String str) {
		if(isNumeric(str) == false) return 0;
		return Double.parseDouble(str);
	}
	
	/**
	 * 解析一个单精度数
	 * @param str
	 * @return floatval
	 */
	public static float parseFloat(String str) {
		if(isNumeric(str) == false) return 0;
		return Float.parseFloat(str);
	}
	
	public static boolean isNumeric(String str) {
		if(str == null || str.length() == 0  || str.length() > 38) return false;
		int pointPos = -1;
		for(int i=0; i<str.length(); i++) {
			if(str.charAt(i) == '.') {
				if(pointPos >= 0) return false; //已经出现了一个"."
				else continue;
			}
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return false;
		}
		if(pointPos == 0 || pointPos == str.length()-1) return false; //"."出现在第一位，或者是末尾都是不对的
		return true;
	}

}
