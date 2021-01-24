package net_io.core;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net_io.core.ssl.SSLChannel;
import net_io.utils.NetLog;


public class AsyncBaseSocket {
	/**
	 * 网络协议类型
	 *   PLAIN 普通Socket
	 *   SSL   SSL Socket
	 */
	public static enum NetType {PLAIN, SSL}
	/** 等待连接的队列长度 **/
	public static final int DEFAULT_BACKLOG = 1024;
	protected Selector selector;
	AsyncSocketProcessor processor = null;
	private String name = null; //Thread Name
	protected RunThread run = null;
	private boolean runable = true;
	
	//是否作为守护线程运行。默认为：0，表示可自动根据端类型设置。ClientSide默认设置为1（即：守护线程），ServerSide默认为2（即：用户线程） 
	private int daemon = 0; 
	
	/** Lister Channel Map **/
	private Map<InetSocketAddress, ServerSocketChannel> listenChannels = new ConcurrentHashMap<InetSocketAddress, ServerSocketChannel>();
	
	protected ConcurrentLinkedQueue<NetChannel> regQueue = new ConcurrentLinkedQueue<NetChannel>();
	protected ConcurrentLinkedQueue<ServerSocketChannel> bindQueue = new ConcurrentLinkedQueue<ServerSocketChannel>();
	protected ConcurrentLinkedQueue<EventUpdate> eventUpdateQueue = new ConcurrentLinkedQueue<EventUpdate>();
	protected NetChannelPool channelPool = new NetChannelPool();
	
	/** 最大连接超时时间（服务器端Socket直接使用；客户端Socket仅对其后更新连接超时时间的NetChannel有效） **/
	protected long maxConnectTimeout = 86400 * 1000;
	/** 最大空闲超时时间（服务器端Socket直接使用；客户端Socket仅对其后更新空闲超时时间的NetChannel有效） **/
	protected long maxIdleTimeout = 86400 * 1000;
	/** 网络协议类型 **/
	private NetType netType = NetType.PLAIN;
	
	// 绑定的端口
//	private ArrayList<InetSocketAddress> bindAddressList = null;
//	private ArrayList<ServerSocketChannel> serverChannels = new ArrayList<ServerSocketChannel>();

	protected AsyncBaseSocket() {
		this.processor = new MyAsyncSocketProcessor();
		StatNIO.bossClass.create_aync_socket.incrementAndGet(); //StatNIO
	}

	public AsyncBaseSocket(AsyncSocketProcessor processor) {
		this.processor = processor;
		StatNIO.bossClass.create_aync_socket.incrementAndGet(); //StatNIO
	}

	protected void init(AsyncSocketProcessor processor) {
		this.processor = processor;
	}
	
	public void setThreadName(String name) {
		this.name = name;
	}
	
	public String getThreadName() {
		return this.name;
	}
	
	public void setDaemon(boolean daemon) {
		if(daemon) {
			this.daemon = 1;
		} else {
			this.daemon = 2;
		}
		if(run != null) {
			run.setDaemon(this.daemon != 2);
		}
	}
	
	/**
	 * 是否后台运行（没有其它线程，仍将继续运行）
	 */
	public boolean isDaemon() {
		return (this.daemon != 2);
	}
	
	/**
	 * 设置最大连接超时时间（服务器端Socket直接使用；客户端Socket仅对其后更新连接超时时间的NetChannel有效）
	 * @param timeout 最大超时时间（单位：ms）
	 */
	public void setMaxConnectTimeout(long timeout) {
		this.maxConnectTimeout = Math.max(timeout, 0);
	}
	
	/**
	 * 最大空闲超时时间（服务器端Socket直接使用；客户端Socket仅对其后更新空闲超时时间的NetChannel有效）
	 * @param timeout 最大超时时间（单位：ms）
	 */
	public void setMaxIdleTimeout(long timeout) {
		this.maxIdleTimeout = Math.max(timeout, 0);
	}
	
	/** 设置网络协议类型 **/
	protected void setNetType(NetType netType) {
		this.netType = netType;
	}
	
