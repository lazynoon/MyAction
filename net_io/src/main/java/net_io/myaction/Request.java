package net_io.myaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import net_io.core.ByteBufferPool;
import net_io.myaction.server.QueryStringParser;
import net_io.utils.Mixed;

import com.sun.net.httpserver.HttpExchange;

public class Request {
	protected long startTime = System.currentTimeMillis(); //开始时间的默认值
	private Map<String, Object> attributes = new LinkedHashMap<String, Object>(); 

	protected String scheme = null;
	protected String path = null;
	protected String queryString = null;
	protected InetSocketAddress remoteAddress = null;
	protected String action = null;
	protected InputStream in = null;
	protected String clientIP = null;
	protected String remoteIP = null;
	protected Mixed params = new Mixed();
	protected HttpHeaders headers = null;
	
	
	public Request() {
	}

	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
	
	/**
	 * 获取HttpHeaders对象
	 * @return 返回对象总是存在（!=null）
	 */
	public HttpHeaders getHttpHeaders() {
		if(headers == null) {
			headers = new HttpHeaders();
		}
		return headers;
	}
	
	public static Request parse(HttpExchange httpExchange) {
		InputStream in = httpExchange.getRequestBody();
		URI uri = httpExchange.getRequestURI();
		Request request = new Request();
		request.in = in;
		request.remoteAddress = httpExchange.getRemoteAddress();
		//取得连接端IP地址
		request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		//HTTP头部对象
		request.headers = HttpHeaders.newInstance(httpExchange.getRequestHeaders().entrySet());
		//检查代理IP
		request.clientIP = getClientIP(request.remoteIP, request.headers);
		request.scheme = uri.getScheme();
		request.path = uri.getPath();
		request.queryString = uri.getRawQuery();
		
		//parameter
		try {
			QueryStringParser.parse(request.params, request.queryString);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return request;
	}
	
	public void parseBody(int maxLen) throws IOException {
		ArrayList<ByteBuffer> buffs = new ArrayList<ByteBuffer>();
		try {
			ByteBuffer buff = ByteBufferPool.mallocLarge();
			buffs.add(buff); //先加入 buffer list ,以便在 finally 中释放
			int offset = 0;
			int capacity = buff.capacity();
			int size = 0;
			while(true) {
				int b = in.read();
				if(b < 0) {
					break;
				}
				if(size >= maxLen) {
					throw new IOException("[PostLengthException] over post max length: "+maxLen);
				}
				if(offset >= capacity) {
					buff.flip();
					buffs.add(buff);
					buff = ByteBufferPool.mallocLarge();
					offset = 0;
					capacity = buff.capacity();
				}
				offset++;
				size++;
				buff.put((byte)b);
			}
			buff.flip();
			//从ByteBuffer转换为String
			StringBuilder build = new StringBuilder();
			for(ByteBuffer item : buffs) {
				if(item.limit() == 0) {
					continue;
				}
				byte[] arr = item.array();
				build.append(new String(arr, 0, item.limit(), ActionFactory.getDefaultCharset()));
			}
			String body = build.toString();
			//第一时间释放内存
			ByteBufferPool.free(buffs);
			buffs = null;
			//parameter
			try {
				QueryStringParser.parse(this.params, body);
			} catch (UnsupportedEncodingException e) {
				throw new IOException("Occurred the UnsupportedEncodingException: "+e.getMessage());
			}
		} finally {
			//内存释放
			if(buffs != null) {
				ByteBufferPool.free(buffs); 
			}
		}
	}
	
	public String getParameter(String name) {
		return params.getString(name);
	}
	
	public void setParameter(String name, String value) {
		params.set(name, value);
	}
	
	public String[] getParameterNames() {
		return params.keys();
	}
	
	public String getAction() {
		return action;
	}
	
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}
	
	/**
	 * 取得请求开始时间（微秒）
	 * @return long
	 */
	public long getStartTime() {
		return this.startTime;
	}
	
	/**
	 * TCP连接方IP
	 */
	public String getRemoteIP() {
		return remoteIP;
	}
	
	/**
	 * 客户端真实IP
	 */
	public String getClientIP() {
		return clientIP;
	}
	
	public String[] getAttributeNames() {
		String[] keys = new String[attributes.size()];
		int offset = 0;
		for(String key : attributes.keySet()) {
			keys[offset++] = key;
		}
		return keys;
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}
	
	public Session getSession() {
		return new Session();
	}
	
	/**
	 * Returns the query string that is contained in the request URL after the path
	 */
	public String getQueryString() {
		if(queryString == null) {
			return "";
		}
		return queryString;
	}

	/**
	 * a String containing the name of the scheme used to make this request
	 */
	public String getScheme() {
		if(scheme == null) {
			return "";
		}
		return scheme;
	}

	protected static String getClientIP(String remoteIP, HttpHeaders headers) {
		if(headers == null) {
			return remoteIP;
		}
		String httpClientIP = null;
		if(remoteIP.equals("127.0.0.1")
				|| remoteIP.startsWith("192.168.")
				|| (remoteIP.compareTo("172.16.") >= 0 && remoteIP.compareTo("172.32.") < 0)
				|| remoteIP.startsWith("10.")) {
			if(headers.containsKey("X-forwarded-for")) {
				httpClientIP = headers.getFirst("X-forwarded-for");
			} else if(headers.containsKey("Proxy-Client-IP")) {
				httpClientIP = headers.getFirst("Proxy-Client-IP");
			} else if(headers.containsKey("Cdn-Src-Ip")) {
				httpClientIP = headers.getFirst("Cdn-Src-Ip");
			}
			if(httpClientIP != null) {
				httpClientIP = httpClientIP.trim();
				int pos = httpClientIP.indexOf(',');
				if(pos >= 0) {
					httpClientIP = httpClientIP.substring(0, pos);
				}
				if(httpClientIP.length() == 0) {
					httpClientIP = null;
				}
			}
		}
		if(httpClientIP != null) {
			return httpClientIP;
		} else {
			return remoteIP;
		}

	}
}
