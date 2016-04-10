package test.action;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import net_io.utils.NetLog;

public class Config {
	private static String logDir = "";
	//控制台监听端口号
	public static int ConsolePort = 7083;
	private static HashMap<String, Boolean> agentMap = new HashMap<String, Boolean>(); //客户端代理IP地址

	public static final String APP_NAME = "WCS";
	public static final String APP_VERSION = "1.0";
	
	public static boolean inited = false;
	//控制台监听端口号
	public static int ServerPort = 8087;
	public static String COMMON_RESOURCE_URL = "";
	public static int COMMON_RESOURCE_VERSION = 0;
	public static String AVATAR_PATH_URL = "";
	private static ArrayList<ActionPackage> actionPackages = new ArrayList<ActionPackage>();
	
	public static volatile long requestCount = 0;
	public static volatile long processCount = 0;
	public static int concurrent = 0;
	

	public static InetSocketAddress getServerAddr() {
//		return new InetSocketAddress("192.168.0.241", 5002);
		return new InetSocketAddress("192.168.0.241", 8002);
	}
	
	public static List<ActionPackage> getActionPackages() {
		return actionPackages;
	}
	
	/**
	 * 检查是否为可信任的代理服务器IP
	 * @param ip
	 * @return true OR false
	 */
	public static boolean isAgentIP(String ip) {
		if(ip == null) {
			return false;
		}
		if(agentMap.containsKey(ip)) {
			return true;
		}
		int pos = ip.lastIndexOf('.');
		if(pos <= 0) {
			return false;
		}
		String ipMask = ip.substring(0, pos) + ".*";
		if(agentMap.containsKey(ipMask)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void loadProperites() throws Exception {
        Properties prop = new Properties();
        InputStream in = Config.class.getResourceAsStream("/config.properties");
        prop.load(in);
        Enumeration names = prop.keys();
        while(names.hasMoreElements()) {
        	String name = (String)names.nextElement();
        	String value = prop.getProperty(name);
        	if(value == null) {
        		continue; 
        	}
       		value = value.trim();
       		if("webservice.concurrent".equals(name)) {
       			concurrent = Integer.parseInt(value);
        	} else if("webservice.logdir".equals(name)) {
        		logDir = value;
        		if(logDir.endsWith("/") == false) {
        			logDir += "/";
        		}
        		//自动创建目录
        		File file = new File(logDir);
        		if(file.exists() == false) {
        			file.mkdir();
        		}
        	} else if(name.startsWith("webservice.action.")) {
        		String key = name.substring("webservice.action.".length());
        		int pos = key.indexOf('.');
        		if(pos <= 0) {
        			NetLog.logError("MyAction class configure name error: "+name);
        			continue;
        		}
        		String moduleName = key.substring(0, pos).toLowerCase();
        		ActionPackage pkgInfo = null;
        		for(ActionPackage info : actionPackages) {
        			if(moduleName.equals(info.getModuleName())) {
        				pkgInfo = info;
        				break;
        			}
        		}
        		if(pkgInfo == null) {
        			pkgInfo = new ActionPackage(moduleName);
        			actionPackages.add(pkgInfo);
        		}
        		//前置路径
        		if(key.endsWith(".path")) {
        			pkgInfo.setPrefixPath(value);
        		} else if(key.endsWith(".package")) {
        			pkgInfo.setPkgName(value);
        		} else {
        			NetLog.logWarn("Unknown action package configure parameter: "+name);
        		}
        	} else if("console.port".equals(name)) { //控制台端口号
        		Config.ConsolePort = Integer.parseInt(value);
        	} else if("socket_server.port".equals(name)) { //Socket服务器端口号
        		Config.ServerPort = Integer.parseInt(value);
        	} else {
        		NetLog.logWarn("Unsupport 'name' on config.properties: "+name);
        	}
        }
        in.close();
	}
	
	public static String getLogDir() {
		return logDir;
	}
	
}
