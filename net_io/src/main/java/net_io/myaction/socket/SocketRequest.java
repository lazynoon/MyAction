package net_io.myaction.socket;

import net_io.core.NetChannel;
import net_io.myaction.Request;
import net_io.myaction.server.CommandMsg;
import net_io.utils.JSONException;
import net_io.utils.JSONUtils;
import net_io.utils.Mixed;

public class SocketRequest extends Request {
	public static final int MSG_ID = 0x8301;
	protected NetChannel channel = null;
	private int requestID = 0;
	private int version = 0;

	public NetChannel getChannel() {
		return channel;
	}
	
	protected void setChannel(NetChannel channel) {
		this.channel = channel;
	}
	
	public static SocketRequest parse(CommandMsg msg) {
		return parse(msg, null);
	}
	public static SocketRequest parse(CommandMsg msg, NetChannel channel) {
		SocketRequest request = new SocketRequest();
		request.channel = channel;
		request.path = msg.getPath();
		request.requestID = msg.getRequestID();
		request.params = msg.getData();
		if(channel != null) {
			request.remoteAddress = channel.getRemoteAddress();
		}
		//取得连接端IP地址
		if(request.remoteAddress != null) {
			request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		}
		//检查HTTP_CLIENT_IP
//		String httpClientIP = request.params.getString("HTTP_CLIENT_IP");
//		if(httpClientIP != null) {
//			request.clientIP = httpClientIP;
//		} else {
//			request.clientIP = request.remoteIP;
//		}
		request.clientIP = request.remoteIP;
		return request;
	}
	/**
	 * WebSocket请求参数解析
	 * @param data 格式：用 \t 分割
	 * 		版本号（16进制数），请求ID（16进制），路径（字符串），JSON格式请求参数
	 * @param header
	 * 
	 * @return
	 * @throws JSONException 
	 */
	public static SocketRequest parseWebSocket(String data, NetChannel channel) throws JSONException {
		SocketRequest request = new SocketRequest();
		request.channel = channel;
		
		//第1个字段：版本号
		int pos1 = data.indexOf('\t');
		if(pos1 > 0 && pos1 < data.length() - 1) {
			request.version = Integer.parseInt(data.substring(0, pos1), 16);
		} else {
			throw new RuntimeException("formet error.");
		}
		//第2个字段：请求ID
		int pos2 = data.indexOf('\t', pos1+1);
		if(pos2 > 0 && pos2 < data.length() - 1) {
			request.requestID = Integer.parseInt(data.substring(pos1+1, pos2), 16);
		} else {
			throw new RuntimeException("formet error.");
		}
		//第3个字段：访问路径
		int pos3 = data.indexOf('\n', pos2+1);
		if(pos3 > 0) {
			request.path = data.substring(pos2+1, pos3);
		} else {
			throw new RuntimeException("formet error.");
		}
		//正文：JSON格式的参数
		 if(pos3 < data.length() - 1) {
			 request.params = JSONUtils.parseJSON(data.substring(pos3+1));
		 } else {
			 request.params = new Mixed();
		 }
		
		

		if(channel != null) {
			request.remoteAddress = channel.getRemoteAddress();
		}
		//取得连接端IP地址
		if(request.remoteAddress != null) {
			request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		}
		//检查HTTP_CLIENT_IP
//		String httpClientIP = request.params.getString("HTTP_CLIENT_IP");
//		if(httpClientIP != null) {
//			request.clientIP = httpClientIP;
//		} else {
//			request.clientIP = request.remoteIP;
//		}
		request.clientIP = request.remoteIP;
		return request;
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
	
	public int getVersion() {
		return version;
	}

}
