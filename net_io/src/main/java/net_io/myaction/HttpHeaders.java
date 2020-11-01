package net_io.myaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class HttpHeaders {
	protected ArrayList<String> names = new ArrayList<String>();
	protected HashMap<String, List<String>> data = new HashMap<String, List<String>>();
	public HttpHeaders() {}

	public static HttpHeaders newInstance(Set<Entry<String, List<String>>> params) {
		HttpHeaders obj = new HttpHeaders();
		for(Entry<String, List<String>> item : params) {
			String key = item.getKey();
			List<String> val = item.getValue();
			String lowerKey = key.toLowerCase();
			if(obj.data.containsKey(lowerKey)) {
				continue;
			}
			obj.names.add(key);
			obj.data.put(lowerKey, val);
		}
		return obj;
	}
	
	/**
	 * 添加HttpHeader
	 * @param name
	 * @param value
	 */
	public void putHeader(String name, String value) {
		String lowerName = name.toLowerCase();
		List<String> val = data.get(lowerName);
		if(val == null) {
			names.add(name);
			val = new ArrayList<String>();
			data.put(lowerName, val);
		}
		val.add(value);
	}
	
	public String getCookie(String name) {
		String cookie = getFirst("Cookie");
		if(cookie == null || cookie.length() == 0) {
			return null;
		}
		for(String line : cookie.split(";")) {
			line = line.trim();
			if(line.length() == 0) {
				continue;
			}
			String[] arr = line.split(":", 2);
			if(arr.length < 2) {
				continue;
			}
			if(arr[0].equals(name)) {
				return arr[1];
			}
		}
		return null;
	}
	
	/**
	 * 获取所有Header名称
	 * @return 不为null的名称数组
	 */
	public String[] getNames() {
		int size = names.size();
		String[] keys = new String[size];
		for(int i=0; i<size; i++) {
			keys[i] = names.get(i);
		}
		return keys;
	}
	
	/**
	 * 根据名称获取值（列表）
	 * @param name 大小写不敏感
	 * @return 找不到返回null
	 */
	public List<String> get(String name) {
		return data.get(name.toLowerCase());
	}
	
	/**
	 * 根据名称获取第一个值
	 * @param name 大小写不敏感
	 * @return 找不到返回null
	 */
	public String getFirst(String name) {
		List<String> list = data.get(name.toLowerCase());
		if(list != null && list.size() > 0) {
			return list.get(0);
		}
		return null;
	}
	
	/**
	 * 检查名称是否存在
	 * @param name 大小写不敏感
	 * @return true OR false
	 */
	public boolean containsKey(String name) {
		return data.containsKey(name.toLowerCase());
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(String name : names) {
			sb.append(name);
			sb.append(": ");
			List<String> list =  data.get(name.toLowerCase());
			if(list != null) {
				sb.append(list.toString());
			}
			sb.append("\n");
		}
		return sb.toString();
	}


}
