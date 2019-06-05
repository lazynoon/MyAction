package net_io.msg;

import net_io.core.ByteArray;

public abstract class BaseMsg extends MsgReadWrite {
	/** 最大消息体长度 **/
	public static final int MAX_MSG_LENGTH = 8 * 1024*1024;
	/** BaseMsg对象的HEAD区域长度（字节）：10B **/
	public static final int BASE_MSG_HEAD_LENGTH = 10;
	private int msgID = 0;
	private int originMsgID = 0;
	protected int option1 = 0;
	protected int option2 = 0;
	
	protected BaseMsg(int msgID) {
		this.originMsgID = this.msgID = msgID;
	}
	
	/**
	 * 获取消息ID
	 */
	public int getMsgID() {
		return msgID;
	}
	
	/**
	 * 获取在对象创建时的消息ID
	 */
	public int getOriginMsgID() {
		return originMsgID;
	}
	
	/**
	 * 重设消息ID
	 * @param msgID
	 */
	public void resetMsgID(int msgID) {
		this.msgID = msgID;
	}
	
	public void readData(ByteArray data) {
		//消息长度与消息号，在消息接收时已读取
		//读取可选参数
		option1 = data.readUInt16();
		option2 = data.readUInt16();
	}
	public void writeData(ByteArray data) {
		data.writeUInt32(0); //消息长度，初始化为0。在发送消息时设置
		data.writeUInt16(msgID); //消息ID
		data.writeUInt16(option1); //保留参数1
		data.writeUInt16(option2); //保留参数2
	}
	
	/**
	 * 写入完成时调用(写入消息的长度)
	 */
	public void finishWrite(ByteArray data) {
		data.finishWrite();
		data.writeUInt32(data.size());
		data.rewind();
	}

}
