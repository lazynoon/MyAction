package net_io.core;

import java.net.ServerSocket;

public abstract class AsyncSocketProcessor {
	public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
		return true;
	}
	
	abstract public void onConnect(NetChannel channel) throws Exception;
	abstract public void onClose(NetChannel channel) throws Exception;
	
	
	abstract public void onReceive(NetChannel channel) throws Exception;
	
}