	/** 获取网络协议类型 **/
	protected NetType getNetType() {
		return netType;
	}
	
	/**
	 * 侦听一个端口号（不限IP）
	 * @param port 端口号
	 * @throws IOException
	 */
	public void bind(int port) throws IOException {
		bind(new InetSocketAddress(port));
	}
	public void bind(InetSocketAddress address) throws IOException {
		bind(address, DEFAULT_BACKLOG);
	}
	
	/**
	 * 侦听端口号
	 * @param address InetSocketAddress网络地址
	 * @param backlog requested maximum length of the queue of incoming connections.
	 * @throws IOException
	 */
	synchronized public void bind(InetSocketAddress address, int backlog) throws IOException {
		StatNIO.bossClass.bind_invoke_start.incrementAndGet(); //StatNIO
		//设置 daemon 默认为 “用户线程”
		if(this.daemon == 0) {
			this.daemon = 2;
		}
		//检查并启动BOSS线程（含selector初始化）
		boolean needStartThread = false;
		if(run == null) {
			needStartThread = initBossThread();
		}
		//侦听Socket初始化
		ServerSocketChannel serverChannel = ServerSocketChannel.open();	//Open this server socket channel.
		serverChannel.socket().setReuseAddress(true);
		serverChannel.socket().bind(address, backlog);	//Bind a port, and listen to it.
		//配置SOCKET为非阻塞模式
		serverChannel.configureBlocking(false);

		//保存侦听对象
		listenChannels.put(address, serverChannel);
		//加入到接收连接的处理队列
		bindQueue.add(serverChannel);
		
		
		//保存已绑定的地址
//		if(bindAddressList == null) {
//			bindAddressList = new ArrayList<InetSocketAddress>();
//		}
//		bindAddressList.add(address);
		
		//启动线程
		if(needStartThread) {
			run.start();
		} else {
			selector.wakeup();
		}
		StatNIO.bossClass.bind_invoke_end.incrementAndGet(); //StatNIO
	}
	
	/**
	 * 取消侦听地址
	 * @param port 端口号
	 * @throws IOException 
	 */
	public void unbind(int port) throws IOException {
		unbind(new InetSocketAddress(port));
	}
	
	/**
	 * 取消侦听地址
	 * @param address InetSocketAddress
	 * @throws IOException 
	 */
	public void unbind(InetSocketAddress address) throws IOException {
		StatNIO.bossClass.unbind_invoke_start.incrementAndGet(); //StatNIO
		ServerSocketChannel channel = listenChannels.remove(address);
		if(channel == null) {
			StatNIO.bossClass.unbind_not_exist.incrementAndGet(); //StatNIO
			return;
		}
		channel.socket().close();
		channel.close();
		StatNIO.bossClass.unbind_invoke_end.incrementAndGet(); //StatNIO
	}
	
	/**
	 * 取消所有侦听
	 */
	public void unbindAll() {
		for(InetSocketAddress address : listenChannels.keySet()) {
			try {
				unbind(address);
			} catch (IOException e) {
				NetLog.logError(e);
			}
		}
	}
	
	
	/**
	 * 发起连接
	 * @param host
	 * @param port
	 * @return 非空 NetChannel
	 * @throws Exception
	 */
	public NetChannel connect(String host, int port) throws IOException {
		InetSocketAddress remote = new InetSocketAddress(host, port);
		return connect(remote);
	}
	
