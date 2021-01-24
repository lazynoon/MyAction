package net_io.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;


public class StreamSocket extends AsyncBaseSocket {

	protected StreamSocket() {}

	public StreamSocket(SocketEventHandle handle) {
		super(new MyAsyncSocketProcessor(handle));
		StatNIO.streamStat.create_stream_socket.incrementAndGet(); //StatNIO
	}

	protected void init(SocketEventHandle handle) {
		super.init(new MyAsyncSocketProcessor(handle));
	}


	private static class MyAsyncSocketProcessor extends AsyncSocketProcessor {
		private SocketEventHandle handle = null;
		private MyAsyncSocketProcessor(SocketEventHandle handle) {
			this.handle = handle;
		}

		@Override
		public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
			StatNIO.streamStat.call_on_accept.incrementAndGet(); //StatNIO
			InetAddress address = socket.getInetAddress();
			if(address == null) {
				StatNIO.streamStat.accept_error_no_address.incrementAndGet(); //StatNIO
				return false;
			}
			return handle.onAccept(new InetSocketAddress(address, socket.getLocalPort()));
		}

		@Override
		public void onConnect(NetChannel channel) throws Exception {
			StatNIO.streamStat.call_on_connect.incrementAndGet(); //StatNIO
			handle.onConnect(channel);
		}

		@Override
		public void onReceive(NetChannel channel) throws Exception {
			StatNIO.streamStat.call_on_receive.incrementAndGet(); //StatNIO
			ByteBuffer buff = ByteBufferPool.mallocMiddle();
			try {
				int size = channel.socket.read(buff);
				if(size < 0) { //连接关闭请求
					channel.close();
					return; //本轮socket中的数据取完了
				}
				buff.flip();
				//buff.rewind();
				StatNIO.streamStat.receive_size.addAndGet(buff.limit()); //StatNIO
				ByteArray data = new ByteArray(buff.limit());
				data.append(buff);
				data.finishWrite();
				//触发接收事件
				StatNIO.streamStat.default_on_receive.incrementAndGet(); //StatNIO
				handle.onReceive(channel, data);
			} finally {
				//释放 recv buffer
				ByteBufferPool.free(buff);
			}
		}

		@Override
		public void onClose(NetChannel channel) throws Exception {
			StatNIO.streamStat.call_on_close.incrementAndGet(); //StatNIO
			handle.onClose(channel);
		}

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

}
