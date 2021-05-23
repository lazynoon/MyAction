package net_io.utils;

import java.util.*;

import javax.xml.parsers.ParserConfigurationException;


import net_io.mixed.POJOConverter;
import org.w3c.dom.Document;


public class Mixed {
	public enum ENTITY_TYPE{NULL, STRING, LIST, MAP, MIXED, BYTES, PRIMITIVE};
	private Object data = null;

	public Mixed() {
	}

	public Mixed(ENTITY_TYPE entityType) {
		if (entityType == ENTITY_TYPE.LIST) {
			data = new ArrayList<Mixed>();
		} else if (entityType == ENTITY_TYPE.MAP) {
			data = new LinkedHashMap<String, Mixed>();
		}
	}
	public Mixed(Object value) {
		_reset(value, null);
	}

	public Mixed(Object value, POJOConverter pojoConverter) {
		_reset(value, pojoConverter);
	}
	public ENTITY_TYPE type() {
		if(data == null) {
			return ENTITY_TYPE.NULL;
		} else if(data instanceof Number || data instanceof Boolean || data instanceof Character) {
			return ENTITY_TYPE.PRIMITIVE;
		} else if(data instanceof List) {
			return ENTITY_TYPE.LIST;
		} else if(data instanceof Map) {
			return ENTITY_TYPE.MAP;
		} else if(data instanceof TYPE) {
			return ENTITY_TYPE.MIXED;
		} else if(data instanceof byte[]) {
			return ENTITY_TYPE.BYTES;
		} else {
			return ENTITY_TYPE.STRING;
		}
		
	}
	
//	public String getString() {
//		return toString();
//	}
	
	/**
	 * 获取字符串的值。默认为空字符串
	 * @param key
	 * @return 不存在时，返回空字符串
	 */
	public String getString(String key) {
		return getString(key, "");
	}

	/**
	 * 获取字符串的值
	 * @param key KEY
	 * @param defaultValue 默认值（不存在时返回）
	 * @return String
	 */
	public String getString(String key, String defaultValue) {
		Mixed result = _get(key);
		if(result == null || result.data == null) {
			return defaultValue;
		}
		if (result.data instanceof String) {
			return (String) result.data;
		} else {
			return result.data.toString();
		}
	}

	/**
	 * 获取 byte[] 字节数组对象
	 * @param key KEY
	 * @return 不存在返回null；原保存的对象并非 byte[] 类型时，先转成 String 类型再取 byte 数组
	 */
	public byte[] getBytes(String key) {
		Mixed ret = _get(key);
		if(ret == null || ret.data == null) {
			return null;
		}
		if(ret.data instanceof byte[]) {
			return (byte[]) ret.data;
		}
		return ret.data.toString().getBytes();
	}

	/**
	 * 获取 short 类型的值
	 * @param key KEY
	 * @return 不存在时，返回 0
	 */
	public short getShort(String key) {
		return MixedUtils.parseShort(getString(key, ""));
	}

	/**
	 * 获取 int 类型的值
	 * @param key KEY
	 * @return 不存在时，返回 0
	 */
	public int getInt(String key) {
		return MixedUtils.parseInt(getString(key, ""));
	}

	/**
	 * 获取 long 类型的值
	 * @param key KEY
	 * @return 不存在时，返回 0
	 */
	public long getLong(String key) {
		return MixedUtils.parseLong(getString(key, ""));
	}

	/**
	 * 获取 float 类型的值
	 * @param key KEY
	 * @return 不存在时，返回 0
	 */
	public float getFloat(String key) {
		return MixedUtils.parseFloat(getString(key, ""));
	}

	/**
	 * 获取 double 类型的值
	 * @param key KEY
	 * @return 不存在时，返回 0
	 */
	public double getDouble(String key) {
		return MixedUtils.parseDouble(getString(key, ""));
	}

	/**
	 * 计算布尔值
	 * 	 以下任意条件为 false：
	 * 	 1. key 不存在
	 * 	 2. value 为 boolean 类型且 false
	 * 	 3. value 空值：空字符串，空Map对象，空List
	 * 	 4. value 为 字符串 false（英文字母大小写不敏感）
	 * 	 5. value 为 数字类型 0
	 * 	 6. value 为 字符串类型 0 （不含不可见字符，如空格）
	 * @param key KEY
	 * @return true OR false
	 */
	public boolean getBoolean(String key) {
		Mixed result = _get(key);
		if(result == null) {
			return false;
		}
		return toBooleanValue();
	}

	public boolean toBooleanValue() {
		if (data == null) {
			return false;
		}
		if (data instanceof Boolean) {
			return ((Boolean) (data)).booleanValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return false;
		}
		if ("0".equalsIgnoreCase(str)) {
			return false;
		}
		if ("false".equalsIgnoreCase(str)) {
			return false;
		}
		return true;
	}

