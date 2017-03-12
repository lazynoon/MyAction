package net_io.myaction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net_io.myaction.server.CommandMsg;
import net_io.utils.Mixed;

import com.sun.net.httpserver.HttpExchange;

public class Response {
	public static enum MODE{DEFAULT, JSON, TEXT};
	protected Mixed data = null;
	//是否禁止响应
	private boolean disabled = false;
	protected StringBuffer body = new StringBuffer();
	protected int error = 0;
	protected String reason = "";
	private int httpCode = 200;
	private MODE mode = MODE.DEFAULT;
	private HttpExchange httpExchange;
	private String charset = ActionFactory.getDefaultCharset();
	protected int requestID = 0;
	protected String path = null;
	/** 头部参数名列表（KEY对应的原始名称） **/
	protected Map<String, String> headerNames = new LinkedHashMap<String, String>();
	/** 头部参数值列表（KEY为小写字母） **/
	protected Map<String, String> headers = new HashMap<String, String>();
	
	public Response() {
		setHeader("Content-Type", "text/html; charset="+charset);
	}
	public Response(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
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
	
	public void addHeader(String name, String value) {
		httpExchange.getResponseHeaders().add(name, value);
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
			return;
		}
		String key = name.toLowerCase();
		headerNames.put(key, name);
		headers.put(key, value);
	}
	
	/**
	 * 获取头部参数的值
	 */
	public String getHeader(String name) {
		if(name == null || name.length() == 0) {
			return null;
		}
		String key = name.toLowerCase();
		return headers.get(key);
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
	
	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}
	public int getHttpCode() {
		return httpCode;
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

	public void assign(String str) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		if(data == null) {
			data = new Mixed();
		}
		data.add(str);
	}
	
	public void assign(String key, Object value) {
		if(mode == MODE.DEFAULT) {
			mode = MODE.TEXT;
		}
		if(data == null) {
			data = new Mixed();
		}
		data.set(key, value);
	}
	

	public String getPath() {
		return "-";
	}
	
	public String toString() {
		return getPath() + " DATA " + data;
	}
	
	/**
	 * header location跳转
	 * @param url
	 */
//	public void sendRedirect(String url) {
//		
//	}
}
