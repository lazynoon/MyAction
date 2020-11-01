/**
 * 敏感字段过滤
 * 
 * @author Hansen
 * @date 2016-07-10
 */
package myaction.utils;

public class SensitiveUtil {
	/**
	 * 过滤电话号码或手机号
	 * @param phone 电话号码或手机号
	 * @return 脱敏后的号码
	 */
	public static String filterPhone(String phone) {
		if(phone == null) { 	return phone; }
		int len = phone.length();
		if(len <= 3) { return phone; }
		String postfix = phone.substring(len - 3, len);
		if(len <= 7) {
			return "****".substring(0, len - 3) + postfix;
		}
		return phone.substring(0, len-7) + "****" + postfix;
	}

	/**
	 * 过滤名称
	 * @param name 帐号名或姓名
	 * @return 脱敏后的名称
	 */
	public static String filterName(String name) {
		if(name == null) { 	return name; }
		int len = name.length();
		if(len <= 1) { return name; }
		//50%加密，余数加一
		int encLen = len / 2;
		if(len % 2 != 0) {
			encLen++;
		}
		int start = len / 2 - encLen / 2;
		int end = start + encLen;
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<len; i++) {
			if(i < start || i >= end) {
				sb.append(name.charAt(i));
			} else {
				sb.append("*");
			}
		}
		return sb.toString();
	}
}
