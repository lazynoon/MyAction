package net_io.utils;

public class ByteUtils {
	/** long型首字节二进制全1数 **/
	private static final long MASK_LONG_1_BYTE = 0xFFL;
	/** int型首字节二进制全1数 **/
	private static final int MASK_INT_1_BYTE = 0xFF;

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

	/**
	 * 从字节数组，按高字节在前的存储方式，解析一个 long 型数字
	 *   注意：若取数的位置，不在字节数组的范围内，抛出异常 IndexOutOfBoundsException
	 * @param data 字节数组
	 * @param offset 解析数据的偏移量
	 * @param byteCount 取字节数量（最小1，最大8）
	 * @return long型数字（允许负数）
	 */
	public static long parseLongAsBigEndian(byte[] data, int offset, int byteCount) {
		if(byteCount <= 0 || byteCount > 8) {
			throw new IndexOutOfBoundsException("Can not parse long number. byteCount: " + byteCount);
		}
		if(data == null || offset < 0 || offset + byteCount > data.length) {
			throw new IndexOutOfBoundsException("Can not parse long number. offset: " + offset);
		}
		long result = 0;
		int k = (byteCount - 1) * 8;
		while(k >= 0) {
			long num = ((long) data[offset++]) & MASK_LONG_1_BYTE;
			if(k > 0) {
				result |= num << k;
			} else {
				result |= num;
			}
			k -= 8;
		}
		return result;
	}

	/**
	 * 从字节数组，按高字节在前的存储方式，解析一个 int 型数字
	 *   注意：若取数的位置，不在字节数组的范围内，抛出异常 IndexOutOfBoundsException
	 * @param data 字节数组
	 * @param offset 解析数据的偏移量
	 * @param byteCount 取字节数量（最小1，最大4）
	 * @return int型数字（允许负数）
	 */
	public static int parseIntAsBigEndian(byte[] data, int offset, int byteCount) {
		if(byteCount <= 0 || byteCount > 4) {
			throw new IndexOutOfBoundsException("Can not parse integer number. byteCount: " + byteCount);
		}
		if(data == null || offset < 0 || offset + byteCount > data.length) {
			throw new IndexOutOfBoundsException("Can not parse integer number. offset: " + offset);
		}
		int result = 0;
		int k = (byteCount - 1) * 8;
		while(k >= 0) {
			int num = ((int) data[offset++]) & MASK_INT_1_BYTE;
			if(k > 0) {
				result |= num << k;
			} else {
				result |= num;
			}
			k -= 8;
		}
		return result;
	}

	/**
	 * 从字节数组，按高字节在前的存储方式，解析一个 short 型数字
	 *   注意：若取数的位置，不在字节数组的范围内，抛出异常 IndexOutOfBoundsException
	 * @param data 字节数组
	 * @param offset 解析数据的偏移量
	 * @param byteCount 取字节数量（最小1，最大2）
	 * @return short型数字（允许负数）
	 */
	public static short parseShortAsBigEndian(byte[] data, int offset, int byteCount) {
		if(byteCount <= 0 || byteCount > 2) {
			throw new IndexOutOfBoundsException("Can not parse short number. byteCount: " + byteCount);
		}
		if(data == null || offset < 0 || offset + byteCount > data.length) {
			throw new IndexOutOfBoundsException("Can not parse short number. offset: " + offset);
		}
		int result = ((int) data[offset]) & MASK_INT_1_BYTE;
		if(byteCount > 1) {
			result <<= 8;
			result |= ((int) data[offset+1]) & MASK_INT_1_BYTE;
		}
		return (short)result;
	}

	/**
	 * 从字节数组，按高字节在前的存储方式，解析一个 long 型数字
	 *   注意：若取数的位置，不在字节数组的范围内，抛出异常 IndexOutOfBoundsException
	 * @param data 字节数组
	 * @param offset 解析数据的偏移量
	 * @param byteCount 取字节数量（最小1，最大8）
	 * @return 解析数据的偏移量
	 */
	public static int writeNumberAsBigEndian(long num, byte[] data, int offset, int byteCount) {
		if(byteCount <= 0 || byteCount > 8) {
			throw new IndexOutOfBoundsException("Can not write long number. byteCount: " + byteCount);
		}
		if(data == null || offset < 0 || offset + byteCount > data.length) {
			throw new IndexOutOfBoundsException("Can not write long number. offset: " + offset);
		}
		int k = (byteCount - 1) * 8;
		while(k >= 0) {
			data[offset++] = (byte) ((num >>> k)  & MASK_LONG_1_BYTE);
			k -= 8;
		}
		return offset;
	}


}
