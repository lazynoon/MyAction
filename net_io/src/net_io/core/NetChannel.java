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
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import net_io.core.AsyncBaseSocket.EventUpdate;
import net_io.core.AsyncBaseSocket.EventUpdate.MODE;
import net_io.utils.DateUtils;
import net_io.utils.NetLog;

public class NetChannel {
	public enum CONFIG { CLOSE_WAIT_TIME, CONNECT_TIMEOUT, IDLE_TIMEOUT }
	
	/** 默认值：连接准备最后关闭的时间 **/
	final public static int DEFAULT_CLOSE_WAIT_TIME = 3*1000;
	
	/** 默认值：连接超时时间 **/
	final public static int DEFAULT_CONNECT_TIMEOUT = 120*1000;

	/** 默认值：空闲超时时间 **/
	final public static int DEFAULT_IDLE_TIMEOUT = 7200 * 1000; //
	
	/** 关闭状态停留时间（NetChannelPool.ManagerThread需要访问） **/
	protected int closeWaitTime = DEFAULT_CLOSE_WAIT_TIME;
	
	/** 连接超时时间 **/
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	
	/** 通道ID。注册到Pool时更新（累计） **/
	protected long ID = 0;
	
	/** 空闲超时时间 **/
	private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
	
	/** 默认值：拥塞超时时间（暂定） **/
//	final public static int DEFAULT_IDLE_TIME = 7200;
	
	/** 是否遇到了连接超时 **/
	private boolean isConnectTimeout = false;
	
	/** 等待连接完成的信号 **/
	private Object waitConnectSingle = new Object();
	
	/** 通道创建时间 **/
	private long channelCreateTime = System.currentTimeMillis();
	
	/** 最后活跃时间：产生（或准备）网络IO的时间 **/
	protected long lastAliveTime = 0;
	
	/** 最后的IO异常 **/
	protected IOException lastIOException = null;
	
//	/** 关闭时间（关闭后，等待 CLOSE_WAIT_TIME 之后，将从Pool中清除） **/
//	private long closeTime = 0;
	
	/** 状态：INIT（初始，刚创建）；CONNECT（发起连接）；ESTABLISHED（连接已建立）；CLOSE_WAIT（连接等待关闭）。 **/
	public enum Status{INIT, CONNECT, ESTABLISHED, CLOSE_WAIT, CLOSED}; // CLOSED 从Pool中直接删除，在应用类中，可用来检查状态
	
	/** 发送队列 **/
	private ConcurrentLinkedQueue<ByteBuffer> sendQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	
	/** 正在发送中的 ByteBuffer **/
	private ByteBuffer sendingBuff = null;
	
	/** 接收缓存区。不为null，则表示正在取数据，且未取满一个包 **/
	protected ByteBuffer recvBuff = null;
	
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
	
	/** 响应内容缓存池 **/
	private HashMap<Integer, ResponseContainer> responsePool = new HashMap<Integer, ResponseContainer>();
	
	/** NetChannelPool管理队列：前一个 NetChannel **/
	protected NetChannel prev = null;
	
	/** NetChannelPool管理队列：下一个 NetChannel **/
	protected NetChannel next = null;

	/**
	 * 作为队列的节点，创建对象
	 */
	protected NetChannel() {
	}
	
