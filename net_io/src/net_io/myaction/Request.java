package net_io.myaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
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
	
	
	public Request() {
	}

	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
	
	public static Request parse(HttpExchange httpExchange) {
		InputStream in = httpExchange.getRequestBody();
		URI uri = httpExchange.getRequestURI();
		Request request = new Request();
		request.in = in;
		request.remoteAddress = httpExchange.getRemoteAddress();
		//取得连接端IP地址
		request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		//检查HTTP_CLIENT_IP
		String httpClientIP = httpExchange.getRequestHeaders().getFirst("HTTP_CLIENT_IP");
		if(httpClientIP != null) {
			request.clientIP = httpClientIP;
		} else {
			request.clientIP = request.remoteIP;
		}
		request.scheme = uri.getScheme();
		request.path = uri.getPath();
		request.queryString = uri.getQuery();
		

		//parameter
		try {
			QueryStringParser.parse(request.params, uri.getQuery());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return request;
	}
	
	public void parseBody(int maxLen) throws IOException {
		ByteBuffer buff = ByteBufferPool.malloc(maxLen);
		try {
			for(int i=0; i<=maxLen; i++) {
				int b = in.read();
				if(b < 0) {
					break;
				}
				buff.put((byte)b);
			}
			buff.flip();
			if(buff.limit() > maxLen) {
				throw new IOException("[PostLengthException] buffer size: "+buff.limit()+", post length: "+maxLen);
			}
			byte[] arr = buff.array();
			String body = (new String(arr, 0, buff.limit(), ActionFactory.getDefaultCharset())).trim();
			//parameter
			try {
				QueryStringParser.parse(this.params, body);
			} catch (UnsupportedEncodingException e) {
				throw new IOException("Occurred the UnsupportedEncodingException: "+e.getMessage());
			}
		} finally {
			ByteBufferPool.free(buff);
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
	
}
