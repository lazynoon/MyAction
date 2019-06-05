package net_io.myaction;

import java.util.HashMap;

public class Session {
	private HashMap<String, Object> attributes = new HashMap<String, Object>(); 

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}

}
