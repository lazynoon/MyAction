package net_io.core;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import net_io.core.AsyncBaseSocket.EventUpdate;
import net_io.core.AsyncBaseSocket.EventUpdate.MODE;
import net_io.utils.DateUtils;
import net_io.utils.MemoryQueueCache;
import net_io.utils.NetLog;

public class NetChannel {
	public enum CONFIG { CONNECT_TIMEOUT, IDLE_TIMEOUT }
	
	/** 默认值：连接超时时间 **/
	final public static int DEFAULT_CONNECT_TIMEOUT = 120*1000;

	/** 默认值：空闲超时时间 **/
	final public static int DEFAULT_IDLE_TIMEOUT = 7200 * 1000; //
	
	/** 通道ID。注册到Pool时更新（累计） **/
	protected long ID = 0;
	
	/** 连接超时时间 **/
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	
	/** 空闲超时时间 **/
	private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
	
	/** 是否遇到了连接超时 **/
	private boolean isConnectTimeout = false;
	
	/** 等待连接完成的信号 **/
	private Object waitConnectSingle = new Object();
	
	/** 通道创建时间 **/
	private long channelCreateTime = System.currentTimeMillis();
	
	/** 最后活跃时间：产生（或准备）网络IO的时间 **/
	private long lastActiveTime = 0;
	
	/** 等待过期后关闭连接的时间 **/
	protected volatile long waitExpireTime = 0;

	/** 最后的IO异常 **/
	protected IOException lastIOException = null;
	
	/** 状态：INIT（初始，刚创建）；CONNECT（发起连接）；ESTABLISHED（连接已建立）。 **/
	public enum Status{INIT, CONNECT, ESTABLISHED, CLOSED}; // CLOSED 从Pool中直接删除。在应用类中，可用来检查状态
	
	/** 发送队列 **/
	private ConcurrentLinkedQueue<ByteBuffer> sendQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	
	/** 正在发送中的 ByteBuffer **/
	private ByteBuffer sendingBuff = null;
	
	/** 接收缓存区。不为null，则表示正在取数据，且未取满一个包 **/
	protected ByteBuffer recvBuff = null;
	
	/** 从Socket读取次数 **/
	protected long socketReadCount = 0;
	
	/** 从Socket写入次数 **/
	protected long socketWriteCount = 0;
	
	/** 网络通道的状态 **/
	protected Status status = Status.INIT;
	
	/** 异步 Socket 管理对象 **/
	protected AsyncBaseSocket asyncSocket;
	
	/** 对应到 NIO 中的 Channel **/
	protected SocketChannel socket;
	
	protected AsyncBaseSocket.ChannelRegisterHandle channelRegisterHandle = null;
	/** 是否为服务器端的 Channel **/
	protected boolean serverSide = false;
	
	/** 自增长序列号（调用时有效） **/
	private AtomicLong sequenceID = new AtomicLong(0);
	
	/** 响应内容KEY的缓存池 **/
	private ConcurrentHashMap<String, Long> responseKeyMap = new ConcurrentHashMap<String, Long>();
	
	/** 通道存储（生命周期：连接建立期间） **/
	private ConcurrentHashMap<String, Object> channelStorage = new ConcurrentHashMap<String, Object>();

	/** 移除缓存KEY的回调 **/
	private RemoveCacheKeyMonitor removeCacheKeyMonitor = new RemoveCacheKeyMonitor();

	/** 连接建立事件侦听列表 **/
	private ArrayList<Event> listConnectEvent = new ArrayList<Event>();
	/** 系统扫描事件侦听列表 **/
	private ArrayList<Event> listScanEvent = new ArrayList<Event>();
	/** 连接关闭事件侦听列表 **/
	private ArrayList<Event> listCloseEvent = new ArrayList<Event>();
	
