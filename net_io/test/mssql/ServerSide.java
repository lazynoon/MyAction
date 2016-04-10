package mssql;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.StreamSocket;
import net_io.core.NetChannel.CONFIG;
import net_io.utils.DateUtils;
import net_io.utils.NetLog;

public class ServerSide extends StreamSocket {
	private String serverHost = null;
	private int serverPort = 0;
	private BothSide both;
	private LinkInfo[] links = new LinkInfo[1024];
	private int keepAliveSize = 3;
	private ConcurrentHashMap<NetChannel, LinkInfo> map = new ConcurrentHashMap<NetChannel, LinkInfo>();
	private int connectChannelSize = 0;
	private int assignChannelSize = 0;
	

	private static class LinkInfo {
		NetChannel serverSide = null;
		boolean isFree = true;
		boolean isConnected = false;
		long activeTime = System.currentTimeMillis();
	}
	

	
	public ServerSide(BothSide both) {
		this.both = both;  
	}
	
	public void initConnectPool(String serverHost, int serverPort, int keepAliveSize) throws Exception {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.keepAliveSize = keepAliveSize;
		for(int i=0; i<links.length; i++) {
			links[i] = new LinkInfo();
		}

		this.autoConnect();
	}
	
	synchronized private void autoConnect() {
		int freeSize = this.connectChannelSize - this.assignChannelSize;
		int newSize = this.assignChannelSize / 3 + keepAliveSize - freeSize;
		int offset = 0;
		for(int i=0; i<links.length; i++) {
			if(offset >= newSize) {
				break;
			}
			LinkInfo link = links[i];
			if(link.serverSide != null) {
				continue;
			}
			try {
				link.serverSide = this.connect(serverHost, serverPort);
				offset++;
				this.connectChannelSize++;
			} catch (Exception e) {
				NetLog.logWarn(e);
			}
			map.put(link.serverSide, link);
		}

	}
	
	public NetChannel getFreeConnection() {
		synchronized(links) {
			for(LinkInfo link : links) {
				if(link.isFree && link.isConnected) {
					link.isFree = false;
					//分配了连接
					this.assignChannelSize++;
					this.autoConnect();
					return link.serverSide;
				}
			}
		}
		System.err.println(DateUtils.getDateTime() +" - The connection pool is busy. Pool size: "+links.length);
		return null;
	}
	
	public void setFreeConnection(NetChannel channel) {
		LinkInfo link = map.get(channel);
		if(link != null) { //Client Side
			link.activeTime = System.currentTimeMillis();
			this.closeChannel(link.serverSide);
			//link.isFree = true;
		}
	}

	public void onConnect(NetChannel channel) throws Exception {
		LinkInfo link = map.get(channel);
		if(link == null) { //Client Side 
			throw new Exception("not exists channel: "+channel);
		}
		link.activeTime = System.currentTimeMillis();
		link.isConnected = true;
		link.isFree = true;
		link.activeTime = System.currentTimeMillis();
		//连接
		channel.config(CONFIG.IDLE_TIMEOUT, 60 * 1000); //mssql 自动关闭120秒的空闲连接
		both.onServerConnect(channel);
		System.out.println(DateUtils.getDateTime() +" - [ServerSide] onConnect. ChannelID: "+channel.getChannelID());
	}
	
	public void onClose(NetChannel channel) throws Exception {
		LinkInfo link = map.remove(channel);
		if(link == null) {
			return;
		}
		link.activeTime = System.currentTimeMillis();
		link.isConnected = false;
		
		//关闭了连接
		this.connectChannelSize--;
		if(link.isFree == false) {
			this.assignChannelSize--;
			both.onServerClose(channel);
		}
		//建立新连接
		link.serverSide = null;
		link.isFree = true;
		this.autoConnect();
		
		System.out.println(DateUtils.getDateTime() +" - [ServerSide] onClose server side. ChannelID: "+channel.getChannelID()
				+ ", SelectKeys: "+channel.getBaseSocket().getSelectionKeyCount()
				+ ", TotalChannelCount: "+channel.getBaseSocket().getTotalChannelCount()
				+ ", ConnectServer: "+this.connectChannelSize
				+ ", Assign: "+this.assignChannelSize
				);
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		LinkInfo link = map.get(channel);
		if(link == null) {
			this.closeChannel(channel);
			return;
		}
		link.activeTime = System.currentTimeMillis();
		both.onServerReceive(channel, data);
	}
	

}
