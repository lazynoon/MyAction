package net_io.core;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import net_io.utils.NetLog;


abstract public class AsyncBaseSocket {
	protected Selector selector;
	AsyncSocketProcessor processor = null;
	private String name = null; //Thread Name
	protected RunThread run = null;
	private boolean runable = true;
	
	//是否作为守护线程运行。默认为：0，表示可自动根据端类型设置。ClientSide默认设置为1（即：守护线程），ServerSide默认为2（即：用户线程） 
	private int daemon = 0; 
	protected ConcurrentLinkedQueue<NetChannel> regQueue = new ConcurrentLinkedQueue<NetChannel>();
	protected ConcurrentLinkedQueue<ServerSocketChannel> bindQueue = new ConcurrentLinkedQueue<ServerSocketChannel>();
	protected NetChannelPool channelPool = null;
	final public static int DEFAULT_BACKLOG = 1024;
	// 绑定的端口
	private ArrayList<InetSocketAddress> bindAddressList = null;
	private ArrayList<ServerSocketChannel> serverChannels = new ArrayList<ServerSocketChannel>();

	public AsyncBaseSocket(AsyncSocketProcessor processor) {
		this.processor = processor;
		this.channelPool = new NetChannelPool(1024*1024);
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
	

	public void bind(int port) throws IOException {
		bind(new InetSocketAddress(port));
	}
	public void bind(InetSocketAddress address) throws IOException {
		bind(address, DEFAULT_BACKLOG);
	}
	public void bind(InetSocketAddress address, int backlog) throws IOException {
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
		
		serverChannel.configureBlocking(false);
		
		
		bindQueue.add(serverChannel);
		
		//保存已绑定的地址
		if(bindAddressList == null) {
			bindAddressList = new ArrayList<InetSocketAddress>();
		}
		bindAddressList.add(address);
		
		//启动线程
		if(needStartThread) {
			run.start();
		} else {
			selector.wakeup();
		}
	}
	
	/**
	 * 发起连接
	 * @param host
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public NetChannel connect(String host, int port) throws Exception {
		InetSocketAddress remote = new InetSocketAddress(host, port);
		return connect(remote);
	}
	
	/**
	 * 发起连接
	 * @param remote
	 * @return
	 * @throws Exception
	 */
	public NetChannel connect(final InetSocketAddress remote) throws Exception {
		//检查并启动BOSS线程（含selector初始化）
		boolean needStartThread = false;
		if(run == null) {
			needStartThread = initBossThread();
		}
		//连接初始化
		SocketChannel socket = SocketChannel.open();
		socket.configureBlocking(false);
		//封装为网络频道
		NetChannel channel = new NetChannel(this, socket);
		channel._gotoConnect(); //设置NetChannel状态：CONNECT
		channelPool.register(channel);
		final AsyncBaseSocket asyncSocket = this;

		//注册到回调事件中
		channel.channelRegisterHandle = new ChannelRegisterHandle() {

			@Override
			public void doConnect(NetChannel channel) throws Exception {
				boolean ret = channel.socket.connect(remote);
				if(ret) { //连接立即成功
					channel._gotoEstablished(); //设置NetChannel状态：ESTABLISHED
					//增加监控“读”事件
					SelectionKey key = channel.socket.keyFor(asyncSocket.selector);
					key.interestOps(key.interestOps() | SelectionKey.OP_READ);
					//回调连接成功消息
					asyncSocket.processor.onConnect(channel);
				}
			}
			
		};
		
		//注册epoll事件
		regQueue.offer(channel);
		
		if(needStartThread) {
			run.start();
		} else {
			selector.wakeup();
		}
		return channel;
	}
	
	public void send(NetChannel channel, ByteBuffer buff) throws IOException {
		buff.rewind();
		ByteBuffer newBuff = buff.slice();
		channel._send(newBuff);
	}
	
	protected void sendDirect(NetChannel channel, ByteBuffer buff) throws IOException {
		channel._send(buff);
	}
	
	
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
		//boolean firstClose = socket.isOpen();
		//NetLog.logDebug(new Exception("closeSocketChannel: "+socket));
		NetLog.logDebug("closeSocketChannel: "+socket);
		try {
			socket.socket().close();
			socket.close();
		} catch (Exception e) {
			NetLog.logWarn("[SocketCloseError] "+Thread.currentThread().getName());
			NetLog.logWarn(e);
		}
		SelectionKey key = socket.keyFor(selector);
		if(key != null) {
			key.cancel();
		} else {
			NetLog.logDebug("Cat not find SelectionKey. Socket: "+socket);
		}
		NetChannel channel = channelPool.get(socket);
		if(channel != null) {
			try {
				channel._gotoCloseWait(); //设置NetChannel状态：CLOSE_WAIT
				processor.onClose(channel);
			} catch (Exception e) {
				NetLog.logWarn("[AppCloseError] "+Thread.currentThread().getName());
				NetLog.logWarn(e);
			} finally {
				channel.closed(); //连接关闭了，由Channel，取消注册的事件
			}
		} else {
			NetLog.logDebug(new Exception(Thread.currentThread().getName()+" - Twice close channel."));
		}
	}
	
