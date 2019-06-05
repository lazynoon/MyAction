package myaction.extend;

import myaction.utils.DateUtil;

public class MyLog {
	public static void println(String str) {
		System.out.println(DateUtil.getDateTime() + " - "+str);
	}
}
