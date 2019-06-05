package net_io.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net_io.utils.Mixed.ENTITY_TYPE;


public class JSONUtils {
	public final static String ANONYMOUS_NODE = "__ANONYMOUS_NODE__"; 
	public final static String EMPTY_NAME_NODE = "__EMPTY_NAME_NODE__";
    protected static final char[][] CHARS_ESCAPE = {
        {'\b', 'b'},
        {'\n', 'n'},
        {'\t', 't'},
        {'\f', 'f'},
        {'\r', 'r'},
        {'\"', '\"'},
        {'\\', '\\'},
    };
    
    private final static int SPACE_CHAR = 32;
    private final static String INDENT = "  ";
    
    public static String escape(String str) {
    	int len = str.length();
    	StringBuffer sb = new StringBuffer();
    	for(int i=0; i<len; i++) {
    		char ch = str.charAt(i);
    		int j = 0;
    		for(; j<CHARS_ESCAPE.length; j++) {
    			if(ch == CHARS_ESCAPE[j][0]) {
    				sb.append('\\');
    				sb.append(CHARS_ESCAPE[j][1]);
    				break;
    			}
    		}
    		//非特殊字符
    		if(j >= CHARS_ESCAPE.length) {
    			sb.append(ch);
    		}
    	}
    	return sb.toString();
    }

    public static String unescape(String str) {
    	int len = str.length();
    	StringBuffer sb = new StringBuffer();
    	for(int i=0; i<len; i++) {
    		char ch = str.charAt(i);
    		if(ch == '\\') {
    			if(++i == len) {
    				//TODO: Exception
    				break; //忽略最后一个 \
    			}
    			ch = str.charAt(i);
    			if(ch == 'u') { //紧跟着4字节的16进制数
    				if(i + 4 >= len) {
        				//TODO: Exception
        				break; //忽略最后一个 \\u
    				}
    				sb.append((char)Integer.parseInt(str.substring(i+1, i+5), 16));
    				i += 4;
    			} else { //转义字符
    	    		int j = 0;
    	    		for(; j<CHARS_ESCAPE.length; j++) {
    	    			if(ch == CHARS_ESCAPE[j][1]) {
    	    				sb.append(CHARS_ESCAPE[j][0]);
    	    				break;
    	    			}
    	    		}
    	    		//非特殊字符
    	    		if(j >= CHARS_ESCAPE.length) {
    	    			sb.append(ch); //转义字符表查不到，直接保存原文
    	    		}
    				
    			}
    		} else {
    			sb.append(ch);
    		}
    	}
    	return sb.toString();
    }