	/**
	 * 创建网络通道
	 * @param asyncSocket 异步Socket管理对象
	 * @param socketChannel
	 */
	protected NetChannel(AsyncBaseSocket asyncSocket, SocketChannel socketChannel) {
		this.channelCreateTime = System.currentTimeMillis();
		this.asyncSocket = asyncSocket;
		this.socket = socketChannel;
		this.lastIOException = null;
		this.connectTimeout = Math.min(this.connectTimeout, asyncSocket.maxConnectTimeout);
		this.idleTimeout = Math.min(this.idleTimeout, asyncSocket.maxIdleTimeout);
		_setActiveTime(); //设置激活状态
	}
	
	/**
	 * 配置网络通道的参数 
	 * @param type 参数类型
	 * @param value 值
	 * @return 返回 this 对象
	 */
	public NetChannel config(CONFIG type, int value) {
		if(type == CONFIG.CONNECT_TIMEOUT) {
			connectTimeout = value;
			this.connectTimeout = Math.min(this.connectTimeout, this.asyncSocket.maxConnectTimeout);
		} else if(type == CONFIG.IDLE_TIMEOUT) {
			idleTimeout = value;
			this.idleTimeout = Math.min(this.idleTimeout, this.asyncSocket.maxIdleTimeout);
		}
		return this;
	}
	
	/**
	 * 取得通道ID
	 */
	public long getChannelID() {
		return ID;
	}
	
	/**
	 * 从Socket读取的次数（由NIO底层计数）
	 */
	public long getSocketReadCount() {
		return socketReadCount;
	}
	
	/**
	 * 从Socket写入的次数（由NIO底层计数）
	 */
	public long getSocketWriteCount() {
		return socketWriteCount;
	}
	
	/**
	 * 等待连接完成
	 * @param timeout 连接超时时间（单位：微秒）
	 * @return true 连接成功， false连接超时
	 * @throws ConnectException 连接失败异常
	 */
	public boolean waitConnect(long timeout) throws ConnectException {
		if(status != Status.CONNECT) {
			return _returnWaitConnect();
		}
		connectTimeout = timeout;
		synchronized(waitConnectSingle) {
			if(status != Status.CONNECT) {
				return _returnWaitConnect();
			}
			try {
				waitConnectSingle.wait(timeout);
			} catch (InterruptedException e) {
				//ignore this exception
			}
			return _returnWaitConnect();
		}
	}
	
	protected void _setActiveTime() {
		lastActiveTime = System.currentTimeMillis();
		if(status == Status.ESTABLISHED) {
			waitExpireTime = lastActiveTime + idleTimeout;
		} else if(status == Status.CONNECT || status == Status.INIT) {
			waitExpireTime = lastActiveTime + connectTimeout;						
		} else { //CLOSE
			waitExpireTime = lastActiveTime - DEFAULT_IDLE_TIMEOUT; //设置过期
		}
	}
	
	/**
	 * 返回waitConnect的结果
	 * @return true 连接成功， false连接超时
	 * @throws ConnectException 连接失败
	 */
	private boolean _returnWaitConnect() throws ConnectException {
		if(status == Status.ESTABLISHED) {
			return true;
		}
		IOException e = lastIOException;
		if(e == null) {
			return false;
		}
		if(e instanceof ConnectException) {
			throw (ConnectException) e;
		}
		return false;
	}
	
	/**
	 * 获取“异步Socket管理对象”
	 */
	public AsyncBaseSocket getBaseSocket() {
		return asyncSocket;
	}
	
	/**
	 * 获取 通道的状态
	 */
	public Status getChannelStatus() {
		return status;
	}
	
	/**
	 * 获取远端网络地址
	 * 仅支持 Internet Protocol。
	 * @return 成功返回 InetSocketAddress，除此之外，全部返回null
	 */
	public InetSocketAddress getRemoteAddress() {
		if(socket != null) {
			try {
				SocketAddress address = socket.getRemoteAddress();
				if(address instanceof InetSocketAddress) {
					return (InetSocketAddress) address; 
				}
			} catch (IOException e) {
				//取不到远端网络地址，全部返回null
			}
		}
		return null;
	}

