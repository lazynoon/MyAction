package net_io.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import net_io.utils.NetLog;


public class StreamSocket extends AsyncBaseSocket {
	final public static int HEAD_SIZE = 8;
	//private ByteArray headBuff = new ByteArray(HEAD_SIZE);

	public StreamSocket() {
		super(initProcessor());
		StatNIO.streamStat.create_stream_socket.getAndIncrement(); //StatNIO
	}
	
	private static AsyncSocketProcessor initProcessor() {
		return new AsyncSocketProcessor() {
			
			@Override
			public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
				//NetLog.logInfo("acceptPrecheck: "+socket);
				return true;
			}

			@Override
			public void onConnect(NetChannel channel) throws Exception {
				StatNIO.streamStat.call_on_connect.getAndIncrement(); //StatNIO				
				((StreamSocket)channel.asyncSocket).onConnect(channel);
			}

			@Override
			public void onReceive(NetChannel channel) throws Exception {
				StatNIO.streamStat.call_on_receive.getAndIncrement(); //StatNIO				
				StreamSocket that = ((StreamSocket)channel.asyncSocket);
				ByteBuffer buff = ByteBufferPool.malloc(8192);
				int size = channel.socket.read(buff);
				if(size < 0) { //连接关闭请求
					that.closeChannel(channel);
					return; //本轮socket中的数据取完了
				}
				buff.flip();
				//buff.rewind();
				StatNIO.streamStat.receive_size.addAndGet(buff.limit()); //StatNIO				
				ByteArray data = new ByteArray(buff.limit());
				data.append(buff);
				data.finishWrite();
				//释放 recv buffer
				ByteBufferPool.free(buff);
				//触发接收事件
				that.onReceive(channel, data);
			}

			@Override
			public void onClose(NetChannel channel) throws Exception {
				StatNIO.streamStat.call_on_close.getAndIncrement(); //StatNIO
				((StreamSocket)channel.asyncSocket).onClose(channel);
			}
			
		};
	}
	
	public void send(NetChannel channel, ByteArray data) throws IOException {
		StatNIO.streamStat.send_invoke.getAndIncrement(); //StatNIO
		StatNIO.streamStat.send_size.addAndGet(data.size()); //StatNIO
		channel._send(data.getByteBuffer());
	}

	public boolean onAccept(ServerSocket socket) throws Exception {
		StatNIO.streamStat.default_on_accept.getAndIncrement(); //StatNIO
		return true;
	}
	
	public void onConnect(NetChannel channel) throws Exception {
		StatNIO.streamStat.default_on_connect.getAndIncrement(); //StatNIO
	}
	public void onClose(NetChannel channel) throws Exception {
		StatNIO.streamStat.default_on_close.getAndIncrement(); //StatNIO
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		StatNIO.streamStat.default_on_receive.getAndIncrement(); //StatNIO
	}
}
