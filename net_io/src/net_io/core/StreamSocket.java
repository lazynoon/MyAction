package net_io.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;


public class StreamSocket extends AsyncBaseSocket {
	final public static int HEAD_SIZE = 8;
	private ByteArray headBuff = new ByteArray(HEAD_SIZE);

	public StreamSocket() {
		super(initProcessor());
	}
	
	private static AsyncSocketProcessor initProcessor() {
		return new AsyncSocketProcessor() {

			@Override
			public void onConnect(NetChannel channel) throws Exception {
				((StreamSocket)channel.asyncSocket).onConnect(channel);
			}

			@Override
			public void onReceive(NetChannel channel) throws Exception {
				StreamSocket that = ((StreamSocket)channel.asyncSocket);
				ByteBuffer buff = ByteBufferPool.malloc(8192);
				int size = channel.socket.read(buff);
				if(size < 0) { //连接关闭请求
					that.closeChannel(channel);
					return; //本轮socket中的数据取完了
				}
				buff.flip();
				//buff.rewind();
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
				((StreamSocket)channel.asyncSocket).onClose(channel);
			}
			
		};
	}
	
	public void send(NetChannel channel, ByteArray data) throws IOException {
		channel._send(data.getByteBuffer());
	}

	public boolean onAccept(ServerSocket socket) throws Exception {
		return true;
	}
	
	public void onConnect(NetChannel channel) throws Exception {
		
	}
	public void onClose(NetChannel channel) throws Exception {
		;
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		;
	}
}
