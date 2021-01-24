package net_io.core;

public class WebSocketChannel {
	String sid;
	NetChannel channel;
	int flag = 0;
	ByteArray buff = new ByteArray(4096); //head 最大4K
	long dataLength = 0;
	byte[] mask = null; //null表示没有MASK
	ByteArray receiveBuff = null;

	protected WebSocketChannel(String sid, NetChannel channel) {
		this.sid = sid;
		this.channel = channel;
	}

	public String getSid() {
		return sid;
	}

	public NetChannel getChannel() {
		return channel;
	}
}
