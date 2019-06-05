package myaction.extend;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import myaction.model.RunTimer;
import myaction.plog.LogService;
import net_io.myaction.MyActionServer;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.tool.DSN;
import net_io.utils.EncodeUtils;
import net_io.utils.NetLog;
import net_io.utils.thread.MyThreadPool;

public class AppConfig {
	public static final MyActionServer ACTION_SERVER = new MyActionServer();
	
	private static MyActionSocket defaultActionSocket = null;
	
	//中间件版本号。每次发版本前更新
	public static final String VERSION = "5.0.0";

	private static String RunEnv = "DEV";

	//异步日志服务
	public static final LogService logService = new LogService();

	public static final RunTimer timer = new RunTimer(true);
	private static final String RUNTIME_ID = EncodeUtils.createTimeRandId();

	private static ArrayList<ActionPackage> actionPackages = new ArrayList<ActionPackage>();

	// 程序的激活状态
	private static int LOG_LEVEL = 2;

	private static String logDir = "";
	
	private static HashMap<String, Boolean> agentMap = new HashMap<String, Boolean>(); //客户端代理IP地址

	// 控制台监听端口号
	private static int httpServerPort = 0;
	private static int httpsServerPort = 0;
	private static int socketServerPort = 0;
	private static int sslSocketServerPort = 0;
	// 默认连接超时时间（单位：ms）
	private static long defaultConnectTimeout = 30 * 1000;

	public static volatile long requestCount = 0;
	public static volatile long processCount = 0;
	public static int concurrent = 0;
	
	private static LinkedHashMap<String, String> configMap = new LinkedHashMap<String, String>();
	
	/** 连接配置 **/
	private static LinkedHashMap<String, DSN> connectDSN = new LinkedHashMap<String, DSN>();
	
	//中间件KEY
	private static HashMap<String, String> middlewareKeyMap = new HashMap<String, String>();

	// 启动时间
	private static Date startupTime = new Date();

	public static List<ActionPackage> getActionPackages() {
		return actionPackages;
	}

	public static Date getStartTime() { return startupTime; }
	public static int getLogLevel() { return LOG_LEVEL; }
	
	public static int getSocketServerPort() {
		return socketServerPort;
	}
	public static int getSslSocketServerPort() {
		return sslSocketServerPort;
	}
	public static int getHttpServerPort() {
		return httpServerPort;
	}
	public static int getHttpsServerPort() {
		return httpsServerPort;
	}
	public static String getHttpsCertFile() {
		return getProperty("myaction.server.https.cert.file");
	}
	public static String getHttpsCertPassword() {
		return getProperty("myaction.server.https.cert.password");
	}
	public static long getConnectTimeout() {
		return defaultConnectTimeout;
	}
	public static DSN getConnectDSN(String name) {
		return connectDSN.get(name.toLowerCase());
	}
	public static String getRuntimeID() {
		return RUNTIME_ID;
	}
	public static MyActionSocket getDefaultActionSocket() {
		if(defaultActionSocket == null) {
			synchronized(AppConfig.class) {
				if(defaultActionSocket == null) {
					defaultActionSocket = new MyActionSocket(new MyThreadPool(8, 64));
				}
			}
		}
		return defaultActionSocket;
	}
	
	
	/**
	 * 获取平台的密钥
	 * @param platform 平台代码
	 * @return 存在返回密钥，不存在返回null
	 */
	public static String getAppKey(String appID) {
		if(appID == null || appID.length() == 0) {
			throw new IllegalArgumentException("Parameter Error! (appID is empty)");
		}
		String appKey = middlewareKeyMap.get(appID);		
		if(appKey == null) {
			//检查主账户的KEY
			int pos = appID.indexOf('-');
			if(pos > 0) {
				appKey = middlewareKeyMap.get(appID.substring(0, pos));
			}
		}
		return appKey;
	}
	
	/**
	 * 获取全部配置的名称
	 */
	public static List<String> getPropertyNames() {
		ArrayList<String> list = new ArrayList<String>();
		for(String name : configMap.keySet()) {
			list.add(name);
		}
		return list;
	}

	/**
	 * 获取项目自定义的配置
	 * 
	 * @param name
	 * @return
	 */
	public static String getProperty(String name) {
		if(name == null) {
			return null;
		}
		name = name.trim().toLowerCase();
		return configMap.get(name);
	}
	
