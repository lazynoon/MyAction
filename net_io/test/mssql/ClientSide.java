package mssql;

import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.StreamSocket;

public class ClientSide extends StreamSocket {
	private ConcurrentHashMap<NetChannel, LinkInfo> map = new ConcurrentHashMap<NetChannel, LinkInfo>();
	private BothSide both;
	

	private static class LinkInfo {
		NetChannel clientSide = null;
		boolean isFree = true;
		long activeTime = System.currentTimeMillis();
	}
	
	public ClientSide(BothSide both) {
		this.both = both;  
	}
	
	
	public void onConnect(NetChannel channel) throws Exception {
		LinkInfo link = new LinkInfo();
		link.clientSide = channel;
		map.put(channel, link);
		both.onClientConnect(channel);
	}
	
	public void onClose(NetChannel channel) throws Exception {
		LinkInfo link = map.remove(channel);
		if(link == null) {
			return;
		}
		both.onClientClose(channel);
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		LinkInfo link = map.get(channel);
		if(link == null) {
			this.closeChannel(channel);
			return;
		}
		link.activeTime = System.currentTimeMillis();
		both.onClientReceive(channel, data);
	}
	

}
