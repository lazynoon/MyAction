package net_io.myaction.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

import net_io.core.NetChannel;
import net_io.core.PacketSocket;
import net_io.msg.BaseMsg;
import net_io.myaction.ActionProcessor;
import net_io.myaction.Request;
import net_io.myaction.Response;

public class MyActionSocket extends PacketSocket {
	private HashMap<NetChannel, HashMap<Integer, Reader>> callbacks = new HashMap<NetChannel, HashMap<Integer, Reader>>();
	
	ThreadPoolExecutor producerPool;


	public MyActionSocket(ThreadPoolExecutor producerPool) {
        // 构造一个线程池  
		this.producerPool = producerPool;
		//注册消息ID
		super.msgClass.register(Request.MSG_ID, CommandMsg.class);
		super.msgClass.register(Response.MSG_ID, CommandMsg.class);
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

	public void onConnect(NetChannel channel) throws Exception {
		callbacks.put(channel, new HashMap<Integer, Reader>());
		super.onConnect(channel); //回调上级方法
	}
	public void onClose(NetChannel channel) throws Exception {
		callbacks.remove(channel);
		super.onClose(channel); //回调上级方法
	}

	public void onReceive(final NetChannel channel, final BaseMsg msg) throws Exception {
		int msgID = msg.getMsgID();
		if(msgID == Request.MSG_ID)  { //请求类型
			producerPool.execute(new ActionProcessor(channel, (CommandMsg)msg));
		} else if(msgID == Response.MSG_ID) { //响应数据包
			HashMap<Integer, Reader> readerMap = callbacks.get(channel);
			if(readerMap == null) {
				throw new IOException("The channel is not exist on callbacks pool.");
			}
			Response response = Response.clone(Request.parse((CommandMsg)msg));
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
		
	}
	
	public static interface Reader {
		public long createTime = System.currentTimeMillis();
		public void callback(Response response) ;
	}
	
}
