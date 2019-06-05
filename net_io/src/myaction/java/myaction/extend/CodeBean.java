package myaction.extend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class CodeBean {
	private LinkedHashMap<String, String> codeMap = new LinkedHashMap<String, String>();
	private HashMap<String, String> nameMap = new HashMap<String, String>();
	
	/**
	 * 获取Code的名称
	 * @param code
	 * @return name
	 */
	public String getName(String code) {
		return codeMap.get(code);
	}

	/**
	 * 获取Code的名称
	 * @param code
	 * @return name
	 */
	public String getName(int code) {
		return codeMap.get(String.valueOf(code));
	}

	public boolean contains(String code) {
		return codeMap.containsKey(code);
	}
	public boolean contains(int code) {
		return codeMap.containsKey(String.valueOf(code));
	}
	
	/**
	 * 获取所有的Code
	 * @param code
	 * @return code list
	 */
	public List<String> getCodeList() {
		ArrayList<String> codes = new ArrayList<String>();
		for(String code : codeMap.keySet()) {
			codes.add(code);
		}
		return codes;
	}
	
	public String getCode(String name) {
		if(name == null) {
			return null;
		}
		return nameMap.get(name);
	}
	
	/**
	 * 配置“代码”，“名称”组合（重复code自动忽略）
	 * @param code
	 * @param name
	 */
	protected void config(int code, String name) {
		config(String.valueOf(code), name);
	}

	/**
	 * 配置“代码”，“名称”组合（重复code自动忽略）
	 * @param code
	 * @param name
	 */
	protected void config(String code, String name) {
		if(code == null || name == null) {
			throw new NullPointerException("The parameter of 'code' or 'name' is null.");
		}
		if(codeMap.containsKey(code)) {
			return;
		}
		codeMap.put(code, name);
		if(nameMap.containsKey(name) == false) {
			nameMap.put(name, code);
		}
	}
}
