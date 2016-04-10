package net_io.myaction.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net_io.myaction.ActionProcessor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class MyHttpServer {
	private HttpServer httpserver = null;
	public static int concurrent = 256; // 并发量
	public static int backlog = 256; // 并发量
	ThreadPoolExecutor producerPool;
	
	public MyHttpServer(ThreadPoolExecutor producerPool) {
		this.producerPool = producerPool;
	}

	/**
	 * 启动服务，监听来自客户端的请求
	 * @param port 端口号
	 * @throws IOException
	 */
	public void start(int port) throws IOException {
		HttpServerProvider provider = HttpServerProvider.provider();
		httpserver = provider.createHttpServer(new InetSocketAddress(port), backlog);
		httpserver.createContext("/", new ActionHttpHandle());
		ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 256, 600,  
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10240),  
                new ThreadPoolExecutor.DiscardOldestPolicy());
		httpserver.setExecutor(executor);
		httpserver.start();
	}

	/**
	 * 关闭服务
	 * @param delay 延迟关闭时间（单位：秒）
	 */
	public void stop(int delay) {
		httpserver.stop(delay);
	}

	/**
	 * Action处理类
	 * @author Hansen
	 */
	protected class ActionHttpHandle  implements HttpHandler {
		public void handle(HttpExchange httpExchange) throws IOException {
			long t1 = System.nanoTime();
			producerPool.execute(new ActionProcessor(httpExchange));
		}
	}
	
	
}
