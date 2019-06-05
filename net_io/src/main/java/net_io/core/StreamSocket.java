package net_io.core;

import java.net.ServerSocket;
import java.nio.ByteBuffer;


public class StreamSocket extends AsyncBaseSocket {
	private SocketEventHandle handle = null;

	public StreamSocket() {
		super(initProcessor());
		StatNIO.streamStat.create_stream_socket.incrementAndGet(); //StatNIO
	}
	
	public StreamSocket(SocketEventHandle handle) {
		super(initProcessor());
		this.handle = handle;
		StatNIO.streamStat.create_stream_socket.incrementAndGet(); //StatNIO
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
				StatNIO.streamStat.call_on_connect.incrementAndGet(); //StatNIO				
				((StreamSocket)channel.asyncSocket).onConnect(channel);
			}

			@Override
			public void onReceive(NetChannel channel) throws Exception {
				StatNIO.streamStat.call_on_receive.incrementAndGet(); //StatNIO				
				StreamSocket that = ((StreamSocket)channel.asyncSocket);
				ByteBuffer buff = ByteBufferPool.mallocMiddle();
				try {
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
					//触发接收事件
					that.onReceive(channel, data);
				} finally {
					//释放 recv buffer
					ByteBufferPool.free(buff);					
				}
			}

			@Override
			public void onClose(NetChannel channel) throws Exception {
				StatNIO.streamStat.call_on_close.incrementAndGet(); //StatNIO
				((StreamSocket)channel.asyncSocket).onClose(channel);
			}
			
		};
	}
	
//	public void send(NetChannel channel, ByteArray data) throws IOException {
//		StatNIO.streamStat.send_invoke.incrementAndGet(); //StatNIO
//		StatNIO.streamStat.send_size.addAndGet(data.size()); //StatNIO
//		channel._send(data.getByteBuffer());
//	}
//
//	public void send(NetChannel channel, ByteBuffer data) throws IOException {
//		StatNIO.streamStat.send_invoke.incrementAndGet(); //StatNIO
//		StatNIO.streamStat.send_size.addAndGet(data.limit()); //StatNIO
//		channel._send(data);
//	}
//	
	public boolean onAccept(ServerSocket socket) throws Exception {
		StatNIO.streamStat.default_on_accept.incrementAndGet(); //StatNIO
		return true;
	}
	
	public void onConnect(NetChannel channel) throws Exception {
		StatNIO.streamStat.default_on_connect.incrementAndGet(); //StatNIO
		if(handle != null) {
			handle.onConnect(channel);
		}
	}
	public void onClose(NetChannel channel) throws Exception {
		StatNIO.streamStat.default_on_close.incrementAndGet(); //StatNIO
		if(handle != null) {
			handle.onClose(channel);
		}
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		StatNIO.streamStat.default_on_receive.incrementAndGet(); //StatNIO
		if(handle != null) {
			handle.onReceive(channel, data);
		}
	}
}
