package myaction.utils;

import java.math.BigDecimal;

public class MathUtil {
	/** 双精度数据的误差（1分钱） **/
	public static final double ONE_CENT_DEVIATION = 0.01;
	
//	public static String round(long num, int decimalNum) {
//		long a = num / 
//	}
	
	public static int parseInt(String str) {
		if(str == null || str.length() == 0) {
			return 0;
		}
		return Integer.parseInt(str);
	}
	
	public static long parseLong(String str) {
		if(str == null || str.length() == 0) {
			return 0;
		}
		return Long.parseLong(str);
	}
	
	/** 金额取整（保留2位小数点） **/
	public static double roundAsMoney(double money) {
		 return (new BigDecimal(money)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(); 
	}
	
	/** 对浮点数进行四舍五入 **/
	public static double round(double num, int num2) {
		 return (new BigDecimal(num)).setScale(num2, BigDecimal.ROUND_HALF_UP).doubleValue(); 
	}
	
	/**
	 * 检查两个金额是否相等（误差1分钱）
	 * @param money1
	 * @param money2
	 * @return boolean
	 */
	public static boolean isMoneyEqual(double money1, double money2) {
		double money3 = money1 - money2;
		if(money3 < -ONE_CENT_DEVIATION || money3 > ONE_CENT_DEVIATION) {
			return false;
		} else {
			return true;
		}
	}
	
	/** 显示金额取整（保留2位小数点）。若余数全为0，则取整输出 **/
	public static String displayMoney(double money) {
		money = roundAsMoney(money);
		String str = String.valueOf(money);
		String[] arr = StringUtil.split(str, '.');
		if(arr.length == 2) {
			for(int i=0; i<arr[1].length(); i++) {
				if(arr[1].charAt(i) != '0') {
					return str;
				}
			}
		} else if(arr.length > 2) {
			return str;
		}
		return arr[0];
	}

}
