package net_io.core;

public interface SocketEventHandle {
	public void onConnect(NetChannel channel) throws Exception;
	public void onReceive(NetChannel channel, ByteArray data) throws Exception;
	public void onClose(NetChannel channel) throws Exception;
}
