package net_io.myaction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net_io.core.ByteArray;
import net_io.myaction.server.CommandMsg;
import net_io.utils.Mixed;

import com.sun.net.httpserver.HttpExchange;

public class Response {
	public static final int MSG_ID = 0x8302;
	public static enum MODE{DEFAULT, JSON, TEXT};
	private Mixed data = null;
	private CommandMsg msg = null;
	//是否禁止响应
	private boolean disabled = false;
	private StringBuffer body = new StringBuffer();
	private int error = 0;
	private String reason = "";
	private int httpCode = 200;
	private MODE mode = MODE.DEFAULT;
	private HttpExchange httpExchange;
	private String charset = "UTF-8";
	protected int requestID = 0;
	protected String path = null;
	
	public Response(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
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
	public static Response parse(CommandMsg msg) {
		Response response = new Response();
		response.path = msg.getPath();
		response.requestID = msg.getRequestID();
		response.data = msg.getData();
		response.error = response.data.getInt("error");
		response.reason = response.data.getString("reason");
		return response;
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
	
	public Response() {
		msg = new CommandMsg(MSG_ID);
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
	
	public static Response clone(Request request) {
		Response response = new Response();
		response.msg.setPath(request.getPath());
		response.msg.setRequestID(request.getRequestID());
		return response;
	}
	
	public int getRequestID() {
		return msg == null ? 0 : msg.getRequestID();
	}
	
	public String getPath() {
		return msg == null ? null : msg.getPath();
	}
	
	public String toString() {
		return getPath() + " DATA " + data;
	}
	
	protected void writeSendBuff(ByteArray sendBuff) throws IOException {
		if(msg == null) {
			throw new IOException("[MyAction] The response is not open with SOCKET mode.");
		}
		Mixed result = new Mixed();
		result.set("error", error);
		result.set("reason", reason);
		if(data != null) {
			result.set("data", data);
		}
		if(body.length() > 0) {
			result.set("body", body.toString());
		}
		msg.resetData(result);
		msg.writeData(sendBuff);
		msg.finishWrite(sendBuff);
	}
}
