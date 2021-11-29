package net_io.myaction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net_io.utils.FindClassUtils;
import net_io.utils.NetLog;

public class ActionFactory {
	private static String defaultCharset = "UTF-8"; //默认编码
	private static Map<String, ActionClassMethod> actions = new LinkedHashMap<String, ActionClassMethod>(1024);
	private static List<RewriteRule> rewriteRules = new ArrayList<RewriteRule>();
	
	/**
	 * 设置MyAction默认的字符编码
	 * @param charset 默认为UTF-8
	 */
	public static void setDefaultCharset(String charset) {
		defaultCharset = charset;
	}
	
	/**
	 * 获取MyAction默认的字符编码
	 * @return CharacterEncoding 如 UTF-8
	 */
	public static String getDefaultCharset() {
		return defaultCharset;
	}
	
	/** 添加一条重写规则 **/
	public static void addRewriteRule(String regex, String dstPath) {
		if(regex == null || regex.length() == 0) {
			throw new RuntimeException("The parameter of 'path' is empty.");
		}
		if(dstPath.startsWith("/") == false) {
			dstPath = "/" + dstPath;
		}
		rewriteRules.add(new RewriteRule(regex, dstPath));
	}
	
	/**
	 * 检查重写规则，获取重写后路径（URI）
	 * @return 找不到，返回 null
	 */
	public static String searchRewritePath(String path) {
		for(RewriteRule info : rewriteRules) {
			if(info.isMatch(path)) {
				return info.dstPath;
			}
		}
		return null;
	}
	

	/**
	 * 注册一个Action类
	 * @param path
	 * @param clazz
	 */
	public static void register(String path, Class<?> clazz) {
		if(path == null || path.length() == 0) {
			throw new RuntimeException("The parameter of 'path' is empty.");
		}
		if(path.startsWith("/") == false) {
			path = "/" + path;
		}
		ActionClassMethod info = new ActionClassMethod(path, clazz);
		if(NetLog.LOG_LEVEL <= NetLog.DEBUG) {
			NetLog.logDebug("[ActionFactory] register action path: "+path+", class: "+clazz.getName());
		}
		actions.put(path.toLowerCase(), info);
	}
	
	/**
	 * 注册一个Action类的方法
	 * @param path
	 * @param clazz
	 * @param methodName
	 * @param method
	 */
	public static void register(String path, Class<?> clazz, String methodName, Method method) {
		if(path == null || path.length() == 0) {
			throw new RuntimeException("The parameter of 'path' is empty.");
		}
		if(methodName == null || methodName.length() == 0) {
			throw new RuntimeException("The parameter of 'methodName' is empty.");
		}
		if(path.startsWith("/") == false) {
			path = "/" + path;
		}
		if(NetLog.LOG_LEVEL <= NetLog.INFO) {
			int pos = path.lastIndexOf("/");
			String okPath = path;
			//Action名称的首字母小写。若仅一个字母，则保留大写。
			if(pos >= 0 && pos < path.length()-2) {
				pos++;
				okPath = path.substring(0, pos) + path.substring(pos, pos+1).toLowerCase() + path.substring(pos+1);
			}
			
			String okMethodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
			NetLog.logInfo("[ActionPath] " + okPath + "." + okMethodName);
		}
		String fullPath = path + "." + methodName;
		ActionClassMethod info = new ActionClassMethod(fullPath, clazz, method);
		String key = path + "--wb--" + methodName;
		if(NetLog.LOG_LEVEL <= NetLog.DEBUG) {
			NetLog.logDebug("[ActionFactory] register key: "+key+", class: "+clazz.getName());
		}
		actions.put(key.toLowerCase(), info);
	}
	
	/**
	 * 将路径转换为执行方法
	 * @param path
	 * @return ActionClassMethod
	 */
	public static ActionClassMethod get(String path) {
		String method = "index"; //默认访问 doIndex() 方法
		if(path == null || path.length() == 0) {
			throw new RuntimeException("The parameter of 'path' is empty.");
		}
		path = path.toLowerCase();
		if(path.endsWith("/")) {
			path += "home"; //如果是目录，则默认为“HomeAction”
		} else { //格式：/packageName1/packageName2/className.methodName
			int pos = path.lastIndexOf('/');
			if(pos >= 0) {
				int pos2 = path.indexOf(".", pos);
				if(pos2 > 0) {
					method = path.substring(pos2+1);
					path = path.substring(0, pos2);
				}
			}
		}
		String key = path + "--wb--" + method;
		ActionClassMethod info = actions.get(key); //方法名直接匹配优先
		if(info == null) {
			info = actions.get(path); //取Action类配置
			if(info != null) {
				info = info.newInstance(method); //根据方法名，重建对象。
			}
		}
		if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL && info != null) {
			NetLog.logDebug("[ActionFactory] action key: "+key+", class: "+info.clazz.getName());
		}

