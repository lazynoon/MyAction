package net_io.myaction.server;

import net_io.core.ByteArray;
import net_io.msg.BaseMsg;
import net_io.utils.JSONException;
import net_io.utils.JSONUtils;
import net_io.utils.Mixed;

public class CommandMsg extends BaseMsg {
	public static final int MSG_ID = 0x8300;
//	protected int option = 0; //0: 请求；1：响应
	protected int requestID = 0;
	//private static AtomicLong generateRequestID = new AtomicLong(0);
	// /** 默认的RequestID。顺序自增加 **/
	//protected int requestID = (int)generateRequestID.incrementAndGet(); 
	protected String path = null;
//	protected String method = null;
	protected int systemCode = 0;
	protected int custemCode = 0;
	protected short contentType = 0;
	protected Mixed data = null;
	protected byte[] attachment = null;
	final protected static byte[] zeroBytes = new byte[0];
	
	public CommandMsg() {
		super(MSG_ID);
	}
	
	public CommandMsg(int msgID) {
		super(msgID);
	}
	
	public void resetData(Mixed data) {
		this.data = data;
	}
	
//	public void setResponseType(boolean flag) {
//		if(flag) {
//			option = 1;
//		} else {
//			option = 0;
//		}
//	}
//	
//	/**
//	 * 是否为请求类型
//	 * @return
//	 */
//	public boolean isRequest() {
//		return (option & 0x1) == 0;
//	}
//
//	/**
//	 * 是否为响应包
//	 * @return
//	 */
//	public boolean isResponse() {
//		return (option & 0x1) == 1;
//	}
	
	public Mixed get(String name) {
		if(data == null) {
			data = new Mixed();
		}
		return data.get(name);
	}

	public String getString(String name) {
		if(data == null) {
			data = new Mixed();
		}
		return data.getString(name);
	}

	public void set(String name, Object value) {
		if(data == null) {
			data = new Mixed();
		}
		data.set(name, value);
	}
	
	public Mixed getData() {
		return data;
	}
	
	public int getRequestID() {
		return requestID;
	}

	public void setRequestID(int requestID) {
		this.requestID = requestID;
	}

	public String getPath() {
		return path;
	}

	public CommandMsg setPath(String path) {
		this.path = path;
		return this;
	}

//	public String getMethod() {
//		return method;
//	}
//
//	public CommandMsg setMethod(String method) {
//		this.method = method;
//		return this;
//	}

	public byte[] getAttachment() {
		return attachment;
	}

	public void setAttachment(byte[] attachment) {
		this.attachment = attachment;
	}

	public void readData(ByteArray buff) {
		super.readData(buff);
		this.requestID = buff.readInt32();
		this.path = buff.readString();
		String content = buff.readString();
		if(content != null) {
			try {
				this.data = JSONUtils.parseJSON(content);
			} catch (JSONException e) {
				//TODO: exception process
				this.data = new Mixed();
			}
		} else {
			this.data = new Mixed();
		}
		this.attachment = buff.readBytes();
	}
	
	public void writeData(ByteArray buff) {
		super.writeData(buff);
		buff.writeInt32(this.requestID);
		buff.writeString(this.path != null ? this.path : "");
		buff.writeString(this.data != null ? this.data.toJSON() : "");
		buff.writeBytes(this.attachment != null ? this.attachment : zeroBytes);
	}
	
	public String toString() {
		return "[" + path + "] " +
				"SID" + requestID + " " + data;

	}
}
