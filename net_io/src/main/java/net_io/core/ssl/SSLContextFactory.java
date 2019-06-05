package net_io.core.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SSLContextFactory {
	private static Map<String, SSLContext> contextPool = new ConcurrentHashMap<String, SSLContext>();
	public static SSLContext get(String host) throws Exception {
		SSLContext context = contextPool.get(host);
		if(context != null) {
			return context;
		}
		char[] password = "111111".toCharArray();
		KeyStore keyStore = KeyStore.getInstance("JKS");
		//InputStream in = SSLHandshakeServer.class.getResourceAsStream("serverkeystore");
		//simple.demo.ssl.both.SSLHandshakeServer
		InputStream in = SSLContextFactory.class.getResourceAsStream("tomcat.keystore");
		if(in == null) {
			throw new NullPointerException("keystore file is null");
		}
		keyStore.load(in, password);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore, password);

		context = SSLContext.getInstance("SSL");
		context.init(kmf.getKeyManagers(), null, null);
		contextPool.put(host, context);
		return context;
	}
}
