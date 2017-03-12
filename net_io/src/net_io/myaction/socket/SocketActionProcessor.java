package net_io.myaction.socket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.myaction.ActionProcessor;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.tool.HttpHeader;
import net_io.utils.NetLog;

public class SocketActionProcessor extends ActionProcessor {
	private NetChannel channel;
	private CommandMsg msg;
	private boolean websocketMode = false;
	//WebSocket模式下专有参数
	private WebSocket websocket = null;
	private String sid = null;
	private String buff = null;
	
	
	public SocketActionProcessor(NetChannel channel, CommandMsg msg) {
		this.runMode = MODE_SOCKET;
		this.channel = channel;
		this.msg = msg;
		websocketMode = false;
	}
	
	public SocketActionProcessor(WebSocket websocket, String sid, String buff) {
		websocketMode = true;
		this.websocket = websocket;
		this.sid = sid;
		this.buff = buff;
	}
	
	protected void processRequest() {
		try {
			if(websocketMode) {
				processRequestWebeSocket();
			} else {
				processRequestPrivateSocket();
			}
		} catch(Exception e) {
			NetLog.logError(e);
			if(websocket != null) {
				try {
					websocket.close(this.sid);
				} catch (IOException e1) {
					//ignore here
				}
			} else {
				channel.close();
			}
		}
	}
	protected void processRequestWebeSocket() throws Exception {
		//解析请求消息
		SocketRequest request = SocketRequest.parseWebSocket(buff, websocket.getNetChannel(this.sid));
		SocketResponse response = SocketResponse.clone(request);
		//处理Action请求
		executeAction(request, response);
		//输出结果
		if(response.isDisabled() == false) {
			byte[] bodyBytes = response.getBodyBytes();
			String headLine = Integer.toHexString(request.getVersion()) + "\t"
					+ Integer.toHexString(request.getRequestID()) + "\t"
					+ request.getPath() + "\n";
			byte[] headLineArr = headLine.getBytes();
			byte[] sendBuff = new byte[headLineArr.length + bodyBytes.length];
			System.arraycopy(headLineArr, 0, sendBuff, 0, headLineArr.length);
			System.arraycopy(bodyBytes, 0, sendBuff, headLineArr.length, bodyBytes.length);
			websocket.sendMsg(this.sid, sendBuff);			
		}
		
	}
	protected void processRequestPrivateSocket() {
		//解析请求消息
		SocketRequest request = SocketRequest.parse(msg, channel);
		SocketResponse response = SocketResponse.clone(request);
		//处理Action请求
		executeAction(request, response);
		//输出结果
		//TODO：错误码机制
		if(response.isDisabled() == false) {
			//组装返回消息
			ByteArray sendBuff = new ByteArray(ByteBufferPool.malloc(ByteBufferPool.MAX_BUFFER_SIZE));
			try {
				response.writeSendBuff(sendBuff);
				//发送消息
				channel.send(sendBuff.getByteBuffer());
			} catch (IOException e) {
				NetLog.logWarn(e);
			} finally {
				ByteBufferPool.free(sendBuff.getByteBuffer()); //发送消息后，立即回收缓存区
				sendBuff = null;
			}
		}
	}
	

}
