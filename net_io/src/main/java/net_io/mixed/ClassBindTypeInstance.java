package net_io.mixed;

public class ClassBindTypeInstance {
	public static ClassBindType getInstance(Class<?> cls) {
		ClassBindType bindType = new ClassBindType(cls);
		return bindType;
	}

	/**
	 * 将字符串转换为蛇形命名方式
	 * @return 蛇形命名方式的字符串
	 */
	public static String toSnakeName(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		StringBuilder builder = new StringBuilder();
		boolean prevUnderscore = false;
		int length = str.length();
		for (int i=0; i<length; i++) {
			char ch = str.charAt(i);
			if (ch >= 'A' && ch <= 'Z') {
				if (!prevUnderscore) {
					builder.append("_");
				} else {
					prevUnderscore = false;
				}
				ch += 32;
			} else if (ch == '_') {
				prevUnderscore = true;
			} else {
				if (!prevUnderscore) {
					prevUnderscore = false;
				}
			}
			builder.append(ch);
		}
		return builder.toString();
	}

}
