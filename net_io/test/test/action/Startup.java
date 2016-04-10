package test.action;

import net_io.myaction.ActionFactory;
import net_io.myaction.MyActionClient;
import net_io.myaction.MyActionServer;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.DateUtils;
import net_io.utils.NetLog;

public class Startup {
	public static int port = 8083; // 默认端口号
	public static int concurrent = 256; // 并发量

	public static void main(String[] args) throws Exception {
		String cmd = null;
		if(args.length > 0) {
			cmd = args[0];
		}
		NetLog.LOG_LEVEL = NetLog.RECORD_ALL;
		//初始配置信息加载
		Config.loadProperites(); //加载配置
		//注册plugins
		for(ActionPackage pkgInfo : Config.getActionPackages()) {
			if(pkgInfo.isValid() == false) {
				NetLog.logError("Ingore not valid action package module: "+pkgInfo.getModuleName());
				continue;
			}
			ActionFactory.registerByPackage(pkgInfo.getPkgName(), pkgInfo.getPrefixPath());			
		}
		//检查启动时命令进行处理
		MyActionServer server = new MyActionServer();
		server.startSocketServer(9038);
		server.startHttpServer(8083);
		StringBuffer sb = new StringBuffer();
		
		
		MyActionClient client = new MyActionClient();
		client.connect("127.0.0.1", 9038);
		System.out.println("Please input some thing");
		while(true) {
			int c = System.in.read();
			sb.append((char)c);
			if(c == '\n') {
				Request request = new Request();
				request.setPath("/action/hello");
				request.setParameter("val", sb.toString());
				Response response = client.post(request);
				System.out.println("Response: "+response);
				System.out.println("Please input some thing");
				sb = new StringBuffer();
			}
		}
		
//		long requestCount = 0;
//		long processCount = 0;
//		while(true) {
//			Thread.sleep(3600*1000);
//			if(requestCount != Config.requestCount || processCount != Config.processCount) {
//				requestCount = Config.requestCount;
//				processCount = Config.processCount;
//				System.out.println(DateUtils.getDateTime() + " - Request: "+requestCount+", Process: "+processCount);
//			}
//		}
	}
	

}
