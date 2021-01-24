package net_io.myaction.tool;

import java.util.ArrayList;
import java.util.HashMap;

public class HttpHeader {
	private static final String[] stringArr = new String[0];
//	private String method = null;
	private ArrayList<String> names = new ArrayList<String>();
	private HashMap<String, String> values = new HashMap<String, String>();
	private HttpHeader() {}
	
	public static HttpHeader parse(String head) {
		HttpHeader obj = new HttpHeader();
		String statusLine = getHeadLine(head, 0);
		int offset = statusLine.length() + 2; //offset增加\r\n
		while(true) {
			String line = getHeadLine(head, offset);
			if(line == null) {
				break;
			}
			offset += line.length() + 2; //offset增加\r\n
			int pos = line.indexOf(':');
			if(pos > 0 && pos < line.length() - 1) {
				String name = line.substring(0, pos);
				String value;
				if(line.charAt(pos + 1) == ' ') { //跳过空格
					value = line.substring(pos + 2);
				} else {
					value = line.substring(pos + 1);
				}
				String nameUP = name.toUpperCase();
				//TODO: 特殊name处理。如COOKIE
				if(obj.values.containsKey(nameUP)) {
					continue; //忽略重复名称
				}
				obj.names.add(name);
				obj.values.put(nameUP, value);
			} else {
				//ignore error key-value
			}
		}
		return obj;
	}
	
	public String[] getHeaderNames() {
		return names.toArray(stringArr);
	}
	
	public String getHeader(String name) {
		return values.get(name.toUpperCase());
	}
	
	private static String getHeadLine(String head, int offset) {
		if(offset >= head.length()) {
			return null;
		}
		int pos = head.indexOf('\n', offset);
		if(pos < 1) {
			return head.substring(offset);
		} else {
			return head.substring(offset, pos-1);
		}
	}
}
