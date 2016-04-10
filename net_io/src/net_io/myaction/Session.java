package net_io.myaction;

import java.util.HashMap;

public class Session {
	private HashMap<Object, Object> attributes = new HashMap<Object, Object>(); 

	public Object getAttribute(Object key) {
		return attributes.get(key);
	}

	public Object setAttribute(Object key, Object value) {
		return attributes.put(key, value);
	}

}