	public static String getRunEnv() {
		return RunEnv;
	}

	/**
	 * 检查是否为可信任的代理服务器IP
	 * 
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
		/*
		 * 读取配置文件config.properties, Class.getResourceAsStream(String path),
		 * path 不以’/'开头时默认是从此类所在的包下取资源，以’/'开头则是从
		 * ClassPath根下获取。其只是通过path构造一个绝对路径，最终还是由ClassLoader获取资源
		 */
		InputStream in = AppConfig.class.getResourceAsStream("/config.properties");
		loadProperites(in);
	}
	
	public static void loadProperites(InputStream in) throws Exception {
		Properties prop = new Properties();
		prop.load(in);
		Enumeration<Object> names = prop.keys();
		while(names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String value = prop.getProperty(name);
			if(value == null) {
				continue;
			}
			value = new String(value.getBytes("ISO8859-1"), "UTF-8");
			value = value.trim();
			if("webservice.concurrent".equals(name)) {
				// WEB服务器端口号
				concurrent = Integer.parseInt(value);
			} else if("webservice.logdir".equals(name)) {
				logDir = value;
				if(logDir.endsWith("/") == false) {
					logDir += "/";
				}
				// 自动创建目录
				File file = new File(logDir);
				if(file.exists() == false) {
					file.mkdir();
				}
			} else if(name.startsWith("middleware.key.") && value.length() >= 8) { // 中间件的密钥
				name = name.substring("middleware.key.".length());
				middlewareKeyMap.put(name, value);
			} else if(name.equals("middleware.log_level") && value.length() > 0) { // 中间件的密钥
				AppConfig.LOG_LEVEL = Integer.parseInt(value);
			} else if("myaction.server.http.port".equals(name)) { // Http服务器端口号
				AppConfig.httpServerPort = Integer.parseInt(value);
			} else if("myaction.server.https.port".equals(name)) { // Https服务器端口号
				AppConfig.httpsServerPort = Integer.parseInt(value);
			} else if("myaction.server.socket.port".equals(name)) { // Socket服务器端口号
				AppConfig.socketServerPort = Integer.parseInt(value);
			} else if("myaction.server.sslsocket.port".equals(name)) { // Socket服务器端口号
				AppConfig.sslSocketServerPort = Integer.parseInt(value);
			} else if(name.startsWith("myaction.connect.")) {
				name = name.substring("myaction.connect.".length()).toLowerCase();
				connectDSN.put(name, DSN.parse(value));
			} else if("run_env".equals(name)) { // Socket服务器端口号
				RunEnv = value;
			} else if(name.startsWith("webservice.action.")) {
				String key = name.substring("webservice.action.".length());
				int pos = key.indexOf('.');
				if(pos <= 0) {
					NetLog.logError("MyAction class configure name error: " + name);
					continue;
				}
				String moduleName = key.substring(0, pos).toLowerCase();
				// 每一个模块对应一个pkgInfo
				ActionPackage pkgInfo = null;
				// 如果pkgInfo已经设置了部分属性, 则从actionPackages找出该pkgInfo
				for(ActionPackage info : actionPackages) {
					if(moduleName.equals(info.getModuleName())) {
						pkgInfo = info;
						break;
					}
				}
				// 如果这个模块属性还没有设置过, 则新建ActionPackage对象, 设置模块名, 并存入到actionPackages中
				if(pkgInfo == null) {
					pkgInfo = new ActionPackage(moduleName);
					actionPackages.add(pkgInfo);
				}
				// 经过上面两步这个pkgInfo对应当前的moduleName(同模块的.path和.package必须使用同一个pkgInfo)
				if(key.endsWith(".path")) {
					// 前置路径
					pkgInfo.setPrefixPath(value);
				} else if(key.endsWith(".package")) {
					// 包名
					pkgInfo.setPkgName(value);
					// 设置完成后, 通过getActionPackages()可以拿到所有设置好的actionPackages
				} else {
					NetLog.logWarn("Unknown action package configure parameter: " + name);
				}
			} else {
				if(value.length() > 0) {
					NetLog.logDebug("dynamic config in the config.properties: " + name);
					name = name.toLowerCase();
					configMap.put(name, value);
				} else {
					NetLog.logInfo("ignore empty config in the config.properties: " + name);
				}
			}
		}
		in.close();
	}

	public static String getLogDir() {
		return logDir;
	}


}