	/**
	 * 发起连接
	 * @param remote
	 * @return 非空 NetChannel
	 * @throws Exception
	 */
	public NetChannel connect(final InetSocketAddress remote) throws IOException {
		StatNIO.bossClass.connect_invoke_start.incrementAndGet(); //StatNIO
		//检查并启动BOSS线程（含selector初始化）
		boolean needStartThread = false;
		if(run == null) {
			needStartThread = initBossThread(); //内部含同步锁
		}
		//连接初始化
		SocketChannel socket = SocketChannel.open();
		socket.configureBlocking(false);
		//封装为网络频道
		NetChannel channel;
		if(netType == NetType.SSL) {
			channel = new SSLChannel(this, socket);			
		} else {
			channel = new NetChannel(this, socket);
		}
		channel._gotoConnect(); //设置NetChannel状态：CONNECT
		channelPool.register(channel);
		final AsyncBaseSocket asyncSocket = this;

		//注册到回调事件中
		channel.channelRegisterHandle = new ChannelRegisterHandle() {

			@Override
			public void doConnect(NetChannel channel) throws Exception {
				StatNIO.bossClass.do_connect_start.incrementAndGet(); //StatNIO
				boolean ret = channel.socket.connect(remote);
				if(ret) { //连接立即成功
					StatNIO.bossClass.do_connect_immediate.incrementAndGet(); //StatNIO
					channel._gotoEstablished(); //设置NetChannel状态：ESTABLISHED
					//增加监控“读”事件
					SelectionKey key = channel.socket.keyFor(asyncSocket.selector);
					key.interestOps(key.interestOps() | SelectionKey.OP_READ);
					//回调连接成功消息
					asyncSocket.processor.onConnect(channel);
					StatNIO.bossClass.do_connect_finish.incrementAndGet(); //StatNIO
				}
				StatNIO.bossClass.do_connect_end.incrementAndGet(); //StatNIO
			}
			
		};
		
		//注册epoll事件
		regQueue.offer(channel);
		
		if(needStartThread) {
			run.start();
		} else {
			selector.wakeup();
		}
		StatNIO.bossClass.connect_invoke_end.incrementAndGet(); //StatNIO
		return channel;
	}
	
//	public void send(NetChannel channel, ByteBuffer buff) throws IOException {
//		buff.rewind();
//		ByteBuffer newBuff = buff.slice();
//		channel._send(newBuff);
//	}
//	
//	protected void sendDirect(NetChannel channel, ByteBuffer buff) throws IOException {
//		channel._send(buff);
//	}
//	
	
	/**
	 * 启动BOSS线程
	 * @throws IOException
	 */
	private boolean initBossThread() throws IOException {
		synchronized(this) {
			if(run == null) {
				this.selector = Selector.open();
				run = new RunThread(this);
				//run.start();
				return true;
			} else {
				return false;
			}
		}
	}
	
	public void closeChannel(NetChannel channel) {
		closeSocketChannel(channel.socket);
	}
	