		return info;
	}
	
	public static List<String> fetchAllPath() {
		List<String> list = new ArrayList<String>();
		for(String path : actions.keySet()) {
			ActionClassMethod obj = actions.get(path);
			list.add(obj.getPath());
		}
		return list;
	}
	
	
	/**
	 * 按包名，注册所有 action 类
	 * @param pkgName
	 */
	public static void registerByPackage(String pkgName) {
		registerByPackage(pkgName, null);
	}
	/**
	 * 按包名，注册所有 action 类
	 * @param pkgName
	 * @param prefixPath 前缀路径. 默认为 '/' 
	 */
	public static void registerByPackage(String pkgName, String prefixPath) {
		if(NetLog.LOG_LEVEL <= NetLog.INFO) {
			NetLog.logInfo("registerByPackage: "+pkgName);
		}
		if(prefixPath == null) {
			prefixPath = "/";
		} else if(prefixPath.endsWith("/") == false) {
			prefixPath = prefixPath + "/";
		}
		if(pkgName.endsWith(".")) {
			pkgName = pkgName.substring(0, pkgName.length()-1);
		}
		int pkgLen = pkgName.length();
		for(String className : FindClassUtils.getClassInPackage(pkgName)) {
			//检索访问路径: path
			int classLen = className.length();
			if(classLen < pkgLen + 7) {
				continue; //类名的长度检查不通过
			}
			String postfix = className.substring(className.length()-6);
			if(postfix.equalsIgnoreCase("Action") == false) {
				continue; //Action类必须以 Action 结尾
			}
			String path = className.substring(pkgLen+1, classLen-6);
			path = path.replace('.', '/');
			path = prefixPath + path;
			if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL) {
				NetLog.logDebug("[ActionFactory] find path: "+path);
			}
			//检索访问方法: method
			try {
				Class<?> clazz = Class.forName(className);
				if(NetLog.LOG_LEVEL <= NetLog.DEBUG) {
					NetLog.logDebug("[ActionFactory] find class: "+clazz.getName());
				}
//				if(clazz.isAssignableFrom(BaseMyAction.class) == false) {
//					continue; //非 BaseMyAction 类
//				}
				for(Method method : clazz.getMethods()) {
					String methodName = method.getName();
					if("do".equalsIgnoreCase(methodName.substring(0, 2)) == false) {
						continue; //方法名，必须以 "do" 开头
					}
					if(method.isVarArgs()) {
						continue; //必须是无参方法。如：doXXX()
					}
					register(path, clazz, methodName.substring(2), method);
				}
			} catch (ClassNotFoundException e) {
				NetLog.logError(e);
			}
		}
		
	}

	public static class ActionClassMethod {
		private Class<BaseMyAction> clazz;
		private Method methodObject = null;
		private String methodName = null;
		private String path = null;
		protected ActionClassMethod(String path, Class<?> clazz) {
			this.path = path;
			this.clazz = convertClass(clazz);
		}		
		protected ActionClassMethod(String path, Class<?> clazz, Method methodObject) {
			this.path = path;
			this.clazz = convertClass(clazz);
			this.methodObject = methodObject;
		}
		private ActionClassMethod(Class<?> clazz, String methodName) {
			this.clazz = convertClass(clazz);
			this.methodName = methodName;
		}
		public BaseMyAction createInstance() throws InstantiationException, IllegalAccessException {
			return clazz.newInstance();
		}
		
		public ActionClassMethod newInstance(String methodName) {
			return new ActionClassMethod(clazz, methodName);
		}
		
		/**
		 * 执行业务层Action方法
		 * @param actionObj
		 * @throws Exception
		 */
		public void executeUserMethod(BaseMyAction actionObj) throws Exception {
			if(methodObject != null) {
				methodObject.invoke(actionObj);
			} else {
				actionObj.defaultExecute(methodName);
			}
		}
		
		@SuppressWarnings("unchecked")
		private Class<BaseMyAction> convertClass(Class<?>clazz) {
//			if(clazz.isAssignableFrom(BaseMyAction.class) == false) {
//				throw new IllegalArgumentException("Action Class Must instanceof BaseMyAction");
//			}
			return (Class<BaseMyAction>) clazz;
		}

		public String getPath() {
			return path;
		}
		
	}
	
	private static class RewriteRule {
		private String regex;
		private String dstPath;
		private Pattern pattern;
		RewriteRule(String regex, String dstPath) {
			this.regex = regex;
			this.dstPath = dstPath;
			this.pattern = Pattern.compile(this.regex, Pattern.DOTALL);
		}
		boolean isMatch(String path) {
			Matcher m = this.pattern.matcher(path);
			return m.matches();			
		}
	}
}
