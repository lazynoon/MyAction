package net_io.myaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;

import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.server.QueryStringParser;
import net_io.utils.Mixed;

import com.sun.net.httpserver.HttpExchange;

public class Request {
	public static final int MSG_ID = 0x8301;
	protected NetChannel channel = null;
	protected long startTime = 0;
	private HashMap<Object, Object> attributes = new HashMap<Object, Object>(); 

	private String path = null;
	private InetSocketAddress remoteAddress = null;
	private String action = null;
	private InputStream in = null;
	private String clientIP = null;
	private String remoteIP = null;
	private Mixed params = new Mixed();
	private int requestID = 0;
	
	
	public Request() {
	}

	public CommandMsg generateCommandMsg() {
		CommandMsg msg = new CommandMsg(MSG_ID);
		msg.setPath(path);
		msg.setRequestID(requestID);
		msg.resetData(params);
		return msg;
	}
	
	public int getRequestID() {
		return requestID;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
	
	public static Request parse(CommandMsg msg) {
		Request request = new Request();
		request.path = msg.getPath();
		request.requestID = msg.getRequestID();
		request.params = msg.getData();
		return request;
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
		request.path = uri.getPath();
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
			String body = (new String(arr, 0, buff.limit(), "UTF-8")).trim();
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

	public Object getAttribute(Object key) {
		return attributes.get(key);
	}

	public Object setAttribute(Object key, Object value) {
		return attributes.put(key, value);
	}
	
	public Session getSession() {
		return new Session();
	}
	
	public NetChannel getChannel() {
		return channel;
	}
	public void setChannel(NetChannel channel) {
		this.channel = channel;
	}
}
