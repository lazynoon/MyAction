package net_io.myaction;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.server.MyActionSocket;

public class MyActionClient {
	private MyActionSocket actionSocket = null;
	private NetChannel channel = null;
	private int defaultTimeout = 30000; //单位：ms
	
	public MyActionClient() {
		ThreadPoolExecutor producerPool = new ThreadPoolExecutor(2, 16, 60,  
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),  
                new ThreadPoolExecutor.DiscardOldestPolicy());
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
	public void connect(String host, int port) throws Exception {
		channel = this.actionSocket.connect(host, port);
	}
	
	/**
	 * 异步提交
	 * @param request
	 * @throws IOException
	 */
	public void anyncPost(Request request) throws IOException {
		post(request, -1);
	}
	/**
	 * 接受异步提交的结果
	 */
	public Response anyncResponse(Request request) {
		return anyncResponse(request, 0);
	}
	
	public Response anyncResponse(Request request, int waitTime) {
		return (Response)channel.fetchResponse(request.getRequestID(), waitTime);
	}
	
	public void anyncPost(Request request, MyActionSocket.Reader reader) throws IOException {
		_post(request, reader, -1);
	}
	
	public Response post(Request request) throws IOException {
		return post(request, defaultTimeout);
	}
	public Response post(Request request, int timeout) throws IOException {
		return _post(request, null, timeout);
	}
	private Response _post(Request request, MyActionSocket.Reader reader, int timeout) throws IOException {
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
				CommandMsg responseMsg = (CommandMsg)channel.fetchResponse(requestID, timeout);
				if(responseMsg == null) {
					throw new IOException("receive timeout!");
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
	
	public void sendResponse(Response response) throws IOException {
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
