package net_io.myaction;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import net_io.core.AsyncSocketProcessor;
import net_io.core.StatNIO;
import net_io.myaction.ActionProcessor.ProcessorInfo;
import net_io.myaction.server.MyHttpServer;
import net_io.myaction.server.MyHttpsServer;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.socket.MyWebSocket;
import net_io.utils.NetLog;
import net_io.utils.thread.MyThreadPool;
import net_io.utils.thread.MyThreadPool.RunTask;
import net_io.utils.thread.RunnerThread.ThreadInfo;


public class MyActionServer {
	private MyThreadPool producerPool;
	private MyActionSocket socketServer = null;
	private MyWebSocket websocket = null;
	private MyHttpServer httpServer = null;
	private MyHttpsServer httpsServer = null;
	private int minPoolSize = 16;
	private int maxPoolSize = 256;
	
	public MyActionServer() {
	}
	
	public MyActionServer configThreadPool(int minPoolSize, int maxPoolSize) {
		this.minPoolSize = minPoolSize;
		this.maxPoolSize = maxPoolSize;
		return this;
	}
	
	private void initThreadPool() {
        // 构造一个线程池
		if(producerPool == null) {
			producerPool = new MyThreadPool("MyActionThread", minPoolSize, maxPoolSize);
		}
	}
	
	public void startSocketServer(int port) throws IOException {
		startSocketServer(port, null);
	}

	public void startSslSocketServer(int port) throws IOException {
		initThreadPool(); //初始化线程池
		socketServer = new MyActionSocket(producerPool, null);
		socketServer.bind(port);
		NetLog.logInfo("ssl socket server started. port: "+port);
	}

	public void startSocketServer(int port, AsyncSocketProcessor eventProcessor) throws IOException {
		initThreadPool(); //初始化线程池
		socketServer = new MyActionSocket(producerPool, eventProcessor);
		socketServer.bind(port);
		NetLog.logInfo("socket server started. port: "+port);
	}

	public void startWebSocketServer(int port) throws IOException {
		initThreadPool(); //初始化线程池
		websocket = new MyWebSocket(producerPool);
		websocket.bind(port);
		NetLog.logInfo("Web Socket server started. port: "+port);
	}
	
	public void startWebSocketSecureServer(int port) throws IOException {
		initThreadPool(); //初始化线程池
		websocket = new MyWebSocket(producerPool, true);
		websocket.bind(port);
		NetLog.logInfo("Web SSL Socket server started. port: "+port);
	}
	
	public void startHttpServer(int port) throws IOException {
		initThreadPool(); //初始化线程池
		httpServer = new MyHttpServer(producerPool);
		httpServer.start(port);
		NetLog.logInfo("http server started. port: "+port);
	}
	
	public void startHttpsServer(int port, String pfxFile, String pfxPassword) throws IOException {
		pfxFile = "/" + pfxFile;
		InputStream in = MyActionServer.class.getResourceAsStream(pfxFile);
		if(in == null) {
			throw new IOException("Https cert file is not exists: "+pfxFile);
		}
		//InputStream in = new FileInputStream(pfxFile);
		startHttpsServer(port, in, pfxPassword);
	}
	
	public void startHttpsServer(int port, InputStream pfxIn, String pfxPassword) throws IOException {
		initThreadPool(); //初始化线程池
		httpsServer = new MyHttpsServer(producerPool); 
		httpsServer.start(port, pfxIn, pfxPassword);
		NetLog.logInfo("https server started. port: "+port);
	}
	
	
	/**
	 * 关闭SocketServer
	 * @param delay 延迟关闭的秒数
	 */
	public void stopServer(int delay) {
		long startTime = System.currentTimeMillis();
		if(socketServer != null) {
			socketServer.stop(delay);
		}
		if(httpServer != null) {
			httpServer.stop(delay);
		}
		if(socketServer != null) {
			try {
				socketServer.stop();
			} catch (IOException e) {
				NetLog.logError(e);
			}
			try {
				long delay2 = delay * 1000 - (System.currentTimeMillis() - startTime);
				if(delay2 > 0) {
					if(delay2 > 100) {
						delay2 = 100;
					}
					Thread.sleep(delay2); //
				}
			} catch (InterruptedException e) {
				NetLog.logWarn(e);
			}
		}
		
	}
	
	/**
	 * 获取核心线程池大小
	 * @return int
	 */
	public int getMinPoolSize() {
		return minPoolSize;
	}

	/**
	 * 获取最大工作线程数
	 * @return int
	 */
	public int getMaxPoolSize() {
		return maxPoolSize;
	}
	
	/**
	 * 获取等待处理的请求数量
	 */
	public int getPendingRequestCount() {
		if(producerPool == null) {
			return 0;
		}
		return producerPool.getTaskQueueSize();
	}
	
	/**
	 * 获取重建线程的次数
	 */
	public long getRebuildThreadCount() {
		return producerPool.getRebuildThreadCount();
	}

	public List<RequestInfo> listRequestInfo() {
		ArrayList<RequestInfo> result = new ArrayList<RequestInfo>();
		if(producerPool == null) {
			return result;
		}
		for(ThreadInfo threadInfo : producerPool.listThreadInfo()) {
			RequestInfo data = new RequestInfo();
			RunTask task = threadInfo.getRunTask();
			//线程状态
			data.runNsTime = threadInfo.getRunNsTime();
			data.status = threadInfo.getStatus();
			data.runCount = threadInfo.getRunCount();
			data.path = "-"; //等待解析request并执行Action
			//请求信息
			if(task != null) {
				Runnable runner = task.getRunner();
				ActionProcessor processor = null;
				if(runner != null && runner instanceof ActionProcessor) {
					processor = (ActionProcessor) runner;
					ProcessorInfo processorInfo = processor.getProcessorInfo();
					data.runMode = processorInfo.getRunMode();
					data.path = processorInfo.getPath();
					data.remote = processorInfo.getRemoteAddress();
				}
			}
			result.add(data);
			
		}
		return result;
	}
	
	
	public static class RequestInfo {
		private String status;
		private long runCount = 0;
		private long runNsTime = 0;
		private String runMode = null;
		private String path = null;
		private InetSocketAddress remote = null;
		
		public String getStatus() {
			return status.toString();
		}

		/**
		 * 运行时间，单位毫秒
		 */
		public long getRunMsTime() {
			return runNsTime / StatNIO.ONE_MILLION_LONG;
		}

		/**
		 * 运行时间，单位微秒
		 */
		public long getRunUsTime() {
			return runNsTime / StatNIO.ONE_THOUSAND_LONG;
		}

		public long getRunCount() {
			return runCount;
		}

		public String getRunMode() {
			return runMode;
		}

		public String getPath() {
			return path;
		}

		public InetSocketAddress getRemoteAddress() {
			return remote;
		}
	}
}
