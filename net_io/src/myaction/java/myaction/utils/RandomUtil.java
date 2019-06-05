package myaction.utils;

public class RandomUtil {
	private static final char[] CHARATERS = {
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N',
		'O','P','Q','R','S','T','U','V','W','X','Y','Z'
	};
	private static final char[] NUMBERS = {
		'0','1','2','3','4','5','6','7','8','9'
	};
	private static final char[] CHARATERS_AND_NUMBERS = {
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N',
		'O','P','Q','R','S','T','U','V','W','X','Y','Z',
		'0','1','2','3','4','5','6','7','8','9'
	};
	
	/**
	 * 获取随机数的字符串
	 * @param length
	 * @return
	 */
	public static String getRandNumber(int length) {
		return getRandString(NUMBERS, length);
	}
	
	/**
	 * 获取随机英文字符的字符串
	 * @param length
	 * @return
	 */
	public static String getRandEnglish(int length) {
		return getRandString(CHARATERS, length);
	}
	
	/**
	 * 获取随机英数字的字符串
	 * @param length
	 * @return
	 */
	public static String getRandString(int length) {
		return getRandString(CHARATERS_AND_NUMBERS, length);
	}
	
	private static String getRandString(char[] encodeChars, int length) {
		StringBuffer encoding = new StringBuffer();
		for(int i=0; i<length; i+=8) {
			long seed = getRandLongNumber();
			for(int j=0; j<8 && i+j<length; j++) {
				int num = (int) ( (seed >>> (j * 8)) & 0xFF );
				encoding.append(encodeChars[num % encodeChars.length]);
			}
		}
		return encoding.toString();
	}
	
	private static long getRandLongNumber() {
		Double d = new Double((Math.random() * Long.MAX_VALUE));
		long num = d.longValue();
		num ^= System.nanoTime();
		return num;
	}
}