	/**
	 * 创建网络通道
	 * @param asyncSocket 异步Socket管理对象
	 * @param socket 
	 */
	protected NetChannel(AsyncBaseSocket asyncSocket, SocketChannel socketChannel) {
		this.lastAliveTime = this.channelCreateTime = System.currentTimeMillis();
		this.asyncSocket = asyncSocket;
		this.socket = socketChannel;
		this.lastIOException = null;
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
		} else if(type == CONFIG.IDLE_TIMEOUT) {
			idleTimeout = value;
		} else if(type == CONFIG.CLOSE_WAIT_TIME) {
			closeWaitTime = value;
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
	 * net_io 核心调用：连接中
	 */
	protected void _gotoConnect() {
		status = Status.CONNECT;
		this.lastAliveTime = System.currentTimeMillis();
	}
	
	/**
	 * net_io 核心调用：已建立连接
	 */
	protected void _gotoEstablished() {
		status = Status.ESTABLISHED;
		this.lastAliveTime = System.currentTimeMillis();
		synchronized(waitConnectSingle) {
			waitConnectSingle.notify();
			//waitConnectSingle = null;
		}
	}
	
	/**
	 * net_io 核心调用：关闭 NetChannel
	 */
	protected void _gotoCloseWait() {
		status = Status.CLOSE_WAIT;
//		closeTime = System.currentTimeMillis();
		responsePool = new HashMap<Integer, ResponseContainer>();
		this.lastAliveTime = System.currentTimeMillis();
		synchronized(waitConnectSingle) {
			waitConnectSingle.notify();
			//waitConnectSingle = null;
		}
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
	
	protected ByteBuffer _getSendBuff() {
		if(sendingBuff == null || sendingBuff.hasRemaining() == false) {
			sendingBuff = sendQueue.poll();
		}
		return sendingBuff;
	}

	public void send(ByteBuffer buff) throws IOException {
		buff.rewind();
		_send(buff);
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
		this.lastAliveTime = System.currentTimeMillis();
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
		asyncSocket.closeChannel(this);
	}
	
	/**
	 * 关闭连接
	 */
	public void closeAndSendRemaining() {
		if(status == Status.CLOSE_WAIT || status == Status.CLOSED) {
			return; //Channel已经关闭，则直接退出
		}
		
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
	protected void closed() {
		synchronized(responsePool) {
			ArrayList<Integer> keys = new ArrayList<Integer>();
			for(Integer objID : responsePool.keySet()) {
				keys.add(objID);
			}
			for(Integer objID : keys) {
				ResponseContainer container = responsePool.remove(objID);
				synchronized(container) {
					container.notifyAll();
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
		sb.append(", ActiveTime: "+DateUtils.getDateTime(new Date(lastAliveTime)));
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
	
	/**
	 * 获取响应包
	 * @param sequenceID 序列号
	 * @param timeout 超时时间（单位：秒）
	 * @return sequenceID的响应内容
	 */
	public Object fetchResponse(int sequenceID, int timeout) {
		boolean waitFlag = false;
		ResponseContainer container = null;
		Integer objID = new Integer(sequenceID);
		//从 responsePool 中取消息，取不到时自动创建
		try {
			synchronized(responsePool) {
				container = responsePool.get(objID);
				if(container == null) {
					container = new ResponseContainer();
					
					container.expireTime = System.currentTimeMillis() + timeout * 1000;
					waitFlag = true;
					responsePool.put(objID, container);
				}
			}
			//等待响应容器返回
			if(waitFlag && timeout > 0) {
				synchronized(container) {
					if(container.data == null) {
						try {
							if(this.status != Status.INIT && this.status != Status.CONNECT && this.status != Status.ESTABLISHED) {
								return null; //未连接
							}
							container.wait(timeout);
						} catch (InterruptedException e) {
							NetLog.logWarn(e);
						}
					}
				}
			}
		} finally {
			//从 responsePool 移除消息监听
			synchronized(responsePool) {
				responsePool.remove(objID);
			}
		}
		return container.data;
	}
	
	/**
	 * 保存响应包
	 * @param sequenceID 序列号
	 * @param data sequenceID的响应内容
	 */
	public void putResponse(int sequenceID, Object data) {
		boolean notifyFlag = false;
		ResponseContainer container = null;
		Integer objID = new Integer(sequenceID);
		//从 responsePool 中取响应包容器，取不到时自动创建
		synchronized(responsePool) {
			container = responsePool.get(objID);
			if(container == null) {
				container = new ResponseContainer();
				responsePool.put(objID, container);
			} else {
				notifyFlag = true;
			}
			container.data = data;
		}
		//通知接收线程，读取数据
		if(notifyFlag) {
			synchronized(container) {
				container.notify();
			}
		}
	}

	/**
	 * 根据 NetChannel 的状态，自动检查处理：CONNECT
	 */
	protected void checkConnectStatus() {
		long minTime = System.currentTimeMillis() - connectTimeout;
		if(minTime > lastAliveTime) {
			if(NetLog.LOG_LEVEL >= NetLog.DEBUG) {
				NetLog.logDebug("Close Connect Timeout Channel: "+this);
			}
			isConnectTimeout = true;
			asyncSocket.closeChannel(this);
		}
	}
	
	/**
	 * 根据 NetChannel 的状态，自动检查处理：ESTABLISHED
	 */
	protected void checkEstablishedStatus() {
		long minTime = System.currentTimeMillis() - idleTimeout;
		if(minTime > lastAliveTime) {
			if(NetLog.LOG_LEVEL >= NetLog.DEBUG) {
				NetLog.logDebug("Close Idle Timeout Channel: "+this);
			}
			asyncSocket.closeChannel(this);
		} else {
			//TODO 清除接收区缓存
		}
	}
	
	protected int clearResponseContainer(int maxExpireTime) {
		
		synchronized(responsePool) {
			
		}
		return 0;
	}

	private static class ResponseContainer {
		private ResponseContainer() {
			
		}
		private long startTime = 0;
		private long recvTime = 0;
		private long expireTime = 0;
		private Object data = null;
	}
	
}
