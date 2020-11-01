/**
 * 数据源定义
 * @author Hansen
 */
package net_io.myaction.tool;

import java.util.HashMap;

public class DSN {
	/** DNS配置 **/
	private String url = null;
	/** 数据源协议 **/
	private String scheme = null;
	/** 访问用户 **/
	private String user = null;
	/** 访问密钥 **/
	private String pass = null;
	/** 主机地址 **/
	private String host = null;
	/** 端口号 **/
	private int port = 0;
	/** 路径 **/
	private String path = null;
	/** 附加参数 **/
	private HashMap<String, String> params = new HashMap<String, String>();
	
	private DSN() {}
	
	public DSN(String scheme, String host, int port, String path, String user, String pass) {
		this.scheme = scheme;
		this.user = user;
		this.pass = pass;
		this.host = host;
		this.port = port;
		this.path = path;
		StringBuilder build = new StringBuilder();
		build.append(scheme);
		build.append("://");
		if(user != null && user.length() > 0) {
			build.append(user);
			if(pass != null && pass.length() > 0) {
				build.append(":");
				build.append(pass);
			}
			build.append("@");
		}
		build.append(host);
		build.append(":");
		build.append(port);
		build.append(path);
		this.url = build.toString();
	}
	

	@Override
	public int hashCode() {
		return url.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof DSN) {
			return url.equals(((DSN)obj).url);
		}
		return false;
	}
	
	/** 数据源唯一名称 **/
	public String getScheme() {
		return scheme;
	}

	/** 访问用户 **/
	public String getUser() {
		return user;
	}
	
	/** 访问密钥 **/
	public String getPasswd() {
		return pass;
	}

	/** 主机地址 **/
	public String getHost() {
		return host;
	}

	/** 端口号 **/
	public int getPort() {
		return port;
	}

	/** 路径 **/
	public String getPath() {
		return path;
	}
	
	/**
	 * 获取参数的值
	 * @param name 参数名
	 * @return String，不存在，返回null
	 */
	public String getParameter(String name) {
		return params.get(name);
	}
	
	/**
	 * 解析DSN通用定义
	 * @param url DSN的配置
	 * @return 总是返回DNS对象。若有错误，抛出RuntimeException
	 */
	public static DSN parse(String url) {
		url = url.trim();
		DSN dsn = new DSN();
		dsn.url = url;
		//Parse DSN Value
		int pos1 = url.indexOf(':');
		if(pos1 < 0 || url.charAt(pos1+1) != '/' || url.charAt(pos1+2) != '/') {
			throw new RuntimeException("DSN format error.");
		}
		dsn.scheme = url.substring(0, pos1).toLowerCase();
		int pos2 = url.indexOf('/', pos1+3);
		if(pos2 < 0) {
			throw new RuntimeException("DSN format error.");
		}
		//example: user:password@host:port
		String midStr = url.substring(pos1+3, pos2);
		int pos3 = url.indexOf('?', pos2);
		if(pos3 > 0) {
			dsn.path = url.substring(pos2, pos3);
			String paramStr = url.substring(pos3+1);
			for(String str : paramStr.split("&")) {
				str = str.trim();
				String[] arr = str.split("=", 2);
				if(arr.length < 2 || arr[1].length() == 0) {
					continue; //忽略空值参数
				}
				arr[0] = arr[0].toLowerCase(); //参数名采用小写
				dsn.params.put(arr[0], arr[1]);
			}
		} else {
			dsn.path = url.substring(pos2);
		}
		//parse midStr
		pos1 = midStr.indexOf(':');
		pos2 = midStr.indexOf('@');
		if(pos2 < 0) { //no authority area
			if(pos1 < 0) { //no port area
				dsn.host = midStr;
			} else {
				dsn.host = midStr.substring(0, pos1);
				dsn.port = Integer.parseInt(midStr.substring(pos1+1));
			}
		} else { //has authority area
			if(pos1 >= 0) {
				if(pos1 < pos2) { //in authority area
					dsn.user = midStr.substring(0, pos1);
					dsn.pass = midStr.substring(pos1+1, pos2);
					pos3 = midStr.indexOf(':', pos2+1);
					if(pos3 > 0) {
						dsn.host = midStr.substring(pos2+1, pos3);
						dsn.port = Integer.parseInt(midStr.substring(pos3+1));
					} else { //no port
						dsn.host = midStr.substring(pos2+1);
					}
				} else { //only user name. note: pos1 after pos2
					dsn.user = midStr.substring(0, pos2);
					dsn.host = midStr.substring(pos2+1, pos1);
					dsn.port = Integer.parseInt(midStr.substring(pos1+1));
				}
			} else { //no password, no port
				dsn.user = midStr.substring(0, pos2);
				dsn.host = midStr.substring(pos2+1, pos1);
			}
		}
		//默认端口处理
		if(dsn.port == 0) {
			if("http".equals(dsn.scheme)) {
				dsn.port = 80;
			} else if("https".equals(dsn.scheme)) {
				dsn.port = 443;
			}
		}
		return dsn;
	}

}