	public byte toByteValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Byte) {
			return ((Byte) data).byteValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Byte.parseByte(str);
	}

	public char toCharValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Character) {
			return ((Character) data).charValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return (char) Integer.parseInt(str);
	}

	public short toShortValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Short) {
			return ((Short) data).shortValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Short.parseShort(str);
	}

	public int toIntValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Integer) {
			return ((Integer) data).intValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Integer.parseInt(str);
	}

	public long toLongValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Long) {
			return ((Long) data).longValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Long.parseLong(str);
	}

	public float toFloatValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Float) {
			return ((Float) data).floatValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Float.parseFloat(str);
	}

	public double toDoubleValue() {
		if (data == null) {
			return 0;
		}
		if (data instanceof Double) {
			return ((Double) data).doubleValue();
		}
		String str = data.toString();
		if (str.length() == 0) {
			return 0;
		}
		return Double.parseDouble(str);
	}

	/**
	 * 当前对象的内部数据，是否null
	 * @return boolean
	 */
	public boolean isSelfNull() {
		if(data == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 当前对象的内部数据，是否空值（范围：null或空值）
	 * @return boolean
	 */
	public boolean isSelfEmpty() {
		if(data == null) {
			return true;
		}
		if(data instanceof String) {
			return ((String)data).length() == 0;
		} else {
			return false;
		}
	}

	/**
	 * 当前对象的内部数据，是否true
	 *     排除范围：null、空字符串、0（含String类型）、false(大小写不敏感)
	 * @return boolean
	 */
	public boolean isSelfTrue() {
		if(data == null) {
			return false;
		}
		String str;
		switch(type()) {
			case STRING:
				str = (String) data;
				break;
			case PRIMITIVE:
				str = data.toString();
				break;
			default:
				return true;
		}
		if(str.length() == 0 || str.equals("0") || str.equalsIgnoreCase("false")) {
			return false;
		}
		return true;
	}

	/**
	 * 当前对象的内部数据，是否数字
	 * @return boolean
	 */
	public boolean isSelfNumeric() {
		if(data == null) {
			return false;
		}
		boolean ret = false;
		switch(type()) {
			case STRING:
				ret = MixedUtils.isNumeric((String) data);
				break;
			case PRIMITIVE:
				if(!(data instanceof Boolean)) {
					ret = true; //排除布尔型，都是数字
				}
				break;
		}
		return ret;
	}

	/**
	 * 指定KEY，是否null（KEY不存在，也按null处理）
	 * @param key
	 * @return boolean
	 */
	public boolean isNull(String key) {
		Mixed result = _get(key);
		if(result == null) {
			return true;
		}
		return result.isSelfNull();
	}

	/**
	 * 指定KEY，是否为空字符串
	 * @param key
	 */
	public boolean isEmpty(String key) {
		Mixed result = _get(key);
		if(result == null) {
			return true;
		}
		return result.isSelfEmpty();
	}

	/**
	 * 指定KEY，是否为TRUE
	 * @return 返回false的范围：null、空字符串、0（含String类型）、false(大小写不敏感)；除此之外，都返回true
	 */
	public boolean isTrue(String key) {
		Mixed result = _get(key);
		if(result == null) {
			return false;
		}
		return result.isSelfTrue();
	}
	
	/**
	 * 指KEY，是否为数字
	 * @param key
	 */
	public boolean isNumeric(String key) {
		Mixed result = _get(key);
		if(result == null) {
			return false;
		}
		return result.isSelfNumeric();
	}

	/**
	 * 是否存在key
	 * @param key
	 * @return boolean
	 */
	@SuppressWarnings("unchecked")
	public boolean containsKey(String key) {
		if(data != null && data instanceof Map<?, ?>) {
			return ((Map<String, Mixed>)data).containsKey(key);
		}
		return false;
	}
	
	public String getString(int index) {
		Mixed result = get(index);
		if(result == null) {
			return "";
		}
		return result.toString();
	}
	
	public int size() {
		if(data == null) {
			return 0;
		} else if(data instanceof List) {
			return ((List<?>)data).size();
		} else if(data instanceof Map) {
			return ((Map<?, ?>)data).size();
		} else {
			return 0;
		}
	}
	
	public String[] keys() {
		String[] keys;
		if(data == null) {
			keys = new String[0];
		} else if(data instanceof List) {
			List<Mixed> list = (List<Mixed>) data;
			keys = new String[list.size()];
			for(int i=0; i<keys.length; i++) {
				keys[i] = String.valueOf(i);
			}
		} else if (data instanceof Map) {
			Map<String, Mixed> map = (Map<String, Mixed>) data;
			keys = new String[map.size()];
			int i = 0;
			for (String key : map.keySet()) {
				keys[i++] = key;
			}
		} else {
			keys = new String[0];
		}
		return keys;
	}
	
	/**
	 * 获取Mixed对象中的核心Object对象
	 * @return 原始的Object对象（可为null）
	 */
	public Object getCoreObject() {
		return data;
	}
	public Mixed get(String key) {
		Mixed ret = _get(key);
		if(ret == null) ret = new Mixed();
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private Mixed _get(String key) {
		if(data == null) {
			return null;
		}
		if(data instanceof Map<?, ?>) {
			return ((Map<String, Mixed>)data).get(key);
		} else if(data instanceof List<?>) {
			if(MixedUtils.isNumeric(key) == false) {
				return null;
			}
			int index = Integer.parseInt(key); 
			List<Mixed> list = (List<Mixed>) data;
			if(index < 0 || index >= list.size()) {
				return null;
			}
			return list.get(index);
		}
		return null;
	}
	
	public Mixed get(int index) {
		Mixed ret = _get(index);
		if(ret == null) ret = new Mixed();
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private Mixed _get(int index) {
		Mixed ret = null;
		if(data != null && data instanceof List<?>) {
			if(((List<Mixed>)data).size() > index) {
				ret = ((List<Mixed>)data).get(index);
			}
		}
		return ret;
	}
	public Mixed find(String path) {
		String[] arr = path.split("/");
		Mixed result = this;
		for(String key : arr) {
			key = key.trim();
			if(key.length() == 0) continue; //ignore the empty key.
			result = findMe(result, key);
			if(result == null) return new Mixed(); //can not find the ActionResult
		}
		return result;
	}
	
	protected Mixed findMe(Mixed result, String key) {
		if(result.data == null) {
			return new Mixed();
		} else if(result.data instanceof List) {
			return result.get(MixedUtils.parseInt(key));
		} else {
			return result.get(key);
		}
	}

	@Deprecated
	public Mixed set(Object value) {
		return reset(value);
	}

	public Mixed reset(Object value) {
		return _reset(value, null);
	}

	@SuppressWarnings("unchecked")
	private Mixed _reset(Object value, POJOConverter pojoConverter) {
		if(value == null) {
			data = null;
		} else if(value instanceof String) {
			data = value;
		} else if(value instanceof Number || value instanceof Boolean || value instanceof Character) {
			data = value;
		} else if(value instanceof byte[]) {
			data = value;
		} else if(value instanceof Object[]) {
			data = new ArrayList<Mixed>();
			for(Object v : (Object[])value) {
				((ArrayList<Mixed>)data).add(new Mixed(v));
			}
		} else if(value instanceof List) {
			data = new ArrayList<Mixed>();
			for(Object v : (List<?>)value) {
				((ArrayList<Mixed>)data).add(new Mixed(v, pojoConverter));
			}
		} else if(value instanceof Mixed) {
			data = ((Mixed)value).data;
		} else if(value instanceof Map) {
			data = new LinkedHashMap<String, Mixed>();
			for (Object k : ((Map<?, ?>) value).keySet()) {
				String key;
				if (k instanceof String) {
					key = (String) k;
				} else {
					key = k.toString();
				}
				((Map<String, Mixed>) data).put(
						key, new Mixed(((Map<?, ?>) value).get(k), pojoConverter));
			}
		} else if(pojoConverter != null) { //POJO转换器，优先于Mixed.TYPE与默认对象
			data = pojoConverter._toMixedHashMap(value);
		} else if(value instanceof TYPE) {
			Mixed tmp = ((TYPE) value).toMixed();
			data = tmp.data;
		} else {
			data = value.toString();
		}
		return this;
	}
	
	public Mixed put(String key, Object value) {
		return set(key, value);
	}
	@SuppressWarnings("unchecked")
	public Mixed set(String key, Object value) {
		//null也占KEY键。if(value == null) return this;
		if(data == null || !(data instanceof Map<?, ?>)) {
			data = new LinkedHashMap<String, Mixed>();
		}
		if(value instanceof Mixed) { //已经是ActionResult的对象了，不用转换
			((Map<String, Mixed>)data).put(key, (Mixed)value);
//		} else if(value instanceof Integer) { //已经是ActionResult的对象了，不用转换
//			if(((Map<String, Mixed>)data).containsKey(key) == false) { //先检查key是否已存在
//				sequence.add(key);
//			}
//			((Map<String, Mixed>)data).put(key, (Mixed)value);
		} else {
			((Map<String, Mixed>)data).put(key, new Mixed(value));
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public Mixed set(int index, Object value) {
		if(data == null || !(data instanceof List<?>)) {
			data = new ArrayList<Mixed>();
		}
		((List<Mixed>)data).set(index, new Mixed(value));
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public Mixed add(Object value) {
		if(data == null || !(data instanceof List<?>)) {
			data = new ArrayList<Mixed>();
		}
		((List<Mixed>)data).add(new Mixed(value));
		return this;
	}

	@Override
	public String toString() {
		if(data == null) {
			return "";
		} else if (data instanceof String) {
			return (String) data;
		} else if(data instanceof Mixed) {
			Mixed mdata = (Mixed) data;
			if(mdata.data == null) {
				return "";
			}
			if(mdata == mdata.data) {
				throw new RuntimeException("Mixed data is self.");
			}
			return mdata.data.toString();
		} else {
			return data.toString();
		}
	}
	
	public String toJSON() {
		return JSONUtils.toJSON(this);
	}
	public Document toDOM() throws ParserConfigurationException {
		return JSONUtils.toDOM(this);
	}
	
	public static interface TYPE {
		public Mixed toMixed();
	}
}