	/**
	 * 获取最后活动时间
	 */
	public long getLastActiveTime() {
		return lastActiveTime;
	}

	/**
	 * net_io 核心调用：连接中
	 */
	protected void _gotoConnect() {
		status = Status.CONNECT;
		_setActiveTime();
	}
	
	/**
	 * net_io 核心调用：已建立连接
	 */
	protected void _gotoEstablished() {
		status = Status.ESTABLISHED;
		_setActiveTime();
		synchronized(waitConnectSingle) {
			waitConnectSingle.notifyAll();
		}
		processEvent(listConnectEvent); //处理连接建立事件
	}
	
	/**
	 * 是否为服务器端的Channel
	 * @return true or false
	 */
	public boolean isServerSide() {
		return serverSide;
	}
	
	/**
	 * 是否活跃中的Channel(连接建立状态)
	 * @return true of false
	 */
	public boolean isActive() {
		return (status == Status.ESTABLISHED);
	}
	public int getLocalPort() {
		return socket.socket().getLocalPort();
	}
	
	public long getCreateTime() {
		return channelCreateTime;
	}
	
	/** 核心调用：获取发送缓存 **/
	protected ByteBuffer _getSendBuff() {
		_setActiveTime(); //设置激活状态
		if(sendingBuff == null || sendingBuff.hasRemaining() == false) {
			sendingBuff = sendQueue.poll();
		}
		return sendingBuff;
	}

	public void send(ByteBuffer buff) throws IOException {
//		buff.rewind();
		_send(buff);
	}
	
	public void send(ByteArray data) throws IOException {
//		buff.rewind();
		send(data.getByteBuffer());
	}
	
	/**
	 * 暂停读取
	 * @throws IOException 
	 */
	public void pauseRead() throws IOException {
		SelectionKey key = socket.keyFor(asyncSocket.selector);
		if(key == null) {
			throw new IOException("Can not find the SelectionKey.");
		}
		key.interestOps(key.interestOps() & ~SelectionKey.OP_READ); //监听写事件
	}
	
	/**
	 * 重新读取
	 * @throws IOException 
	 */
	public void resumeRead() throws IOException {
		SelectionKey key = socket.keyFor(asyncSocket.selector);
		if(key == null) {
			throw new IOException("Can not find the SelectionKey.");
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_READ); //监听写事件		
	}
	
	public int getSendQueueSize() {
		return sendQueue.size();
	}
	
	protected void _send(ByteBuffer buff) throws IOException {
		_setActiveTime();
		int size = buff.limit() - buff.position();
		ByteBuffer sendBuff = ByteBuffer.allocate(size);
		if(size > 0) {
			System.arraycopy(buff.array(), buff.position(), sendBuff.array(), 0, size);
			sendBuff.limit(size);
		}
		sendQueue.offer(sendBuff);
		SelectionKey key = socket.keyFor(asyncSocket.selector);
		if(key == null) {
			return; //发起连接后，立即发消息，导致 channel 未注册
			//TODO: 其它错误检查，处理
			//throw new IOException("channel is not open. Socket is registered: "+socket.isRegistered()+", is open: "+socket.isOpen());
		}
		//key.interestOps(key.interestOps() | SelectionKey.OP_WRITE); //监听写事件
		asyncSocket.eventUpdateQueue.offer(new EventUpdate(key, SelectionKey.OP_WRITE, MODE.OR));
		asyncSocket.selector.wakeup();
	}
	
	/**
	 * 关闭连接
	 */
	public void close() {
		if(status == Status.CLOSED) {
			return; //Channel已经关闭，则直接退出
		}
		status = Status.CLOSED; //设置CLOSE状态1
		asyncSocket.closeChannel(this);
		processEvent(listCloseEvent); //处理关闭事件
	}
	
