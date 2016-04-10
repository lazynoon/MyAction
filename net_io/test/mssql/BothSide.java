package mssql;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.utils.DateUtils;
import net_io.utils.NetLog;

public class BothSide {
	public ServerSide serverSideSocket = null;
	public ClientSide clientSideSocket = null;
	private ConcurrentHashMap<NetChannel, LinkInfo> serverSideMap = new ConcurrentHashMap<NetChannel, LinkInfo>(1024);
	private ConcurrentHashMap<NetChannel, LinkInfo> clientSideMap = new ConcurrentHashMap<NetChannel, LinkInfo>(1024);
	private ConcurrentHashMap<NetChannel, Boolean> alreadSentHead = new ConcurrentHashMap<NetChannel, Boolean>(1024);
	private boolean disableFirstPackageCache = false;
	private ByteArray sendBytes = null;
	private ByteArray recvBytes = null;
	
	public BothSide() {
		this.serverSideSocket = new ServerSide(this);
		this.clientSideSocket = new ClientSide(this);
	}
	
	private static class LinkInfo {
		NetChannel clientSide = null;
		NetChannel serverSide = null;
		boolean useHeadCache = false;
		long activeTime = System.currentTimeMillis();
	}
	
	public void onServerConnect(NetChannel channel) {
		if(disableFirstPackageCache == false && sendBytes != null && recvBytes != null) {
			try {
				//发送计数
				//channel.generateSequenceID();
				sendBytes.rewind();
				serverSideSocket.send(channel, sendBytes);
				alreadSentHead.put(channel, new Boolean(true));
			} catch (IOException e) {
				NetLog.logWarn(e);
				channel.close();
			}
		}
	}
	public void onClientConnect(NetChannel channel) {
		System.out.println("\n"+DateUtils.getDateTime() +" - [BOTH] onClientConnect: "+channel.getChannelID());
		LinkInfo link = new LinkInfo();
		link.clientSide = channel;
		link.serverSide =  serverSideSocket.getFreeConnection();
		if(link.serverSide == null) {
			System.out.println(DateUtils.getDateTime() +" - can not find the free server connection");
			clientSideSocket.closeChannel(channel);
		} else {
			serverSideMap.put(link.serverSide, link);
			clientSideMap.put(link.clientSide, link);
		}
	}
	
	public void onServerClose(NetChannel channel) {
		//System.out.println(DateUtils.getDateTime() +" - [BOTH] onServerClose: "+channel.getChannelID());
		LinkInfo link = serverSideMap.remove(channel);
		if(link == null) {
			System.err.println("Close unknown server side channel.");
			//serverSideSocket.closeChannel(channel);
			return;
		}
		clientSideSocket.closeChannel(link.clientSide);
	}
	public void onClientClose(NetChannel channel) {
		System.out.println(DateUtils.getDateTime() +" - [BOTH] onClientClose: "+channel.getChannelID());
		LinkInfo link = clientSideMap.remove(channel);
		if(link == null) {
			System.err.println("Close unknown client side channel.");
			return;
		}
		serverSideSocket.setFreeConnection(link.serverSide);
	}
	public void onServerReceive(NetChannel channel, ByteArray data) throws Exception {
		int seq = channel.generateSequenceID();
		
		System.out.println(DateUtils.getDateTime() +" - [BOTH] Receive. ChID: "+channel.getChannelID()+", Buff: "+data.size()+", MD5: "+MD5Util.md5(data));
		LinkInfo link = serverSideMap.get(channel);
		if(link == null) {
			if(seq <= 1) { //首个数据包
				if(disableFirstPackageCache == false && this.sendBytes != null) {
					return; //自动发送的第一个包
				}
			}
			System.err.println("receive unknown server side channel.");
			serverSideSocket.closeChannel(channel);
			return;
		} else if(seq <= 1) { //首个数据包
			//首次，首个数据包，进行缓存
			if(this.recvBytes == null) {
				this.recvBytes = data;
			}
		}
		clientSideSocket.send(link.clientSide, data);
		
	}
	public void onClientReceive(NetChannel channel, ByteArray data) throws Exception {
		System.out.println(DateUtils.getDateTime() +" - [BOTH] Send. ChID: "+channel.getChannelID()+", Buff: "+data.size()+", MD5: "+MD5Util.md5(data));
		LinkInfo link = clientSideMap.get(channel);
		if(link == null) {
			System.err.println(DateUtils.getDateTime() +" - receive unknown client side channel.");
			serverSideSocket.setFreeConnection(channel);
			return;
		}
		int clientSeq = channel.generateSequenceID();
		if(disableFirstPackageCache == false && clientSeq <= 1) { //首次发包
			if(sendBytes == null) {
				sendBytes = data;
				serverSideSocket.send(link.serverSide, data);
			} else if(recvBytes == null || alreadSentHead.containsKey(link.serverSide) == false) {
				serverSideSocket.send(link.serverSide, data);
// linux 平台个别字节变化，但是返回的结果是一样的。因此改为字节数检查
// FIXME：用户名，密码是固定长度的话，在密码改变后，需要重新启动程序
//			} else if(MD5Util.md5(data).equals(MD5Util.md5(sendBytes))) {
			} else if(data.size() == sendBytes.size()) {
				recvBytes.rewind();
				serverSideSocket.send(link.clientSide, recvBytes);
				System.out.println(DateUtils.getDateTime() +" - [BOTH] ***RETURN*** the cached buff. ChID: "+channel.getChannelID()+", Buff: "+recvBytes.size()+", MD5: "+MD5Util.md5(recvBytes));
			} else {
				System.out.println(DateUtils.getDateTime() +" Protocol is changed!!! Old send: "+MD5Util.md5(sendBytes));
				//协议变了，重新来过
				this.sendBytes = null;
				this.recvBytes = null;
				channel.close();
				
			}
			alreadSentHead.remove(link.serverSide); //移除已发head的标记
		} else {
			serverSideSocket.send(link.serverSide, data);
		}
	}
}