	/**
	 * 关闭SocketChannel
	 * @param socket
	 */
	protected void closeSocketChannel(SocketChannel socket) {
		StatNIO.bossClass.close_channel_start.incrementAndGet(); //StatNIO
		//boolean firstClose = socket.isOpen();
		//NetLog.logDebug(new Exception("closeSocketChannel: "+socket));
		if(NetLog.LOG_LEVEL <= NetLog.DEBUG) {
			NetLog.logDebug("closeSocketChannel: "+socket);
		}
		//1. 先关闭 socket 连接
		try {
			try {
				socket.socket().close();
				socket.close();
				StatNIO.bossClass.close_socket_pass.incrementAndGet(); //StatNIO
			} catch (IOException e) {
				StatNIO.bossClass.close_socket_error.incrementAndGet(); //StatNIO
				NetLog.logWarn("[SocketCloseError] "+Thread.currentThread().getName());
				NetLog.logWarn(e);
			}
			SelectionKey key = socket.keyFor(selector);
			if(key != null) {
				StatNIO.bossClass.close_selection_cancel.incrementAndGet(); //StatNIO
				key.cancel();
			} else {
				StatNIO.bossClass.close_selection_null.incrementAndGet(); //StatNIO
				//NetLog.logDebug("Cat not find SelectionKey. Socket: "+socket);
			}
		} catch(Exception e) {
			NetLog.logWarn("[SocketCloseError] "+Thread.currentThread().getName());
			NetLog.logWarn(e);
		}
		//1. 再关闭 NetChannel
		NetChannel channel = channelPool.get(socket);
		if(channel != null) {
			StatNIO.bossClass.close_net_channel.incrementAndGet(); //StatNIO
			channel._doClosed(); //设置NetChannel状态：CLOSED
			try {
				processor.onClose(channel);
				StatNIO.bossClass.close_goto_wait.incrementAndGet(); //StatNIO
			} catch (Exception e) {
				StatNIO.bossClass.close_callback_error.incrementAndGet(); //StatNIO
				NetLog.logWarn("[AppCloseError] "+Thread.currentThread().getName());
				NetLog.logWarn(e);
			}
		} else {
			StatNIO.bossClass.close_twice_invoke.incrementAndGet(); //StatNIO
			NetLog.logDebug(new Exception(Thread.currentThread().getName()+" - Twice close channel."));
		}
		StatNIO.bossClass.close_channel_end.incrementAndGet(); //StatNIO
	}
	
//	/**
//	 * 关闭所有服务channel
//	 */
//	public void closeAllServerChannel() {
//		for(ServerSocketChannel channel : serverChannels) {
//			try {
//				channel.close();
//			} catch (IOException e) {
//				NetLog.logError(e);
//			}
//		}
//		serverChannels = new ArrayList<ServerSocketChannel>();
//	}
//	
	/**
	 * 延迟关闭BOSS线程，停止服务
	 * @param delay 延时关闭的时间（ms）
	 * @throws IOException
	 */
	public void stop(long delay) {
		final AsyncBaseSocket that = this;
		this.unbindAll();
		if(delay <= 0 || that.getSelectionKeyCount() == 0) {
			//Close AsyncBaseSocket（1/2）
			that.runable = false;
			that.selector.wakeup();
		} else {
			final long startTime = System.currentTimeMillis();
			new Thread() {
				@Override
				public void run() {
					while(true) {
						if(that.getSelectionKeyCount() == 0) {
							break;
						}
						long remainTime = System.currentTimeMillis() - startTime;
						if(remainTime <= 0) {
							break;
						}
						try {
							sleep(1);
						} catch (InterruptedException e) {
							NetLog.logDebug(e);
							break;
						}
					}
					//Close AsyncBaseSocket（2/2）
					that.runable = false;
					that.selector.wakeup();
				}
			}.start();;
		}
	}
	
	/**
	 * 立即关闭BOSS线程，停止服务
	 * @throws IOException
	 */
	public void stop() throws IOException {
		stop(0);
	}
	
	/**
	 * 获取 NIO 注册的 KEY 数量
	 */
	public int getSelectionKeyCount() {
		if(selector == null) {
			return 0;
		}
		return selector.keys().size();
	}
	
	/**
	 * 获取 所有的 NetChannel 的数量
	 */
	public int getTotalChannelCount() {
		if(channelPool == null) {
			return 0;
		}
		return channelPool.getTotalChannelCount();
	}
	
