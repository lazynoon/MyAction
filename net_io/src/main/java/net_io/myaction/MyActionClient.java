package net_io.myaction;

import java.io.IOException;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.core.NetChannel.Status;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.socket.MyActionSocket.Reader;
import net_io.myaction.socket.SocketRequest;
import net_io.utils.Mixed;
import net_io.utils.thread.MyThreadPool;

public class MyActionClient {
	/** 默认超时时间（30s） **/
	private static final long DEFAULT_TIMEOUT = 30 * 1000;
	private MyActionSocket asyncSocket = null;
	private NetChannel channel = null;
	
	@Deprecated
	public MyActionClient() {
		MyThreadPool producerPool = new MyThreadPool(8, 64);
		this.asyncSocket = new MyActionSocket(producerPool);
	}

	public MyActionClient(MyActionSocket asyncSocket) {
//		MyThreadPool producerPool = new MyThreadPool(8, 64);
//		this.actionSocket = new MyActionSocket(producerPool);
		this.asyncSocket = asyncSocket;
	}

	public MyActionClient(MyActionSocket asyncSocket, NetChannel channel) {
		this.asyncSocket = asyncSocket;
		this.channel = channel;
	}

	public NetChannel getNetChannel() {
		return channel;
	}
	
	public void connect(String host, int port) throws IOException {
		connect(host, port, DEFAULT_TIMEOUT);
	}
	
	public void connect(String host, int port, long timeout) throws IOException {
		synchronized(this) {
			//关闭旧连接
			if(channel != null && (channel.getChannelStatus() == Status.CONNECT || channel.getChannelStatus() == Status.ESTABLISHED)) {
				channel.close();
			}
			//创建新连接
			channel = this.asyncSocket.connect(host, port);
			//等待连接完成
			if(channel.waitConnect(timeout) == false) { //连接超时
				channel.close(); //关闭连接
				throw new IOException("Connect timeout.");
			}
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
	
	public int sendRequest(String path, Mixed data) throws IOException {
		return sendRequest(path, data, null, null);
	}
	public int sendRequest(String path, Mixed data, byte[] attachment) throws IOException {
		return sendRequest(path, data, attachment, null);
	}
	public int sendRequest(String path, Mixed data, Reader reader) throws IOException {
		return sendRequest(path, data, null, reader);
	}
	public int sendRequest(String path, Mixed data, byte[] attachment, Reader reader) throws IOException {
		int requestID = channel.generateSequenceID();
		CommandMsg msg = new CommandMsg(SocketRequest.MSG_ID);
		msg.setRequestID(requestID);
		msg.setPath(path);
		msg.resetData(data);
		msg.setAttachment(attachment);
		ByteArray sendBuff = new ByteArray();
		try {
			msg.writeData(sendBuff);
			msg.finishWrite(sendBuff);
			//注册回调函数
			if(reader != null) {
				asyncSocket.setReader(channel, requestID, reader);
			}
			//发送消息
			channel.send(sendBuff.getByteBuffer());
		} catch(IOException e) {
			channel.close();
			throw e;
		}
		return requestID;
	}
	
	public CommandMsg readResponse(int requestID) throws IOException {
		return (CommandMsg) channel.fetchResponse(requestID, DEFAULT_TIMEOUT);
	}
	
	public CommandMsg readResponse(int requestID, long timeout) throws IOException {
		return (CommandMsg) channel.fetchResponse(requestID, timeout);
	}
	
	public Mixed readResult(int requestID) throws IOException {
		CommandMsg msg  = readResponse(requestID, 0);
		if(msg == null) {
			return null;
		}
		return msg.getData();
	}
	
	public Mixed readResult(int requestID, long timeout) throws IOException {
		CommandMsg msg  = readResponse(requestID, timeout);
		if(msg == null) {
			return null;
		}
		return msg.getData();
	}
	
	public void close() {
		channel.close();
	}
	
	@Deprecated
	public Response post(SocketRequest request) throws IOException {
		return _post(request, null, DEFAULT_TIMEOUT);
	}
	@Deprecated
	public Response post(SocketRequest request, long timeout) throws IOException {
		return _post(request, null, timeout);
	}
	private Response _post(SocketRequest request, MyActionSocket.Reader reader, long timeout) throws IOException {
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
				asyncSocket.setReader(channel, requestID, reader);
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
	

}
