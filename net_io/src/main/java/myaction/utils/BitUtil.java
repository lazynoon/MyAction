package myaction.utils;

public class BitUtil {
	public static byte set(byte num, int bitPos, boolean value) {
		int flag = 1 << bitPos;
		if(value) {
			num |= flag;
		} else {
			num &= ~flag;
		}
		return num;
	}
	
	public static short set(short num, int bitPos, boolean value) {
		int flag = 1 << bitPos;
		if(value) {
			num |= flag;
		} else {
			num &= ~flag;
		}
		return num;
	}

	public static int set(int num, int bitPos, boolean value) {
		int flag = 1 << bitPos;
		if(value) {
			num |= flag;
		} else {
			num &= ~flag;
		}
		return num;
	}
	public static long set(long num, int bitPos, boolean value) {
		int flag = 1 << bitPos;
		if(value) {
			num |= flag;
		} else {
			num &= ~flag;
		}
		return num;
	}
	
	public static boolean get(byte num, int bitPos) {
		return ((num >>> bitPos) & 0x1) == 1;
	}
	public static boolean get(short num, int bitPos) {
		return ((num >>> bitPos) & 0x1) == 1;
	}
	public static boolean get(int num, int bitPos) {
		return ((num >>> bitPos) & 0x1) == 1;
	}
	public static boolean get(long num, int bitPos) {
		return ((num >>> bitPos) & 0x1) == 1;
	}
}
