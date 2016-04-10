package net_io.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net_io.core.NetChannel.Status;
import net_io.utils.NetLog;

public class NetChannelPool {
	// 管理线程ID（所有管理线程中唯一）
	private static AtomicLong threadID = new AtomicLong(0); 
	//Channel信息
	private Map<SocketChannel, NetChannel> chMap = null;
	private Object updateLock = new Object();
	// head.prev 队尾节点
	// head.next 队首节点
	private NetChannel head = new NetChannel();
	// 管理者线程
	private ManagerThread manager = null;
	// 新注册的Channel的序列号
	private long requestChannelSequence = 0;

	public NetChannelPool(int maxChannelCount) {
		chMap = new HashMap<SocketChannel, NetChannel>(maxChannelCount, 1); //固定大小的HashMap。key list伸缩
	}
	
	public NetChannel get(SocketChannel socket) {
		NetChannel channel = chMap.get(socket);
		if(channel == null || channel.status == NetChannel.Status.CLOSE_WAIT) {
			return null;
		}
		return channel;
	}
	
	/**
	 * 注册NetChannel到队列（仅BOSS线程调用）
	 * @param channel
	 */
	public void register(NetChannel channel) throws IOException {
		synchronized(updateLock) {
			channel.ID = ++requestChannelSequence; //保存序列号到NetChannel对象
			if(head.prev != null) {
				channel.prev = head.prev;
				head.prev.next = channel;
				head.prev = channel;
			} else { //第1个节点
				head.prev = head.next = channel;
			}
			chMap.put(channel.socket, channel);
			//管理者线程启动
			if(manager == null) {
				manager = new ManagerThread();
				manager.setDaemon(true); //设置为守护线程。即：若没有其他“用户线程”时，程序自动终止
				manager.start();
			}
		}
	}
	
	/**
	 * 检查各个通道的状态，并自动处理
	 */
	protected void checkChannelStatus() {
		NetChannel channel = head.next;
		while(channel != null) {
			if(channel.status == Status.CONNECT) {
				channel.checkConnectStatus();
			} else if(channel.status == Status.ESTABLISHED) {
				channel.checkEstablishedStatus();
			} else if(channel.status == Status.CLOSE_WAIT) {
				if(channel.lastAliveTime + channel.closeWaitTime < System.currentTimeMillis()) {
					if(NetLog.LOG_LEVEL >= NetLog.DEBUG) {
						NetLog.logDebug("Remove Close Wait Channel: "+channel);
					}
					channel.status = Status.CLOSED;
					//从队列中移除对象
					synchronized(updateLock) {
						chMap.remove(channel.socket);
						if(channel.prev != null) { //非队首
							channel.prev.next = channel.next;
						} else { //队首
							head.next = channel.next;
						}
						//队尾
						if(head.prev == channel) {
							head.prev = channel.prev;
						}
						//下一个节点的 prev 链接
						if(channel.next != null) {
							channel.next.prev = channel.prev;
						}
					}
				}
			}
			channel = channel.next;
		}
	}
	
	/**
	 * 获取 所有的 NetChannel 的数量（含 CLOSE_WAIT ）
	 */
	public int getTotalChannelCount() {
		return chMap.size();
	}
	
	/**
	 * NetChannelPool管理线程内部类
	 * @author Hansen
	 */
	private class ManagerThread extends Thread {
		public void start() {
			setName("NetChannelPool"+threadID.incrementAndGet());
			super.start();
		}
		public void run() {
			while(true) {
				try {
					checkChannelStatus();
					sleep(100); //0.1s 检测一次
				} catch(Exception e) {
					NetLog.logWarn("ManagerThread Exception: ");
					NetLog.logWarn(e);
				}
			}
		}
	}
}
