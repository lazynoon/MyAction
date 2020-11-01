package net_io.core;

import java.net.InetSocketAddress;

public interface SocketEventHandle {
	public boolean onAccept(InetSocketAddress address) throws Exception;
	public void onConnect(NetChannel channel) throws Exception;
	public void onReceive(NetChannel channel, ByteArray data) throws Exception;
	public void onClose(NetChannel channel) throws Exception;
}
