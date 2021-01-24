package myaction.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathPermitBean {
	/** 权限配置 **/
	private static Map<String, Map<String, Boolean>> permitPool = new ConcurrentHashMap<String, Map<String, Boolean>>();
	/** 默认允许或禁止。true 允许，false禁止 **/
	private static boolean defaultPass = true;

	public static boolean isDefaultPass() {
		return defaultPass;
	}

	/** 默认允许或禁止。true 允许，false禁止 **/
	public static void setDefaultPass(boolean defaultPass) {
		PathPermitBean.defaultPass = defaultPass;
	}
	
	public static void config(String platform, String path, boolean pass) {
		platform = getPlatform(platform);
		path = getPath(path);
		Map<String, Boolean> map = permitPool.get(platform);
		if(map == null) {
			map = new ConcurrentHashMap<String, Boolean>();
			permitPool.put(platform, map);
		}
		map.put(path, new Boolean(pass));
	}
	
	public static boolean hasPermit(String platform, String path) {
		platform = getPlatform(platform);
		path = getPath(path);
		boolean hasPlatformConfig = false;
		while(true) {
			Map<String, Boolean> map = permitPool.get(platform);
			if(map == null) {
				continue;
			}
			if(!hasPlatformConfig) {
				hasPlatformConfig = true;
			}
			int permit = _hasPermit(map, path);
			if(permit > 0) {
				return true;
			} else if(permit < 0) {
				return false;
			}
			int len = platform.length();
			if(len == 0) {
				break;
			}
			if(len < 2) {
				platform = "";
			} else {
				int pos = platform.lastIndexOf('-');
				if(pos > 0) {
					platform = platform.substring(0, pos);
				} else {
					platform = "";
				}
			}
		}
		return defaultPass;
	}
	
	private static int _hasPermit(Map<String, Boolean> map, String path) {
		while(true) {
			Boolean pass = map.get(path);
			if(pass != null) {
				if(pass.booleanValue()) {
					return 1;
				} else {
					return -1;
				}
			}
			int pos = path.lastIndexOf('/', path.length()-2);
			//搜索到根目录（/）为止 
			if(pos < 0) {
				break;
			}
			path = path.substring(0, pos);
		};
		return 0; //未找到
	}
	
	private static String getPlatform(String platform) {
		return platform.trim().toLowerCase();
	}
	
	private static String getPath(String path) {
		path = path.trim().toLowerCase();
		if(path.startsWith("/") == false) {
			path = "/" + path;
		}
		if(path.endsWith("/") == false) {
			path = path + "/";
		}
		return path;
	}
}
