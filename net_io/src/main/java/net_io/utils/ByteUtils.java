package net_io.utils;

public class ByteUtils {

	/**
	 * 生成随机字节数组
	 * @param length 字节数组的长度
	 * @return 非空随机字节数组
	 */
	public static byte[] generateRandomBytes(int length) {
		byte[] result = new byte[length];
		long timeHash = System.nanoTime();
		timeHash ^= timeHash >>> 16;
		timeHash ^= timeHash >>> 16;
		int num = ((int) timeHash) & 0xFFFF;
		for(int i=0; i<length; i++) {
			num ^= (int) (Math.round(Math.random() * 0xFFFF));
			result[i] = (byte) num;
		}
		return result;
	}

	/**
	 * 检查两字节数组的值，是否相等
	 *
	 * @param bts1 一维数组1
	 * @param bts2 一维数组2
	 * @return 值相等返回 true，否则返回 false
	 */
	public static boolean isEqual(byte[] bts1, byte[] bts2) {
		if (bts1 == null) {
			if (bts2 == null) {
				return true;
			} else {
				return false;
			}
		} else if (bts2 == null) {
			return false;
		}
		if (bts1.length != bts2.length) {
			return false;
		}
		for (int i = 0; i < bts1.length; i++) {
			if (bts1[i] != bts2[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查两字节数组的值，是否相等
	 *
	 * @param bts1 二维数组1
	 * @param bts2 二维数组2
	 * @return 值相等返回 true，否则返回 false
	 */
	public static boolean isEqual(byte[][] bts1, byte[][] bts2) {
		if (bts1 == null) {
			if (bts2 == null) {
				return true;
			} else {
				return false;
			}
		} else if (bts2 == null) {
			return false;
		}
		if (bts1.length != bts2.length) {
			return false;
		}
		for (int i = 0; i < bts1.length; i++) {
			if (isEqual(bts1[i], bts2[i]) == false) {
				return false;
			}
		}
		return true;
	}

}