	/**
	 * 执行一次BOSS现场的处理
	 * 在epool通知消息处理期间，执行一次SOCKET事件处理
	 */
	private void runOnce() {

		NetChannel channel = null;
		try {
			//侦听队列检查
			ServerSocketChannel serverChannel;
			while((serverChannel = bindQueue.poll()) != null) {
				StatNIO.bossClass.event_register_accept.incrementAndGet(); //StatNIO
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);	//Register the server socket channel to the selector, and accept any connection.						
//				//侦听channel被客户端关闭
//				serverChannels.add(serverChannel);
			}
			//注册队列加入到侦听连接队列
			while((channel = regQueue.poll()) != null) {
				StatNIO.bossClass.event_register_connect.incrementAndGet(); //StatNIO
				channel.socket.register(selector, SelectionKey.OP_CONNECT);
				if(channel.channelRegisterHandle != null) {
					try {
						channel.channelRegisterHandle.doConnect(channel);
					} catch (Exception e) {
						StatNIO.bossClass.event_doconnect_error.incrementAndGet(); //StatNIO
						NetLog.logWarn(e);
						closeSocketChannel(channel.socket);
					}
				} else {
					StatNIO.bossClass.event_no_onconnect.incrementAndGet(); //StatNIO
				}
			}
			//事件更新队列
			EventUpdate eventUpdate;
			while((eventUpdate=eventUpdateQueue.poll()) != null) {
				if(eventUpdate.key.isValid() == false) {
					continue;
				}
				int optValue = eventUpdate.key.interestOps();
				if(eventUpdate.mode == EventUpdate.MODE.OR) {
					optValue |= eventUpdate.optValue;
				} else if(eventUpdate.mode == EventUpdate.MODE.AND) {
					optValue &= eventUpdate.optValue;
				}
				eventUpdate.key.interestOps(optValue);
			}
			//侦听消息
			if(selector.select() == 0) {
				  return;
			}
		} catch (IOException e) {
			StatNIO.bossClass.event_register_error.incrementAndGet(); //StatNIO
			NetLog.logWarn(e);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				NetLog.logInfo(e1);
			}
		}
		//从现有的selector取消息
		Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
		SelectionKey key = null;
		while(iterator.hasNext()) {
			StatNIO.bossClass.event_active_selection.incrementAndGet(); //StatNIO
			key = iterator.next();
			iterator.remove();
			SocketChannel socket = null;
			try {
				//在其它线程中，可能会取消key。先做一下检查
				if(key.isValid() == false) {
					StatNIO.bossClass.event_invalid_selection.incrementAndGet(); //StatNIO
					key.cancel(); //保险起见，重复调用取消key的操作
					continue;
				}
				//Starts to accept any connection from client.
				if(key.isAcceptable()) {
					StatNIO.bossClass.event_acceptable_selection.incrementAndGet(); //StatNIO
					ServerSocketChannel newServerChannel = (ServerSocketChannel)key.channel();
					//外部调用：新连接检查
					if(processor.acceptPrecheck(this, newServerChannel.socket()) == false) {
						StatNIO.bossClass.run_not_accept.incrementAndGet(); //StatNIO
						key.cancel();
						//newServerChannel.close(); //TODO: 优化
						continue;
					}
					socket = newServerChannel.accept();
					socket.configureBlocking(false);
					socket.register(selector, SelectionKey.OP_READ);
					//封装为网络频道
					if(netType == NetType.SSL) {
						channel = new SSLChannel(this, socket);
					} else {
						channel = new NetChannel(this, socket);
					}
					channel.serverSide = true;
					channel._gotoEstablished(); //设置NetChannel状态：ESTABLISHED
					channelPool.register(channel);
					//外部调用：连接成功通知
					processor.onConnect(channel);
				} else {
					//获取已注册的NetChannel对象
					socket = (SocketChannel)key.channel();
					channel = channelPool.get(socket);
					if(channel == null) {
						StatNIO.bossClass.event_unregister_channel.incrementAndGet(); //StatNIO
						closeSocketChannel(socket); //未注册的频道，直接关闭连接
						continue;
					}
					//设置 NetChannel 的最后活跃时间
					channel._setActiveTime();
					//1. 连接事件处理
					if(key.isConnectable()) {
						StatNIO.bossClass.event_connectable_selection.incrementAndGet(); //StatNIO
						if(socket.isConnectionPending()) { //等待完成连接
							StatNIO.bossClass.run_connection_pedding.incrementAndGet(); //StatNIO
							try {
								key.interestOps(SelectionKey.OP_READ);
								if(socket.finishConnect()) {
									StatNIO.bossClass.run_finish_connect.incrementAndGet(); //StatNIO
									//内部调用：连接建立
									channel._gotoEstablished(); //设置NetChannel状态：ESTABLISHED
									//外部调用：连接成功通知
									processor.onConnect(channel);
								} else {
									closeSocketChannel(socket);
								}
							} catch(ConnectException e) {
								channel.lastIOException = e; //设置IOException
								closeSocketChannel(socket);
								continue;
							}
						} else { //连接失败
							closeSocketChannel(socket);
							continue;
						}
					}
					//2. 发送准备完毕事件处理
					if(key.isWritable()) {
						StatNIO.bossClass.event_writable_selection.incrementAndGet(); //StatNIO
						//NetLog.logDebug("isWritable " + socket.socket());
						ByteBuffer buff = null;
						while((buff = channel._getSendBuff()) != null) {
							StatNIO.bossClass.run_write_buff.incrementAndGet(); //StatNIO
							int size = socket.write(buff);
							StatNIO.bossClass.run_write_size.addAndGet(size); //StatNIO
							if(size > 0) {
								channel.socketWriteCount++;
							} else {
								StatNIO.bossClass.run_write_zero.incrementAndGet(); //StatNIO
							}
							if(buff.hasRemaining()) { //一次没写完，等下次发送
								StatNIO.bossClass.run_has_remaining.incrementAndGet(); //StatNIO
								break;
							}
						}
						//则移除写事件的侦听
						//注意：并发注册了写事件的情况下，会丢失写事件。此时需要监控程序重新注册
						if(buff == null) {
							StatNIO.bossClass.run_remove_wriable.incrementAndGet(); //StatNIO
							key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
						}
					}
					//3. 读取事件处理
					if(key.isReadable())	{
						channel.socketReadCount++;
						StatNIO.bossClass.event_readable_selection.incrementAndGet(); //StatNIO
						//NetLog.logDebug("isReadable " + socket.socket());
						//外部调用：接收新消息
						if(socket.isOpen()) {
							processor.onReceive(channel);
						} else {
							StatNIO.bossClass.run_readsocket_closed.incrementAndGet(); //StatNIO
							closeSocketChannel(socket);
							continue;
						}
					}
				}
			} catch(IOException e) {
				StatNIO.bossClass.run_io_exception.incrementAndGet(); //StatNIO
				NetLog.logWarn(e);
				if(socket != null) {
					closeSocketChannel(socket);
				}
			} catch(Exception e) {
				StatNIO.bossClass.run_other_exception.incrementAndGet(); //StatNIO
				NetLog.logError(e);
				if(socket != null) {
					closeSocketChannel(socket);
				}
			}
		}
	}
	
	/**
	 * BOSS线程
	 * @author Hansen
	 *
	 */
	protected class RunThread extends Thread {
		AsyncBaseSocket asyncSocket = null;
		public RunThread(AsyncBaseSocket asyncSocket) {
			this.asyncSocket = asyncSocket;
			this.setDaemon(daemon != 2);
			String name = getThreadName();
			if(name == null) {
				name = "AsyncSocket";
			}
			this.setName(name + getName());
		}
		
		public void run() {
			while(runable) {
				runOnce();
			}
			//结束线程之前，关闭所有channel
			Iterator<SelectionKey> iterator = selector.keys().iterator();
			while(iterator.hasNext()) {
				SelectionKey key = iterator.next();
				try {
					StatNIO.bossClass.boss_selection_close.incrementAndGet(); //StatNIO
					key.channel().close();
				} catch (IOException e) {
					NetLog.logError(e);
				}
			}
		}
		
	}
	
	abstract protected static class ChannelRegisterHandle {
		abstract public void doConnect(NetChannel channel) throws Exception;
	}
	
	static class EventUpdate {
		enum MODE{AND,OR}
		SelectionKey key;
		int optValue;
		MODE mode;
		EventUpdate(SelectionKey key, int optValue, MODE mode) {
			this.key = key;
			this.optValue = optValue;
			this.mode = mode;
		}
	}

	private static class MyAsyncSocketProcessor extends AsyncSocketProcessor {

		@Override
		public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
			StatNIO.bossClass.default_on_accept.incrementAndGet();
			return true;
		}

		@Override
		public void onConnect(NetChannel channel) throws Exception {
			StatNIO.bossClass.default_on_connect.incrementAndGet();
		}

		@Override
		public void onClose(NetChannel channel) throws Exception {
			StatNIO.bossClass.default_on_close.incrementAndGet();
		}

		@Override
		public void onReceive(NetChannel channel) throws Exception {
			StatNIO.bossClass.default_on_receive.incrementAndGet();
		}
	}
}
