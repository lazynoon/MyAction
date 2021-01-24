package myaction.utils;

import java.security.MessageDigest;
import java.util.UUID;

@Deprecated
public class MD5Util {
	public static String md5(String string) {
		return md5(string, null);
	}
		
	public static String md5(String string, String charset) {
		try{
			byte[] bytes = null; 
			if(charset == null || charset.length() == 0) {
				bytes = string.getBytes();
			} else {
				bytes = string.getBytes(charset);
			}
			return md5(bytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static String md5(byte[] bytes) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(bytes);
			byte[] updateBytes = messageDigest.digest();
			int len = updateBytes.length;
			char myChar[] = new char[len * 2];
			int k = 0;
			for (int i = 0; i < len; i++) {
				byte byte0 = updateBytes[i];
				myChar[k++] = hexDigits[byte0 >>> 4 & 0x0f];
				myChar[k++] = hexDigits[byte0 & 0x0f];
			}
			return new String(myChar);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 获取UUID（去除-，32字节）
	 * @return 非空
	 */
	public static String getUUID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString().replace("-","");
	}
}