	private byte[] data = null;
	private int offset = 0;
	private JSONUtils(byte[] data) {
		this.data = data;
		this.offset = 0;
	}
	public static Document toDOM(Mixed result) throws ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element rootElement = doc.createElement("ROOT");
		doc.appendChild(rootElement);
		processOneNode(doc, result, rootElement);
		return doc;
	}
	
	private static void processOneNode(Document doc, Mixed result, Element element) {
		Mixed.ENTITY_TYPE type = result.type();
		
		if(type == Mixed.ENTITY_TYPE.LIST) {
			for(int i=0; i<result.size(); i++) {
				Element el = doc.createElement(ANONYMOUS_NODE);
				element.appendChild(el);
				processOneNode(doc, result.get(i), el);
			}
		} else if(type == Mixed.ENTITY_TYPE.MAP) {
			String[] keys = result.keys();
			for(int i=0; i<keys.length; i++) {
				String nodeName = keys[i];
				if(nodeName.length() == 0) {
					nodeName = EMPTY_NAME_NODE;
				}
				Element el = doc.createElement(nodeName);
				element.appendChild(el);
				processOneNode(doc, result.get(keys[i]), el);
			}
		} else {
			element.appendChild(doc.createTextNode(result.toString()));
		}
	}
	
	public static String toJSON(Mixed result) {
		return _toJSON(result, null);
	}
	
	public static String toJSON(Mixed result, boolean perfectFormat) {
		if(perfectFormat) {
			return _toJSON(result, "");
		} else {
			return _toJSON(result, null);
		}
	}
	
	private static String _toJSON(Mixed result, String indent) {
		if(result == null) {
			return "null";
		}
		String childIndent = null;
		if(indent != null) {
			childIndent = indent + INDENT;
		}
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		switch(result.type()) {
		case PRIMITIVE:
			sb.append(result.toString());
			break;
		case STRING:
			sb.append('"');
			sb.append(escape(result.toString()));
			sb.append('"');
			break;
		case LIST:
			sb.append('[');
			first = true;
			for(int i=0; i<result.size(); i++) {
				Mixed item = result.get(i);
				if(first) {
					first = false;
				} else {
					sb.append(',');
				}
				if(childIndent != null) {
					ENTITY_TYPE childType = item.type();
					if(childType != ENTITY_TYPE.LIST && childType != ENTITY_TYPE.MAP) {
						sb.append('\n');
						sb.append(childIndent);						
					}
					sb.append(_toJSON(item, childIndent));
				} else {
					sb.append(_toJSON(item, childIndent));
				}
			}
			if(indent != null) {
				sb.append("\n");
				sb.append(indent);
			}
			sb.append(']');
			break;
		case MAP:
			sb.append('{');
			first = true;
			for(String key : result.keys()) {
				if(first) {
					first = false;
				} else {
					sb.append(',');
				}
				Mixed item = result.get(key);
				if(childIndent != null) {
					ENTITY_TYPE childType = item.type();
					if(childType != ENTITY_TYPE.LIST && childType != ENTITY_TYPE.MAP) {
						sb.append('\n');
						sb.append(childIndent);						
					}
					sb.append('"');
					sb.append(key);
					sb.append("\": ");
					sb.append(_toJSON(item, childIndent));
				} else {
					sb.append('"');
					sb.append(key);
					sb.append("\":");
					sb.append(_toJSON(item, childIndent));
				}
			}
			if(indent != null) {
				sb.append('\n');
				sb.append(indent);
			}
			sb.append('}');
			break;
		default:
			sb.append("null");
		}
		return sb.toString();

	}
	
	public static Mixed parseJSON(String str) throws JSONException {
		if(str == null) {
			return null;
		}
		try {
			return new JSONUtils(str.getBytes("UTF-8")).parseStart();
		} catch (UnsupportedEncodingException e) {
			throw new JSONException("UnsupportedEncoding - "+e.getMessage());
		}
	}
	
	/**
	 * 过滤空白字符
	 */
	private void filterEmptyChar() {
		for(; offset<data.length; offset++) {
			if(data[offset] > SPACE_CHAR) {
				break;
			}
		}
	}

	private Mixed parseStart() throws JSONException {
		Mixed ret = null;
		filterEmptyChar(); //忽略全部空白字符
		if(offset >= data.length) {
			return null;
		}
		switch(data[offset]) {
		case '{':
			ret = parseObject();
			break;
		case '[':
			ret = parseArray();
			break;
		case '"':
			ret = parseString();
			break;
		default:
			ret = parsePrimitive();
		}
		return ret;
	}
	
	private Mixed parseObject() throws JSONException {
		@SuppressWarnings("rawtypes")
		Mixed map = new Mixed(new HashMap());
		String key = null;
		offset++; //跳过({)
		while(offset < data.length) {
			filterEmptyChar(); //过滤空白字符
			//对象结束符“}”检查
			if(offset >= data.length || data[offset] == '}') {
				offset++;
				return map;
			}
			//解析KEY
			key = parseKey().toString();
			filterEmptyChar(); //过滤空白字符
			//解析Value
			if(offset >= data.length || data[offset] != ':') {
				throw new JSONException("Miss the (:).");
			}
			offset++; //跳过(:)
			Object ret = parseStart();
			//保存结果
			map.set(key, ret);
			filterEmptyChar(); //忽略全部空白字符
			//下一元素处理
			if(data[offset] == ',') {
				offset++;
				continue;
			}
			break;
		}
		offset++; //跳过(})
		
		return map;
	}
	private Mixed parseArray() throws JSONException {
		@SuppressWarnings("rawtypes")
		Mixed list = new Mixed(new ArrayList());
		offset++; //跳过([)
		while(offset < data.length) {
			filterEmptyChar(); //忽略全部空白字符
			//数组结束符“]”检查
			if(offset >= data.length || data[offset] == ']') {
				offset++; //跳过(])
				return list;
			}
			Object ret = parseStart();
			//保存结果
			list.add(ret);
			filterEmptyChar(); //忽略全部空白字符
			//下一元素处理
			if(data[offset] == ',') {
				offset++;
				continue;
			}
			break;
		}
		offset++; //跳过(])
		return list;
	}
	private Mixed parseKey() throws JSONException {
		//存在单引号或双引号作为定界符，则按正常的字符串解析
		if(data[offset] == '"' || data[offset] == '\'') {
			return parseString();
		}
		int start = offset;
		try {
			while(offset < data.length) {
				if(data[offset] <= SPACE_CHAR || data[offset] == ':') {
					String str = new String(data, start, offset-start, "UTF-8");
					str = unescape(str);
					//要求：offset最后停留（:）
					while(offset < data.length && data[offset] != ':') {
						offset++;
					}
					return new Mixed(str);
				}
				if(data[offset] == '\\') {
					offset += 2;
				} else {
					offset++;
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new JSONException("UnsupportedEncoding - "+e.getMessage());
		}
		throw new JSONException("Format error. not found (\").");
	}

	private Mixed parseString() throws JSONException {
		if(data[offset] != '"' && data[offset] != '\'') {
			throw new JSONException("String data type must start with (\" or ').");
		}
		byte limitChar = data[offset];
		offset++; //跳过第1个(")
		int start = offset;
		try {
			while(offset < data.length) {
				if(data[offset] == limitChar) {
					String str = new String(data, start, offset-start, "UTF-8");
					str = unescape(str);
					offset++; //跳过第2个(")
					return new Mixed(str);
				}
				if(data[offset] == '\\') {
					offset += 2;
				} else {
					offset++;
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new JSONException("UnsupportedEncoding - "+e.getMessage());
		}
		throw new JSONException("Format error. not found ("+(limitChar=='"'?'"':'\'')+").");
	}
	private Mixed parsePrimitive() throws JSONException {
		filterEmptyChar(); //忽略全部空白字符
		int start = offset;
		for(; offset<data.length; offset++) {
			if(data[offset] <= SPACE_CHAR || data[offset] == ',' || data[offset] == ']' || data[offset] == '}') {
				break;
			}
		}
		if(offset == start) {
			throw new JSONException("Primitive data type can not empty.");
		}
		try {
			String str = new String(data, start, offset-start, "UTF-8");
			if(str.equalsIgnoreCase("null")) {
				return null;
			} else if(str.equalsIgnoreCase("true")) {
				return new Mixed(new Boolean(true));
			} else if(str.equalsIgnoreCase("false")) {
				return new Mixed(new Boolean(false));
			} else {
				return new Mixed(str);
			} 
		} catch (UnsupportedEncodingException e) {
			throw new JSONException("UnsupportedEncoding - "+e.getMessage());
		}

	}
	
	
	
}
