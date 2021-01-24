package myaction.action.monitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import myaction.action.MyMonitorBaseAction;
import myaction.extend.AppConfig;
import myaction.utils.DateUtil;
import net_io.myaction.MyActionClient;
import net_io.myaction.MyActionServer;
import net_io.utils.Mixed;
import net_io.utils.NetLog;

public class LifeConsoleAction extends MyMonitorBaseAction {
	private static MyActionServer myServer = null;
	private Object single = new Object();
	private StopThread stopThread = new StopThread();
	//重启时，可能因为 StopThread 未加载，导致出现 java.lang.ClassNotFoundException: myaction.manager.ConsoleAction$StopThread 报错
	private static LifeConsoleAction _preloadInstance = null;

	@Override
	public void service(String methodName) throws Exception {
		if("Stop".equalsIgnoreCase(methodName)) {
			this.doStop();
		} else {
			this.throwNotFoundException(methodName);
		}
		
	}

	public static void saveActionServer(MyActionServer server) {
		myServer = server;
		if(_preloadInstance == null) {
			_preloadInstance = new LifeConsoleAction(); 
		}
	}
	
	public boolean check() {
		if("127.0.0.1".equals(this.request.getRemoteIP()) == false) {
			return false;
		}
		if("127.0.0.1".equals(this.request.getClientIP()) == false) {
			return false;
		}
		if("yes".equalsIgnoreCase(this.request.getParameter("stop")) == false) {
			return false;
		}
		NetLog.logInfo("Console check pass");
		
		return true;
	}
	public void doStop() throws Exception {
		//long time1 = System.currentTimeMillis();
		NetLog.logInfo("Start stop cmd: "+DateUtil.getDateTime());
		
		synchronized(single) {
			stopThread.start();
			single.wait();
		}
		NetLog.logInfo("The server will be close.");
	}
	
	/**
	 * BOSS线程
	 * @author Hansen
	 *
	 */
	protected class StopThread extends Thread {
		public void run() {
			synchronized(single) {
				single.notify();
			}
			myServer.stopServer(3);
			try {
				sleep(500);
			} catch (InterruptedException e) {
				NetLog.logWarn(e);
			}
			//关闭异步日志服务
			AppConfig.logService.stop();
			
			NetLog.logInfo("Stop server at: "+DateUtil.getDateTime());
			System.exit(0);
		}
		
	}
	
	public static void stopAnotherServer() {
		MyActionClient client = new MyActionClient(AppConfig.getDefaultActionSocket());
		String host = "0.0.0.0";
		ArrayList<Integer> ports = new ArrayList<Integer>();
		ports.add(AppConfig.getSocketServerPort());
		ports.add(AppConfig.getHttpServerPort());
		boolean existAnother = false;
		for(int i=0; i<ports.size(); i++) {
			int port = ports.get(i);
			if(port > 0) {
				if(isPortUsing(host, port)) {
					existAnother = true;
					break;
				}
			}
		}
		if(existAnother == false) {
			return; //未启动其他服务
		}
		long startTime = System.currentTimeMillis();
		long waitStopTime = 5000; //关闭等待关闭另一个服务的时间
		try {
			client.connect("127.0.0.1", AppConfig.getSocketServerPort());
			Mixed data = new Mixed();
			data.put("stop", "yes");
			int reqID = client.sendRequest("/myaction/monitor/manager/lifeConsole.stop", data);
			Mixed ret = client.readResult(reqID, waitStopTime);
			client.close();
			NetLog.logInfo("Stop anather server return: "+ret);
		} catch (IOException e) {
			waitStopTime = 100; //出现错误，尽快返回
			NetLog.logInfo("Connect another server error: "+e.toString());
		}
		while(true) {
			existAnother = false;
			for(int i=0; i<ports.size(); i++) {
				int port = ports.get(i);
				if(port > 0 && isPortUsing(host, port)) {
					existAnother = true;
					break;
				} else {
					ports.remove(i); //删除端口
				}
			}
			if(existAnother == false) {
				break; //其他服务器已关闭（不再侦听端口）
			}
			if(System.currentTimeMillis() - startTime >= waitStopTime) {
				break; //等待超时
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				NetLog.logWarn(e);
				break;
			}			
		}
	}
	
	private static boolean isPortUsing(String host, int port) {
		Socket socket = new Socket();
		boolean using = false;
		try {
			InetSocketAddress addr = new InetSocketAddress(host, port);
			socket.bind(addr);
			socket.close();
		} catch(IOException e) {
			using = true;
		}
		return using;
	}
	
	
}
