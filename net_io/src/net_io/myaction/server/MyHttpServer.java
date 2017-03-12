package net_io.myaction.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import net_io.utils.thread.MyThreadPool;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class MyHttpServer {
	private HttpServer httpserver = null;
	//public static int concurrent = 256; // 并发量
	private static int backlog = 8196; // 最大等待连接数
	MyThreadPool producerPool;
	
	public MyHttpServer(MyThreadPool producerPool) {
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
//		ThreadPoolExecutor executor = new ThreadPoolExecutor(256, 256, 600,  
//                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10240),  
//                new ThreadPoolExecutor.DiscardOldestPolicy());
//		httpserver.setExecutor(executor);
		httpserver.setExecutor(new DirectExecutor()); //选择默认处理器
		//		httpserver.setExecutor(null); //选择默认处理器
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
			//long t1 = System.currentTimeMillis();
			
			producerPool.execute(new HttpActionProcessor(httpExchange));
			//long t2 = System.currentTimeMillis() - t1;
			//System.out.println(DateUtils.getDateTime()+" "+httpExchange.getRequestURI().toURL());
		}
	}
	
	private class DirectExecutor implements Executor {
		public void execute(Runnable r) {
			r.run();
		}
	}
	
	
}
