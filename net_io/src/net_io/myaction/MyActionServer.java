package net_io.myaction;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net_io.myaction.server.MyActionSocket;
import net_io.myaction.server.MyHttpServer;
import net_io.utils.NetLog;

public class MyActionServer {
	public static final int MAX_POST_LENGTH = 1024 * 1024 * 2;
	ThreadPoolExecutor producerPool;
	private MyActionSocket socketServer = null;
	private MyHttpServer httpServer = null;
	
	public MyActionServer() {
        // 构造一个线程池  
		producerPool = new ThreadPoolExecutor(16, 256, 600,  
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10240),  
                new ThreadPoolExecutor.DiscardOldestPolicy());
	}
	
	public void startSocketServer(int port) throws IOException {
		socketServer = new MyActionSocket(producerPool);
		socketServer.bind(port);
		NetLog.logInfo("socket server started. port: "+port);
	}
	
	public void startHttpServer(int port) throws IOException {
		httpServer = new MyHttpServer(producerPool);
		httpServer.start(port);
		NetLog.logInfo("http server started. port: "+port);
	}
	
	
	/**
	 * 关闭SocketServer
	 * @param delay 延迟关闭的秒数
	 */
	public void stopServer(int delay) {
		long startTime = System.currentTimeMillis();
		if(socketServer != null) {
			socketServer.closeAllServerChannel();
		}
		if(httpServer != null) {
			httpServer.stop(delay);
		}
		if(socketServer != null) {
			try {
				long delay2 = delay * 1000 - (System.currentTimeMillis() - startTime);
				if(delay2 > 0) {
					Thread.sleep(delay2);
				}
			} catch (InterruptedException e) {
				NetLog.logWarn(e);
			}
			try {
				socketServer.stop();
			} catch (IOException e) {
				NetLog.logError(e);
			}
		}
	}
	
}
