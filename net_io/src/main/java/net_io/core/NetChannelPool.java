package net_io.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net_io.utils.MemoryQueueCache;
import net_io.utils.NetLog;

class NetChannelPool {
	// 管理线程ID（所有管理线程中唯一）
	private static AtomicLong threadID = new AtomicLong(0);
	// 内存缓存时间：5秒
	protected static final long CACHE_TIME = 5 * 1000;
	// 新注册的Channel的序列号（全局唯一）
	private static AtomicLong requestChannelSequence = new AtomicLong(0);
	// 管理线程（定时器）
	private Timer timer = new Timer("NetChannelPool-"+threadID.incrementAndGet(), true);
	// Channel信息
	private ConcurrentHashMap<SocketChannel, NetChannel> chMap;
	//内存缓存对象
	protected MemoryQueueCache<Object> memoryCache;
	
	private static final long[] TASK_INTERVAL = {1*1000, 5*1000, 25*1000, 120*1000, 600*1000};
	private Map<NetChannel, Boolean>[] delayCheckList;
	

	@SuppressWarnings("unchecked")
	protected NetChannelPool() {
		chMap = new ConcurrentHashMap<SocketChannel, NetChannel>(1024); //初始化固定大小的HashMap。key list伸缩
		delayCheckList = new ConcurrentHashMap[TASK_INTERVAL.length];
		for(int i=0; i<delayCheckList.length; i++) {
			delayCheckList[i] = new ConcurrentHashMap<NetChannel, Boolean>();
		}
		//缓存队列
		memoryCache = new MemoryQueueCache<Object>(timer, CACHE_TIME);
		//设置定时器
		for(int i=0; i<TASK_INTERVAL.length; i++) {
			final int index = i;
			this.timer.schedule(new TimerTask() {
	
				@Override
				public void run() {
					try {
						checkChannelStatus(index);
					} catch(Exception e) {
						NetLog.logError("NetChannelPool Exception(1): ");
						NetLog.logError(e);
					}
				}
				
			}, TASK_INTERVAL[i], TASK_INTERVAL[i]);
		}
	}
	
	protected NetChannel get(SocketChannel socket) {
		return chMap.get(socket);
	}
	
	/**
	 * 注册NetChannel到队列（仅BOSS线程调用）
	 * @param channel
	 */
	protected void register(NetChannel channel) throws IOException {
		channel.ID = requestChannelSequence.incrementAndGet(); //保存序列号到NetChannel对象
		chMap.put(channel.socket, channel);
		//加入过期移除检查列表
		int findIndex = findCheckListIndex(channel._checkRemainingAliveTime(System.currentTimeMillis()));
		delayCheckList[findIndex].put(channel, new Boolean(true));
	}
	
	/** 关闭连接后调用：从连接池中删除 **/
	protected NetChannel remove(SocketChannel socket) {
		NetChannel channel = chMap.remove(socket);
		if(channel == null) {
			return null;
		}
		for(Map<NetChannel, Boolean> map : delayCheckList) {
			map.remove(channel);
		}
		return channel;
	}
	
	
	/**
	 * 检查各个通道的状态，并自动处理
	 */
	private void checkChannelStatus(int index) {
		long currentTime = System.currentTimeMillis();
		for(NetChannel channel : delayCheckList[index].keySet()) {
			long expireTime = channel._checkRemainingAliveTime(currentTime);
			//关闭过期连接
			if(expireTime < 0) {
				try {
					channel.close();
				} catch(Exception e) {
					NetLog.logError("NetChannelPool Exception(1): ");
					NetLog.logError(e);
				}
				remove(channel.socket); //从过期检查队列直接移除
			}
			//根据过期时间移位
			int findIndex = findCheckListIndex(expireTime);
			if(index != findIndex) {
				Boolean flag = delayCheckList[index].remove(channel);
				if(flag == null) {
					continue; //其它线程已移除（仅关闭连接的场合，20180103）
				}
				delayCheckList[findIndex].put(channel, flag);
			}
		}
	}
	
	
	/** 找一个合适的位置 **/
	private int findCheckListIndex(long expireTime) {
		int findIndex = 0;
		for(int i=0; i<TASK_INTERVAL.length; i++) {
			if(expireTime <= TASK_INTERVAL[i]) {
				if(i > 0) {
					findIndex = i - 1;
				}
				break;
			}
		}
		return findIndex;
		
	}
	
	/**
	 * 获取 所有的 NetChannel 的数量（含 CLOSE_WAIT ）
	 */
	protected int getTotalChannelCount() {
		return chMap.size();
	}
	
}