	/**
	 * 关闭连接
	 */
	public void closeAndSendRemaining() {
		if(status == Status.CLOSED) {
			return; //Channel已经关闭，则直接退出
		}
		status = Status.CLOSED; //设置CLOSE状态2
		
		//取消读取时间
		SelectionKey key = socket.keyFor(asyncSocket.selector);
		if(key == null) {
			return; //发起连接后，立即发消息，导致 channel 未注册
			//TODO: 其它错误检查，处理
			//throw new IOException("channel is not open. Socket is registered: "+socket.isRegistered()+", is open: "+socket.isOpen());
		}
//		if(sendQueue.isEmpty() && (sendingBuff == null || sendingBuff.hasRemaining() == false)) {
//			asyncSocket.closeChannel(this);
//			return;
//		}
		//去除读取监听事件
		key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
		final NetChannel that = this;
		new Thread() {
			public void run() {
				for(int i=0; i<600; i++) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						NetLog.logDebug(e);
					}
					if(sendQueue.isEmpty() && (sendingBuff == null || sendingBuff.hasRemaining() == false)) {
						break;
					}
				}
				asyncSocket.closeChannel(that);
			}
		}.start();
	}
	
	/**
	 * 连接被关闭了。由底层调用
	 */
	protected void _doClosed() {
		status = Status.CLOSED; //设置CLOSE状态3
		//通知连接结束
		synchronized(waitConnectSingle) {
			waitConnectSingle.notifyAll();
		}
		//清空发送队列
		sendQueue.clear();
		//清空接收缓存
		if(responseKeyMap.size() > 0) {
			MemoryQueueCache<Object> memoryCache = asyncSocket.channelPool.memoryCache;
			for(String cacheKey : responseKeyMap.keySet()) {
				responseKeyMap.remove(cacheKey);
				memoryCache.remove(cacheKey);
				synchronized(cacheKey) {
					cacheKey.notifyAll();
				}
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID: "+ID);
		sb.append(", Status: "+status);
		if(socket != null) {
			sb.append(", Channel: "+socket.socket().getRemoteSocketAddress());
		}
		sb.append(", ActiveTime: "+DateUtils.getDateTime(new Date(lastActiveTime)));
		return sb.toString();
	}
	
	public boolean isConnectTimeout() {
		return isConnectTimeout;
	}
	
	
	/**
	 * 生成并返回，自增长序列号（int类型，可以为负数）
	 */
	public int generateSequenceID() {
		return (int) sequenceID.incrementAndGet();
	}
	
	/** 从通道存储中取出数据 **/
	public Object getChannelStorage(String key) {
		return channelStorage.get(key);
	}
	
	/** 保存到通道存储中 **/
	public void setChannelStorage(String key, Object value) {
		channelStorage.put(key, value);
	}
	
	/** 是否存在于通道存储中 **/
	public boolean containsChannelStorage(String key) {
		return channelStorage.containsKey(key);
	}
	
	/** 删除通道存储中的数据 **/
	public void removeChannelStorage(String key) {
		channelStorage.remove(key);
	}
	
	/**
	 * 获取响应包
	 * @param sequenceID 序列号
	 * @param timeout 超时时间（单位：ms）
	 * @return sequenceID的响应内容
	 */
	public Object fetchResponse(int sequenceID, long timeout) throws IOException {
		String cacheKey = getResponseCacheKey(sequenceID); //缓存KEY
		MemoryQueueCache<Object> memoryCache = asyncSocket.channelPool.memoryCache;
		Object data = memoryCache.get(cacheKey);
		if(data == null && timeout > 0) {
			synchronized(cacheKey) {
				try {
					cacheKey.wait(timeout);
				} catch (InterruptedException e) {
					NetLog.logDebug(e); //ignore...
				}
			}
			data = memoryCache.get(cacheKey);
		}
		//取到数据后，清除缓存
		if(data != null) {
			memoryCache.remove(cacheKey);
			responseKeyMap.remove(cacheKey);
		} else { //未取到数据，检查错误类型
			if(timeout > 0) {
				throw new IOException("Receive timeout.");
			}
			if(this.status == Status.CLOSED) {
				throw new IOException("The connection is closed.");
			}
		}
		return data;
	}
	
	/**
	 * 保存响应包
	 * @param sequenceID 序列号
	 * @param data sequenceID的响应内容
	 */
	public void putResponse(int sequenceID, Object data) {
		if(data == null) {
			throw new NullPointerException("[NetChannel.putResponse] data is null.");
		}
		String cacheKey = getResponseCacheKey(sequenceID); //缓存KEY
		//先加入当前类的缓存KEY，因为memoryCache通过定时器移除KEY时，会有一次回调。
		responseKeyMap.put(cacheKey, new Long(System.currentTimeMillis()));
		//加入缓存
		asyncSocket.channelPool.memoryCache.set(cacheKey, data, removeCacheKeyMonitor);
		//通知接收线程，读取数据
		synchronized(cacheKey) {
			cacheKey.notifyAll();
		}
	}
	
	/** 接收缓存KEY（支持同步锁） **/
	private String getResponseCacheKey(int sequenceID) {
		StringBuilder build = new StringBuilder();
		build.append("NET-IO-CHANNEL-");
		build.append(String.valueOf(ID));
		build.append("-");
		build.append(String.valueOf(sequenceID));
		build.append("#");
		return build.toString().intern();
	}

	/** 核心调用：剩余的存活时间（过期关闭） **/
	protected long _checkRemainingAliveTime(long currentTime) {
		processEvent(listScanEvent); //扫描事件
		return waitExpireTime - currentTime;
	}
	
	public void listenConnectEvent(Event event) {
		addEvent(listConnectEvent, event);
	}
	
	public void listenScanEvent(Event event) {
		addEvent(listScanEvent, event);
	}
	
	public void listenCloseEvent(Event event) {
		addEvent(listCloseEvent, event);
	}
	
	private void addEvent(List<Event> listEvent, Event event) {
		synchronized(listEvent) {
			listEvent.add(event);
		}
	}
	
	private void processEvent(List<Event> listEvent) {
		if(listEvent.isEmpty()) {
			return; //忽略并发导致的延迟更新size的问题
		}
		synchronized(listEvent) {
			for(Event event : listEvent) {
				if(event.handle(this) == false) {
					break;
				}
			}
		}		
	}
	
	/** NetChannel的事件 **/
	public static interface Event {
		/**
		 * 事件处理方法
		 * @param channel 触发事件的NetChannel
		 * @return true继续下一条规则，false当前规则运行完毕后退出 
		 */
		public boolean handle(NetChannel channel);
	}
	
	private class RemoveCacheKeyMonitor implements MemoryQueueCache.RemoveMonitor {

		@Override
		public void callback(String removeKey) {
			if(responseKeyMap.remove(removeKey) != null) {
				synchronized(removeKey) {
					removeKey.notifyAll();
				}
			}
		}
		
	}

//	/**
//	 * 根据 NetChannel 的状态，自动检查处理：CONNECT
//	 */
//	protected void checkConnectStatus() {
//		long minTime = System.currentTimeMillis() - connectTimeout;
//		if(minTime > lastActiveTime) {
//			if(NetLog.LOG_LEVEL >= NetLog.DEBUG) {
//				NetLog.logDebug("Close Connect Timeout Channel: "+this);
//			}
//			isConnectTimeout = true;
//			asyncSocket.closeChannel(this);
//		}
//	}
//	
//	/**
//	 * 根据 NetChannel 的状态，自动检查处理：ESTABLISHED
//	 */
//	protected void checkEstablishedStatus() {
//		long minTime = System.currentTimeMillis() - idleTimeout;
//		if(minTime > lastActiveTime) {
//			if(NetLog.LOG_LEVEL >= NetLog.DEBUG) {
//				NetLog.logDebug("Close Idle Timeout Channel: "+this);
//			}
//			asyncSocket.closeChannel(this);
//		} else {
//			//TODO 清除接收区缓存
//		}
//	}
//	
	
}
