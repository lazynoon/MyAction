package net_io.core.ssl;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import net_io.core.AsyncBaseSocket;
import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.core.SocketEventHandle;
import net_io.core.StreamSocket;
import net_io.utils.EncodeUtils;

public class SSLSocketServer extends StreamSocket {
	public SSLSocketServer(SocketEventHandle handle) {
		super(handle);
		super.setNetType(AsyncBaseSocket.NetType.SSL);
	}

	private static class MySocketEventHandle implements SocketEventHandle {
		private SocketEventHandle handle;
		private MySocketEventHandle(SocketEventHandle handle) {
			this.handle = handle;
		}

		@Override
		public boolean onAccept(InetSocketAddress address) throws Exception {
			return handle.onAccept(address);
		}

		@Override
		public void onConnect(NetChannel channel) throws Exception {
			handle.onConnect(channel);
		}

		@Override
		public void onReceive(NetChannel channel, ByteArray data) throws Exception {
			SSLChannel sslChannel = (SSLChannel) channel;
			SSLSocketEngine engine = sslChannel.getSSLSocketEngine();
			if(engine.isHandshakeFinish()) {
				ByteBuffer newBuff = engine.decrypt(data.getByteBuffer());
				//加密后内容调用上级类方法
				//System.out.println("receive buff size: " + newBuff.limit());
				handle.onReceive(sslChannel, new ByteArray(newBuff));
			} else {
				if(engine.isFirstPacket()) {
					SSLClientHello clientHello = new SSLClientHello(data);
					SSLContext context = SSLContextFactory.get(clientHello.getHostName());
					SSLEngine sslEngine = context.createSSLEngine();
					sslChannel.configSSLSocketEngine(clientHello, sslEngine);
					System.err.println("---------------------");
					System.err.println("fingerprint: "+clientHello.getFingerprint());
					System.err.println("session id: "+EncodeUtils.bin2hex(clientHello.getSessionId()));
					System.err.println("random: "+EncodeUtils.bin2hex(clientHello.getRandom()));
					System.err.println("---------------------");
				}
				ByteBuffer peerAppData = ByteBufferPool.malloc64K();
				try {
					engine.doHandshake(sslChannel, data.getByteBuffer(), peerAppData);
				} finally {
					ByteBufferPool.free(peerAppData);
				}
			}
		}

		@Override
		public void onClose(NetChannel channel) throws Exception {
			handle.onClose(channel);
		}
	}
	
//	@Override
//	public void send(NetChannel channel, ByteArray data) throws IOException {
//		send(channel, data.getByteBuffer());
//	}
//	
//	@Override
//	public void send(NetChannel channel, ByteBuffer data) throws IOException {
//	}
//

	
	
}
