package net_io.myaction;

import java.io.IOException;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.core.NetChannel.Status;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.socket.SocketRequest;
import net_io.myaction.socket.SocketResponse;
import net_io.utils.thread.MyThreadPool;

public class MyActionClient {
	private MyActionSocket actionSocket = null;
	private NetChannel channel = null;
	private int defaultTimeout = 30000; //单位：ms
	
	public MyActionClient() {
		MyThreadPool producerPool = new MyThreadPool(8, 64);
		this.actionSocket = new MyActionSocket(producerPool);
	}
	
	public MyActionClient(NetChannel channel) {
		this.actionSocket = (MyActionSocket)channel.getBaseSocket();
		this.channel = channel;
	}
	
	public void setDefaultTimeout(int timeout) {
		this.defaultTimeout = timeout;
	}
	
	public int getDefaultTimeout(int timeout) {
		return this.defaultTimeout;
	}
	public int getSendQueueSize() {
		if(channel == null) {
			return 0;
		}
		return channel.getSendQueueSize();
	}
	public void connect(String host, int port) throws IOException {
		connect(host, port, defaultTimeout);
	}
	
	synchronized public void connect(String host, int port, long timeout) throws IOException {
		//关闭旧连接
		if(channel != null && (channel.getChannelStatus() == Status.CONNECT || channel.getChannelStatus() == Status.ESTABLISHED)) {
			channel.close();
		}
		//创建新连接
		channel = this.actionSocket.connect(host, port);
		//等待连接完成
		if(channel.waitConnect(timeout) == false) { //连接超时
			channel.close(); //关闭连接
			channel = null;
		}
	}
	
	/**
	 * 检查是否需要连接
	 * @return true需要重新连接，false不需要
	 */
	public boolean needConnect() {
		if(channel == null || (channel.getChannelStatus() != Status.CONNECT && channel.getChannelStatus() != Status.ESTABLISHED)) {
			return true;
		} else {
			return false;
		}
	}
	
	public NetChannel getNetChannel() {
		return channel;
	}
	
	/**
	 * 异步提交
	 * @param request
	 * @throws IOException
	 */
	public void anyncPost(SocketRequest request) throws IOException {
		post(request, -1);
	}
	/**
	 * 接受异步提交的结果
	 */
	public Response anyncResponse(SocketRequest request) {
		return anyncResponse(request, 0);
	}
	
	public Response anyncResponse(SocketRequest request, int waitTime) {
		return (Response)channel.fetchResponse(request.getRequestID(), waitTime);
	}
	
	public void anyncPost(SocketRequest request, MyActionSocket.Reader reader) throws IOException {
		_post(request, reader, -1);
	}
	
	public Response post(SocketRequest request) throws IOException {
		return _post(request, null, defaultTimeout);
	}
	public Response post(SocketRequest request, int timeout) throws IOException {
		return _post(request, null, timeout);
	}
	private Response _post(SocketRequest request, MyActionSocket.Reader reader, int timeout) throws IOException {
		int requestID = channel.generateSequenceID();
		CommandMsg msg = request.generateCommandMsg();
		msg.setRequestID(requestID);
		//组装返回消息
		ByteArray sendBuff = new ByteArray(ByteBufferPool.malloc(ByteBufferPool.MAX_BUFFER_SIZE));
		try {
			msg.writeData(sendBuff);
			msg.finishWrite(sendBuff);
			//注册回调函数
			if(reader != null) {
				actionSocket.setReader(channel, requestID, reader);
			}
			//发送消息
			channel.send(sendBuff.getByteBuffer());
			//接收消息
			Response response = null;
			if(reader == null && timeout >= 0) { //reader等于null, 采用回调方式返回
				long startTime = System.currentTimeMillis();
				CommandMsg responseMsg = (CommandMsg)channel.fetchResponse(requestID, timeout);
				long costTime = System.currentTimeMillis() - startTime;
				if(responseMsg == null) {
					if(costTime>= timeout) {
						throw new IOException("receive timeout!");
					} else {
						CommandMsg smsg = new CommandMsg();
						response = Response.parse(smsg);
						response.setError(580, "connect is closed.");
						return response;
					}
				}
				response = Response.parse(responseMsg);
			}
			return response;
		} catch(IOException e) {
			channel.close();
			throw e;
		} finally {
			ByteBufferPool.free(sendBuff.getByteBuffer()); //发送消息后，立即回收缓存区
			sendBuff = null;
		}
		
	}
	
	public void sendResponse(SocketResponse response) throws IOException {
		//组装返回消息
		ByteArray sendBuff = new ByteArray(ByteBufferPool.malloc(ByteBufferPool.MAX_BUFFER_SIZE));
		try {
			response.writeSendBuff(sendBuff);
			//发送消息
			channel.send(sendBuff.getByteBuffer());
		} finally {
			ByteBufferPool.free(sendBuff.getByteBuffer()); //发送消息后，立即回收缓存区
			sendBuff = null;
		}
		
	}
	
}
