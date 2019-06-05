package myaction.utils;

public class ByteUnitUtil {
	private static long unit_G = 1024*1024*1024;
	private static long unit_M = 1024*1024;
	private static long unit_K = 1024;
	
	public static String format(long size) {
		if(size >= unit_G) {
			return convert(size, unit_G) + "GB";
		}
		if(size >= unit_M) {
			return convert(size, unit_M) + "MB";
		}
		if(size >= unit_K) {
			return convert(size, unit_K) + "KB";
		}
		return size + "B";
	}
	
	private static String convert(long size, long unit) {
		String str = String.valueOf(size / unit);
		double a = size % unit;
		a = a / unit;
		long b = Math.round(a * 100);
		if(b == 0) {
			return str;
		}
		if(b % 10 == 0) {
			return str + "." + (b / 10);
		}
		if(b < 10) {
			return str + ".0" + b;
		} else {
			return str + "." + b;
		}
	}
}