	/**
	 * 关闭所有服务channel
	 */
	public void closeAllServerChannel() {
		for(ServerSocketChannel channel : serverChannels) {
			try {
				channel.close();
			} catch (IOException e) {
				NetLog.logError(e);
			}
		}
		serverChannels = new ArrayList<ServerSocketChannel>();
	}
	
//	/**
//	 * 关闭BOSS线程，停止服务
//	 * @param delay 延时N秒
//	 * @throws IOException
//	 */
//	public void stop(int delay) throws IOException {
//		
//	}
//	
	/**
	 * 关闭BOSS线程，停止服务
	 * @throws IOException
	 */
	public void stop() throws IOException {
		this.closeAllServerChannel();
		runable = false;
		this.selector.wakeup();
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
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);	//Register the server socket channel to the selector, and accept any connection.						
				//侦听channel被客户端关闭
				serverChannels.add(serverChannel);
			}
			//注册队列加入到侦听连接队列
			while((channel = regQueue.poll()) != null) {
				channel.socket.register(selector, SelectionKey.OP_CONNECT);
				if(channel.channelRegisterHandle != null) {
					try {
						channel.channelRegisterHandle.doConnect(channel);
					} catch (Exception e) {
						NetLog.logWarn(e);
						closeSocketChannel(channel.socket);
					}
				}
			}
			//侦听消息
			if(selector.select() == 0) {
				  return;
			}
		} catch (IOException e) {
			NetLog.logWarn(e);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				NetLog.logInfo(e1);
			}
		}
		//从现有的selector取消息
		Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
		SelectionKey key = null;
		while(iterator.hasNext()) {
			key = iterator.next();
			iterator.remove();
			SocketChannel socket = null;
			try {
				//在其它线程中，可能会取消key。先做一下检查
				if(key.isValid() == false) {
					key.cancel(); //保险起见，重复调用取消key的操作
					continue;
				}
				//Starts to accept any connection from client.
				if(key.isAcceptable()) {
					ServerSocketChannel newServerChannel = (ServerSocketChannel)key.channel();
					//外部调用：新连接检查
					if(processor.acceptPrecheck(newServerChannel.socket()) == false) {
						continue;
					}
					socket = newServerChannel.accept();
					socket.configureBlocking(false);
					socket.register(selector, SelectionKey.OP_READ);
					//封装为网络频道
					//channel = new NetChannel(this.asyncSocket, socket);
					channel = new NetChannel(this, socket);
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
						closeSocketChannel(socket); //未注册的频道，直接关闭连接
						continue;
					}
					//设置 NetChannel 的最后活跃时间
					channel.lastAliveTime = System.currentTimeMillis();
					//1. 连接事件处理
					if(key.isConnectable()) {
						if(socket.isConnectionPending()) { //等待完成连接
							try {
								key.interestOps(SelectionKey.OP_READ);
								if(socket.finishConnect()) {
									//内部调用：连接建立
									channel._gotoEstablished(); //设置NetChannel状态：ESTABLISHED
									//外部调用：连接成功通知
									processor.onConnect(channel);
								} else {
									closeSocketChannel(socket);
								}
							} catch(ConnectException e) {
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
						NetLog.logDebug("isWritable " + socket.socket());
						ByteBuffer buff = null;
						while((buff = channel._getSendBuff()) != null) {
							socket.write(buff);
							if(buff.hasRemaining()) { //一次没写完，等下次发送
								break;
							}
						}
						//则移除写事件的侦听
						//注意：并发注册了写事件的情况下，会丢失写事件。此时需要监控程序重新注册
						if(buff == null) {
							key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
						}
					}
					//3. 读取事件处理
					if(key.isReadable())	{
						NetLog.logDebug("isReadable " + socket.socket());
						//外部调用：接收新消息
						if(socket.isOpen()) {
							processor.onReceive(channel);
						} else {
							closeSocketChannel(socket);
							continue;
						}
					}
				}
			} catch(IOException e) {
				NetLog.logWarn(e);
				if(socket != null) {
					closeSocketChannel(socket);
				}
			} catch(Exception e) {
				NetLog.logDebug(e);
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

}
