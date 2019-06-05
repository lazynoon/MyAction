package net_io.myaction.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executor;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

import net_io.utils.NetLog;
import net_io.utils.thread.MyThreadPool;

public class MyHttpsServer {
	private HttpsServer httpserver = null;
	// public static int concurrent = 256; // 并发量
	private static int backlog = 8196; // 最大等待连接数
	MyThreadPool producerPool;

	public MyHttpsServer(MyThreadPool producerPool) {
		this.producerPool = producerPool;
	}

	/**
	 * 启动服务，监听来自客户端的请求
	 * 
	 * @param port
	 *            端口号
	 * @throws IOException
	 */
	public void start(int port, InputStream pfxIn, String pfxPassword) throws IOException {
		HttpServerProvider provider = HttpServerProvider.provider();
		httpserver = provider.createHttpsServer(new InetSocketAddress(port), backlog);
		httpserver.createContext("/", new ActionHttpHandle());
		SSLContext sslContext;
		try {
			// initialise the SSL context
			//SSLContext c = SSLContext.getDefault();
			char[] password = pfxPassword.toCharArray();
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(pfxIn, password);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keyStore, password);
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new IOException(e);
		}
		httpserver.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				try {
					SSLContext sslContext = this.getSSLContext();
					SSLEngine engine = sslContext.createSSLEngine();
					params.setNeedClientAuth(false);
					params.setCipherSuites(engine.getEnabledCipherSuites());
					params.setProtocols(engine.getEnabledProtocols());
					
					// get the default parameters
					SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
					params.setSSLParameters(defaultSSLParameters);
				} catch (Exception ex) {
					NetLog.logWarn(ex);
				}
			}
		});
		httpserver.setExecutor(new DirectExecutor()); // 选择默认处理器
		httpserver.start();
	}

	/**
	 * 关闭服务
	 * 
	 * @param delay
	 *            延迟关闭时间（单位：秒）
	 */
	public void stop(int delay) {
		httpserver.stop(delay);
	}

	/**
	 * Action处理类
	 * 
	 * @author Hansen
	 */
	protected class ActionHttpHandle implements HttpHandler {
		public void handle(HttpExchange httpExchange) throws IOException {
			// long t1 = System.currentTimeMillis();

			producerPool.execute(new HttpActionProcessor(httpExchange));
			// long t2 = System.currentTimeMillis() - t1;
			// System.out.println(DateUtils.getDateTime()+"
			// "+httpExchange.getRequestURI().toURL());
		}
	}

	private class DirectExecutor implements Executor {
		private boolean isFirst = true;

		public void execute(Runnable r) {
			// 首次运行时，更新线程名称
			if (isFirst) {
				isFirst = false;
				String threadName = Thread.currentThread().getName();
				if (threadName == null || threadName.startsWith("Thread-")) {
					threadName = "HttpsServer-" + threadName.substring(7);
				}
				Thread.currentThread().setName(threadName);
			}
			// HttpServer主线程，直接执行
			r.run();
		}
	}

}
