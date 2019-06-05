package myaction.utils;

public class MoneyUtil {
	
	public static int toCent(float yuan) {
		return Math.round(yuan * 100);
	}
	
	public static float toYuan(int cent) {
		return cent / 100f;
	}
}
