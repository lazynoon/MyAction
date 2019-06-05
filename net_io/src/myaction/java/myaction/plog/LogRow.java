package myaction.plog;

import java.util.LinkedHashMap;
import java.util.Map;

import myaction.utils.MD5Util;
import net.sf.jsqlx.DB;
import net_io.utils.Mixed;

public class LogRow {
	private LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
	private String md5Code;
	private int keyLength = 0;
	private int valueLength = 0;
	protected DB db;
	protected String tableName;
	
	public LogRow(Mixed map) {
		StringBuffer sb = new StringBuffer();
		for(String key : map.keys()) {
			sb.append(key);
			String value = map.getString(key);
			data.put(key, value);
			keyLength += key.length();
			valueLength += value.length();
		}
		md5Code = MD5Util.md5(sb.toString());
	}

	public LogRow(Map<String, Object> map) {
		StringBuffer sb = new StringBuffer();
		for(String key : map.keySet()) {
			sb.append(key);
			Object value = map.get(key);
			keyLength += key.length();
			if(value != null) {
				String s = value.toString();
				valueLength += s.length();
				data.put(key, s);
			} else {
				data.put(key, null);
			}
		}
		md5Code = MD5Util.md5(db + tableName + sb.toString());
	}

	public LinkedHashMap<String, String> getData() {
		return data;
	}

	public int getKeyLength() {
		return keyLength;
	}

	public int getValueLength() {
		return valueLength;
	}

	public String getMd5Code() {
		return md5Code;
	}
	
}
