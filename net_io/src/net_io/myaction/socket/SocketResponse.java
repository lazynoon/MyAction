package net_io.myaction.socket;

import java.io.IOException;

import net_io.core.ByteArray;
import net_io.myaction.Response;
import net_io.myaction.server.CommandMsg;
import net_io.utils.Mixed;

public class SocketResponse extends Response {
	public static final int MSG_ID = 0x8302;
	protected CommandMsg msg = null;
	
	public SocketResponse() {
		msg = new CommandMsg(MSG_ID);
	}

	public static SocketResponse clone(SocketRequest request) {
		SocketResponse response = new SocketResponse();
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
	
	public void writeSendBuff(ByteArray sendBuff) throws IOException {
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
