package net_io.myaction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net_io.myaction.server.CommandMsg;
import net_io.utils.Mixed;

public class Response {
	public static enum MODE{DEFAULT, JSON, TEXT, BINARY};
	protected Mixed data = null;
	//是否禁止响应
	private boolean disabled = false;
	protected StringBuffer body = new StringBuffer();
	protected byte[] attachment = null;
	protected int error = 0;
	protected String reason = "";
	private int httpCode = 200;
	protected MODE mode = MODE.DEFAULT;
	private String charset = ActionFactory.getDefaultCharset();
	protected int requestID = 0;
	protected String path = null;
	/** 头部参数名列表（KEY对应的原始名称） **/
	protected Map<String, String> headerNames = new LinkedHashMap<String, String>();
	/** 头部参数值列表（KEY为小写字母） **/
	protected Map<String, List<String>> headers = new HashMap<String, List<String>>();
	
	public Response() {
		setHeader("Content-Type", "text/html; charset="+charset);
	}

	public void print(String str) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		body.append(str);
	}
	
	public void println(String str) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		body.append(str);
		body.append("\r\n");
	}
	
	public void setMode(MODE mode) {
		this.mode = mode;
	}
	
	public MODE getMode() {
		return mode;
	}
	
	/**
	 * 设置字符集(默认UTF-8)
	 * @param charset
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	/**
	 * 获取网页字符集(默认UTF-8)
	 */
	public String getCharset() {
		return this.charset;
	}
	
	public Mixed getData() {
		return data;
	}
	
	public Mixed getData(boolean getInnerData) {
		if(data == null) {
			return null;
		}
		return data.get("data");
	}
	public static Response parse(CommandMsg msg) {
		Response response = new Response();
		response.path = msg.getPath();
		response.requestID = msg.getRequestID();
		response.data = msg.getData();
		if(response.data != null) {
			response.error = response.data.getInt("error");
			response.reason = response.data.getString("reason");
		}
		return response;
	}
	
	/**
	 * 发送头部参数
	 */
	public void setHeader(String name, String value) {
		if(name == null || name.length() == 0) {
			if(value != null && value.length() > 0) {
				throw new IllegalArgumentException("Header name can not be empty");
			}
			return;
		}
		String key = name.toLowerCase();
		headerNames.put(key, name);
		ArrayList<String> list = new ArrayList<String>();
		list.add(value);
		headers.put(key, list);
	}
	
	public void addHeader(String name, String value) {
		if(name == null || name.length() == 0) {
			if(value != null && value.length() > 0) {
				throw new IllegalArgumentException("Header name can not be empty");
			}
			return;
		}
		String key = name.toLowerCase();
		headerNames.put(key, name);
		List<String> list = headers.get(key);
		if(list == null) {
			list = new ArrayList<String>();
			headers.put(key, list);
		}
		list.add(value);
	}

	/**
	 * 获取头部参数的值
	 */
	public List<String> getHeaders(String name) {
		if(name == null || name.length() == 0) {
			return null;
		}
		String key = name.toLowerCase();
		return headers.get(key);
	}
	
	/**
	 * 获取头部参数的值
	 */
	public String getFirstHeader(String name) {
		List<String> list = getHeaders(name);
		if(list == null || list.size() == 0) {
			return null;
		}
		return list.get(0);
	}
	
	/**
	 * 获取所有头部的名称
	 * @return 非null值
	 */
	public String[] getHeaderNames() {
		String[] names = new String[headerNames.size()];
		int offset = 0;
		for(String key : headerNames.keySet()) {
			names[offset++] = headerNames.get(key);
		}
		return names;
	}
		
	public byte[] getBodyBytes() throws UnsupportedEncodingException, IOException {
		if(mode == MODE.BINARY) {
			return null; //
		}
		String str;
		if(mode == MODE.JSON) {
			Mixed result = new Mixed();
			result.set("error", error);
			result.set("reason", reason);
			if(data != null) {
				result.set("data", data);
			}
			if(this.body.length() > 0) {
				result.set("body", this.body.toString());
			}
			str = result.toJSON();
		} else {
			str = this.body.toString();
			if(str.length() == 0 && error != 0) {
				str = error + " " + reason;
			}
		}
		return str.getBytes(charset);
	}
	
	public byte[] getAttachment() {
		return attachment;
	}
	
	@Deprecated
	public void assign(String key, Object value) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		if(data == null) {
			data = new Mixed();
		}
		data.set(key, value);
	}
	
	/**
	 * 获取错误代码
	 * @return
	 */
	public int getError() {
		return this.error;
	}
	/**
	 * 获取错误代码
	 * @return
	 */
	public String getReason() {
		return this.reason;
	}

	public void setError(int error, String reason) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		this.error = error;
		this.reason = reason;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public void resetData(Mixed data) {
		this.data = data;
		this.mode = MODE.JSON;
	}
	
	public void resetAttachment(byte[] attachment) {
		this.attachment = attachment;
	}

	public String toString() {
		return "Response MODE: " + mode;
	}
	
	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}
	
	public int getHttpCode() {
		return httpCode;
	}
	
	public void sendCookie(String name, String value) {
		sendCookie(name, value, 0, null, null);
	}
	public void sendCookie(String name, String value, long expireTime) {
		sendCookie(name, value, expireTime, null, null);
	}
	public void sendCookie(String name, String value, long expireTime, String path) {
		sendCookie(name, value, expireTime, path, null);
	}
	public void sendCookie(String name, String value, long expireTime, String path, String domain) {
		if(value == null) {
			value = "";
		}
		if(path == null) {
			path = "/";
		}
		StringBuilder build = new StringBuilder();
		build.append(name);
		build.append(':');
		build.append(value);
		build.append("; ");
		if(expireTime > 0) {
			build.append("expires=");
			build.append(new Date(expireTime).toString());
			build.append("; ");
		}
		build.append("path=");
		build.append(path);
		if(domain != null) {
			build.append("; domain=");
			build.append(domain);
		}
		addHeader("Set-Cookie", build.toString());
	}
	/**
	 * header location跳转
	 * @param url
	 */
	public void sendRedirect(String url) {
		sendRedirect(url, 302);
	}
	/**
	 * header location跳转
	 * @param url
	 */
	public void sendRedirect(String url, int httpCode) {
		this.mode = MODE.TEXT;
		this.setHttpCode(httpCode);
		this.setHeader("Location", url);
	}
}
