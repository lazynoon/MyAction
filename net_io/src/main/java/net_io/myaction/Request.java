package net_io.myaction;

import java.io.ByteArrayInputStream;
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
	/** 默认最大解析的 POST 内容长度 **/
	protected static final int DEFAULT_MAX_POST_LENGTH = 2 * 1024 * 1024;
	protected long startMsTime = System.currentTimeMillis(); //开始时间的默认值
	protected long startNsTime = System.nanoTime(); //开始运行的纳秒时间
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
	/** 最大POST长度 **/
	protected int maxPostLength = DEFAULT_MAX_POST_LENGTH;
	/** 自动解析POST内容 **/
	protected boolean autoParsePost = false;
	
	public Request() {
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

	/** 是否为 HTTP 请求（由子Http请求子类覆盖方法） **/
	public boolean isHttpRequest() {
		return false;
	}

	/**
	 * 获取输入流
	 * @return 不含null的InputStream
	 */
	public InputStream getInputStream() {
		if(in == null) {
			in = new ByteArrayInputStream(new byte[0]);
		}
		return in;
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
	
	public void parseBody() throws IOException {
		if(in == null) {
			return;
		}
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
				if(size >= maxPostLength) {
					throw new IOException("[PostLengthException] over post max length: "+maxPostLength);
				}
				if(offset >= capacity) {
					buff.flip();
					buff = ByteBufferPool.mallocLarge();
					buffs.add(buff);
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

	/**
	 * 检查是否存在请求参数
	 * @param name 参数名称
	 * @return true OR false
	 */
	public boolean hasParameter(String name) {
		return params.containsKey(name);
	}

	/**
	 * 获取请求参数
	 * @param name 参数名称
	 * @return 参数值（不存在，返回空字符串，不返回null）
	 */
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
		return this.startMsTime;
	}

	/**
	 * 取得请求时间的纳秒偏移时间
	 * @return long
	 */
	public long getStartNanoTime() {
		return this.startNsTime;
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
		return Session.newInstance();
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

	public int getMaxPostLength() {
		return maxPostLength;
	}

	public void setMaxPostLength(int maxPostLength) {
		this.maxPostLength = maxPostLength;
	}

	public boolean isAutoParsePost() {
		return autoParsePost;
	}

	public void setAutoParsePost(boolean autoParsePost) {
		this.autoParsePost = autoParsePost;
	}
}
