package net_io.core.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import net_io.core.AsyncBaseSocket;
import net_io.core.NetChannel;

public class SSLChannel extends NetChannel {
	private SSLSocketEngine engine = new SSLSocketEngine();
	private SSLClientHello clientHello = null;
	public SSLChannel(AsyncBaseSocket asyncSocket, SocketChannel socketChannel) {
		super(asyncSocket, socketChannel);
	}

	@Override
	public void send(ByteBuffer buff) throws IOException {
		if(engine == null || !engine.isHandshakeFinish()) {
			throw new IOException("handshake has not finished");
		}
		buff = engine.encrypt(buff);
		super.send(buff);
	}
	
	protected void sendHandshakeData(ByteBuffer buff) throws IOException {
		super.send(buff);
	}
	
	protected SSLSocketEngine getSSLSocketEngine() {
		return engine;
	}
	
	protected void configSSLSocketEngine(SSLClientHello clientHello, SSLEngine sslEngine) {
		sslEngine.setUseClientMode(false);
		engine.sslEngine = sslEngine;
	}

	public SSLClientHello getClientHello() {
		return clientHello;
	}
}
