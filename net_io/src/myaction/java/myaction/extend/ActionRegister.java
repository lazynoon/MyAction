package myaction.extend;

import java.util.ArrayList;

import net_io.myaction.ActionFactory;

abstract public class ActionRegister {
	private ActionRegister parent = null;
	private String pathName = null;
	private ArrayList<ActionRegister> childs = new ArrayList<ActionRegister>();
	
	
	protected ActionRegister() {
	}
	protected ActionRegister(String pathName) {
		this.pathName = filterPathName(pathName);
	}
	protected ActionRegister(ActionRegister parent) {
		this.parent = parent;
	}
	protected ActionRegister(ActionRegister parent, String pathName) {
		this.parent = parent;
		this.pathName = filterPathName(pathName);
	}
	
	
	abstract protected void onRegister();
	
	public String getPath() {
		String path;
		if(parent == null) {
			path = "/"; //根路径
		} else {
			path = parent.getPath();
		}
		if(pathName != null) {
			path += pathName + "/";
		}
		return path;
	}
	
	/**
	 * 注册Action类
	 */
	public void doRegister() {
		//当前类的先注册
		onRegister();
		//子类注册
		for(ActionRegister child : childs) {
			child.doRegister();
		}
	}

	protected void addChild(ActionRegister child) {
		childs.add(child);
	}
	
	protected void register(Class<?> clazz) {
		String path = getPath();
		String name = clazz.getName();
		int pos = name.lastIndexOf('.');
		if(pos >= 0) {
			name = name.substring(pos+1);
		}
		if(name.length() <= 6 || name.endsWith("Action") == false) {
			throw new IllegalArgumentException("Action Class Name must be endsWith 'Action'.");
		}
		name = name.substring(0, name.length() - 6);
		char firstChar = name.charAt(0);
		if(firstChar >= 'A' && firstChar <= 'Z') {
			firstChar += 32;
			if(name.length() > 1) {
				name = String.valueOf(firstChar) + name.substring(1);
			} else {
				name = String.valueOf(firstChar);
			}
		}
		path += name;
		ActionFactory.register(path, clazz);
	}

	protected void register(String subName, Class<?> clazz) {
		String path = getPath();
		if(subName != null) {
			subName = filterPathName(subName);
			if(subName != null) {
				path += subName;
			}
		}
		ActionFactory.register(path, clazz);
	}

	protected void addRewriteRule(String regex, String dstPath) {
		ActionFactory.adRewriteRule(regex, dstPath);
	}

	private String filterPathName(String name) {
		if(name != null) {
			if(name.startsWith("/")) {
				name = name.substring(1);
			}
			if(name.endsWith("/")) {
				name = name.substring(0, name.length()-1);
			}
			if(name.length() == 0) {
				name = null;
			}
		}
		return name;
	}
}
