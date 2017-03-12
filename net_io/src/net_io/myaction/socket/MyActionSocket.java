package net_io.myaction.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

import net_io.core.AsyncBaseSocket;
import net_io.core.AsyncSocketProcessor;
import net_io.core.NetChannel;
import net_io.core.PacketSocket;
import net_io.msg.BaseMsg;
import net_io.myaction.Response;
import net_io.myaction.server.CommandMsg;
import net_io.utils.thread.MyThreadPool;

public class MyActionSocket extends PacketSocket {
	private HashMap<NetChannel, HashMap<Integer, Reader>> callbacks = new HashMap<NetChannel, HashMap<Integer, Reader>>();
	
	private MyThreadPool producerPool = null;
	private AsyncSocketProcessor eventProcessor = null;


	public MyActionSocket(MyThreadPool producerPool) {
		init(producerPool, null);
	}
	public MyActionSocket(MyThreadPool producerPool, AsyncSocketProcessor eventProcessor) {
        init(producerPool, eventProcessor);
	}

	private void init(MyThreadPool producerPool, AsyncSocketProcessor eventProcessor) {
        // 构造一个线程池  
		this.producerPool = producerPool;
		this.eventProcessor = eventProcessor;
		//注册消息ID
		super.msgClass.register(SocketRequest.MSG_ID, CommandMsg.class);
		super.msgClass.register(SocketResponse.MSG_ID, CommandMsg.class);

	}
	public void setReader(NetChannel channel, int requestID, Reader reader) throws IOException {
		if(reader == null) {
			return;
		}
		HashMap<Integer, Reader> readerMap = callbacks.get(channel);
		if(readerMap == null) {
			throw new IOException("The channel is not exist on callbacks pool.");
		}
		Integer i = Integer.valueOf(requestID);
		readerMap.put(i, reader);
	}

	@Override
	public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
		if(eventProcessor != null) {
			return eventProcessor.acceptPrecheck(that, socket);
		}
		return true;
	}

	public void onConnect(NetChannel channel) throws Exception {
		callbacks.put(channel, new HashMap<Integer, Reader>());
		super.onConnect(channel); //回调上级方法
		if(eventProcessor != null) {
			eventProcessor.onConnect(channel);
		}
	}
	public void onClose(NetChannel channel) throws Exception {
		callbacks.remove(channel);
		super.onClose(channel); //回调上级方法
		if(eventProcessor != null) {
			eventProcessor.onClose(channel);
		}
	}

	public void onReceive(final NetChannel channel, final BaseMsg msg) throws Exception {
		int msgID = msg.getMsgID();
		if(msgID == SocketRequest.MSG_ID)  { //请求类型
			producerPool.execute(new SocketActionProcessor(channel, (CommandMsg)msg));
		} else if(msgID == SocketResponse.MSG_ID) { //响应数据包
			HashMap<Integer, Reader> readerMap = callbacks.get(channel);
			if(readerMap == null) {
				throw new IOException("The channel is not exist on callbacks pool.");
			}
			SocketResponse response = SocketResponse.clone(SocketRequest.parse((CommandMsg)msg));
			int reqID = response.getRequestID();
			Reader reader = readerMap.get(reqID);
			if(reader != null) {
				reader.callback(response);
			} else {
				channel.putResponse(reqID, msg);
			}
		} else {
			throw new Exception("[MyActionSocket] unknown MsgID: "+msgID);
		}
		if(eventProcessor != null) {
			eventProcessor.onReceive(channel);
		}		
	}
	
	public static interface Reader {
		public long createTime = System.currentTimeMillis();
		public void callback(Response response) ;
	}
	
}
