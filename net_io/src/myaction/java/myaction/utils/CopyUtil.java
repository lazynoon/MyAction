package myaction.utils;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlx.PrepareSQL;
import net.sf.jsqlx.Record;
import net_io.myaction.Request;
import net_io.utils.Mixed;

public class CopyUtil {
	/**
	 * 转换为Mixed
	 * @param row
	 * @return
	 */
	public static Mixed toMixed(Record row) {		
		return new Mixed(row);
	}
	/**
	 * 转换为数组
	 * @param list
	 * @return
	 */
	public static int[] toArray(List<Integer> list) {
		int[] arr = new int[list.size()];
		for(int i=0; i<arr.length; i++) {
			arr[i] = list.get(i).intValue();
		}
		return arr;
	}
	
	public static List<String> toStringList(Mixed list) {
		if(list.type() != Mixed.ENTITY_TYPE.LIST) {
			throw new RuntimeException("Parameter type is not LIST.");
		}
		int size = list.size();
		ArrayList<String> ret = new ArrayList<String>();
		for(int i=0; i<size; i++) {
			ret.add(list.getString(i));
		}
		return ret;
	}
	
	public static String[] duplicate(String[] arr) {
		String[] arr2 = new String[arr.length];
		System.arraycopy(arr, 0, arr2, 0, arr.length);
		return arr2;
	}
	
	/**
	 * 复制一个Mixed对象(默认下一层对象)
	 * @param obj
	 * @return Mixed
	 */
	public static Mixed duplicate(Mixed obj) {
		if(obj == null) {
			return null;
		}
		Mixed newObj = null;
		Mixed.ENTITY_TYPE type = obj.type();
		if(type == Mixed.ENTITY_TYPE.LIST) {
			newObj = new Mixed();
			int cnt = obj.size();
			for(int i=0; i<cnt; i++) {
				newObj.add(obj.get(i));
			}
		} else if(type == Mixed.ENTITY_TYPE.MAP) {
			newObj = new Mixed();
			for(String key : obj.keys()) {
				newObj.set(key, obj.get(key));
			}
		} else {
			newObj = obj;
		}
		return newObj;
	}
	
	/**
	 * 复制一个requst对象并转换为Mixed
	 * @param request
	 * @return 不为空的 Mixed
	 */
	public static Mixed duplicate(Request request) {
		return duplicate(request, null);
	}
	
	/**
	 * 复制一个requst对象并转换为Mixed
	 * @param request
	 * @return 不为空的 Mixed
	 */
	public static Mixed duplicate(Request request, String excludeKeyPrefix) {
		if(excludeKeyPrefix != null && excludeKeyPrefix.length() == 0) {
			excludeKeyPrefix = null;
		}
		Mixed data = new Mixed();
		for(String name : request.getParameterNames()) {
			if(excludeKeyPrefix != null && name.startsWith(excludeKeyPrefix)) {
				continue; //排除指定KEY
			}
			data.put(name, request.getParameter(name));
		}
		return data;
	}
	
	/**
	 * 将record中的数据，全部保存到PrepreSQL中
	 * @param psql
	 * @param record
	 */
	public static void setFieldValue(PrepareSQL psql, Mixed record) {
		for(String key : record.keys()) {
			psql.addFieldValue(key, record.getString(key));
		}
	}

}
