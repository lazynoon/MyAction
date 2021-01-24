package myaction.utils;

public class SignUtil {
	public static String getLoginSign(String account, long timestamp, int fm, String serverKey) {
		return MD5Util.md5(account + timestamp + fm + serverKey);
	}
}
