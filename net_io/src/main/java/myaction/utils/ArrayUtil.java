package myaction.utils;

import java.util.List;

import net_io.utils.Mixed;

public class ArrayUtil {
	/**
	 * 是否在Array数组内部
	 * @param str
	 * @param list
	 * @return boolean
	 */
	public static boolean inArray(String str, List<String>list) {
		if(list == null || list.size() == 0) {
			return false;
		}
		for(String s2 : list) {
			if(str == s2) {
				return true;
			}
			if(str != null && str.equals(s2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 连接数组
	 * @param arr LIST类型做连接；其它类型，转成String类型
	 * @param glue 分割字符
	 * @return String
	 */
	public static String implode(Mixed arr, String glue) {
		if(arr == null) {
			return null;
		}
		if(arr.type() != Mixed.ENTITY_TYPE.LIST) {
			return arr.toString();
		}
		int size = arr.size();
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<size; i++) {
			if(i > 0) {
				builder.append(glue);
			}
			builder.append(arr.getString(i));
		}
		return builder.toString();
	}
	
	public static Mixed select(Mixed oldRow, String[] keys) {
		Mixed ret = new Mixed();
		for(String key : keys) {
			if(oldRow.containsKey(key)) {
				ret.set(key, oldRow.getString(key));
			}
		}
		return ret;
	}

	
	public static Mixed merge(Mixed oldRow, Mixed newRow) {
		return _merge(11, oldRow, newRow, null);
	}
	
	public static Mixed merge(Mixed oldRow, Mixed newRow, boolean fillMode) {
		if(fillMode) {
			return _merge(12, oldRow, newRow, null);
		} else {
			return _merge(11, oldRow, newRow, null);
		}
	}
	
	public static Mixed merge(Mixed oldRow, Mixed newRow, boolean fillMode, String[] keys) {
		if(fillMode) {
			return _merge(12, oldRow, newRow, keys);
		} else {
			return _merge(11, oldRow, newRow, keys);
		}
	}
	
	public static Mixed join(Mixed oldRow, Mixed newRow) {
		return _merge(21, oldRow, newRow, null);
	}
	
	public static Mixed join(Mixed oldRow, Mixed newRow, boolean fillMode) {
		if(fillMode) {
			return _merge(22, oldRow, newRow, null);
		} else {
			return _merge(21, oldRow, newRow, null);
		}
	}
	
	public static Mixed join(Mixed oldRow, Mixed newRow, boolean fillMode, String[] keys) {
		if(fillMode) {
			return _merge(22, oldRow, newRow, keys);
		} else {
			return _merge(21, oldRow, newRow, keys);
		}		
	}
	
	public static Mixed getChange(Mixed oldRow, Mixed newRow) {
		return _merge(31, oldRow, newRow, null);
	}
	
	public static Mixed getChange(Mixed oldRow, Mixed newRow, boolean fillMode) {
		if(fillMode) {
			return _merge(32, oldRow, newRow, null);
		} else {
			return _merge(31, oldRow, newRow, null);
		}		
	}
	
	public static Mixed getChange(Mixed oldRow, Mixed newRow, boolean fillMode, String[] keys) {
		if(fillMode) {
			return _merge(32, oldRow, newRow, keys);
		} else {
			return _merge(31, oldRow, newRow, keys);
		}		
	}
	
	/**
	 * 合并数字
	 * @param mode 
	 * 		11不管空值，直接merge；12非空merge；
	 * 		21不管空制，直接join；22非空join；
	 * 		31不管空值，有差异返回；32新值非空且有差异返回；
	 * @param oldRow
	 * @param newRow
	 * @param keys
	 * @return
	 */
	private static Mixed _merge(int mode, Mixed oldRow, Mixed newRow, String[] keys) {
		if(keys == null) {
			keys = oldRow.keys();
		}
		Mixed ret = new Mixed();
		int flag1 = mode / 10;
		int flag2 = mode % 10;
		for(String key : keys) {
			String value1 = null;
			String value2 = null;
			if(oldRow.containsKey(key)) {
				value1 = oldRow.getString(key);
			}
			if(newRow.containsKey(key)) {
				value2 = newRow.getString(key);
			}
			if(value1 != null && (mode<30 || value1.equals(value2) == false)) {
				ret.set(key, value1);
			}
			//新数组中不存在
			if(value2 == null) {
				continue;
			}
			//填充数据模式，但是新值为空
			if(flag2 == 2 && value2.length() == 0) {
				continue;
			}
			//join或getChange方法，KEY必须存在于旧数组中
			if(flag1 > 1 && value1 == null) {
				continue;
			}
			//getChange方法，新旧两个值相等
			if(flag1 == 3 && value2.equals(value1)) {
				continue;
			}
			ret.put(key, value2);
		}
		return ret;
	}
	
	/**
	 * 比较两个字节流的值，是否相等
	 * @param bts1
	 * @param bts2
	 * @return boolean
	 */
	public static boolean equals(byte[] bts1, byte[] bts2) {
		if(bts1 == null && bts2 == null) {
			return true;
		}
		if(bts1 == null || bts2 == null) {
			return false;
		}
		if(bts1.length != bts2.length) {
			return false;
		}
		for(int i=0; i<bts1.length; i++) {
			if(bts1[i] != bts2[i]) {
				return false;
			}
		}
		return true;
	}


}
