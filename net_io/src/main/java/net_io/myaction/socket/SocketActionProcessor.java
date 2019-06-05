package net_io.myaction.socket;

import java.io.IOException;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.myaction.ActionProcessor;
import net_io.myaction.server.CommandMsg;
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
				processRequestWebSocket();
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
	protected void processRequestWebSocket() throws Exception {
		//解析请求消息
		SocketRequest request = SocketRequest.parseWebSocket(buff, websocket.getNetChannel(this.sid));
		SocketResponse response = new SocketResponse();
		//处理Action请求
		executeAction(request, response);
		//输出结果
		if(response.isDisabled() == false) {
			int contentLength = 0;
			byte[] body = response.getBodyBytes();
			if(body != null) {
				contentLength += body.length;
			}
			byte[] attachment = response.getAttachment();
			if(attachment != null) {
				contentLength += attachment.length;
			}
			//WebSocket format
			String headLine = Integer.toHexString(request.getVersion()) + "\t"
					+ Integer.toHexString(request.getRequestID()) + "\t"
					+ request.getPath() + "\n";
			byte[] headLineArr = headLine.getBytes();
			byte[] sendBuff = new byte[headLineArr.length + contentLength];
			System.arraycopy(headLineArr, 0, sendBuff, 0, headLineArr.length);
			int offset = headLineArr.length;
			if(body != null) {
				System.arraycopy(body, 0, sendBuff, offset, body.length);
				offset += body.length;
			}
			if(attachment != null) {
				System.arraycopy(attachment, 0, sendBuff, offset, body.length);
			}
			
			websocket.sendMsg(this.sid, sendBuff);			
		}
		
	}
	protected void processRequestPrivateSocket() {
		//解析请求消息
		SocketRequest request = SocketRequest.parse(msg, channel);
		SocketResponse response = new SocketResponse();
		//处理Action请求
		executeAction(request, response);
		//输出结果
		//TODO：错误码机制
		if(response.isDisabled() == false) {
			//组装返回消息
			CommandMsg msg = new CommandMsg();
			msg.resetMsgID(SocketResponse.MSG_ID);
			msg.setRequestID(request.getRequestID());
			msg.setPath(request.getPath());
			msg.resetData(response.getBodyMixed());
			msg.setAttachment(response.getAttachment());

			ByteArray sendBuff = new ByteArray(8192);
			try {
				msg.writeData(sendBuff);
				msg.finishWrite(sendBuff);
				//发送消息
				channel.send(sendBuff.getByteBuffer());
			} catch (IOException e) {
				NetLog.logWarn(e);
			}
		}
	}
	

}
