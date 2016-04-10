package test.action.chat;

import net_io.myaction.ActionFactory;
import net_io.myaction.MyActionClient;
import net_io.myaction.MyActionServer;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;
import test.action.ActionPackage;
import test.action.Config;

public class ChatClient {
	public static int socketPort = 9083; // 默认端口号
	public static int concurrent = 256; // 并发量

	public static void main(String[] args) throws Exception {
		//NetLog.LOG_LEVEL = NetLog.RECORD_ALL;
		if(args.length < 3) {
			System.out.println("Command: java test.action.chat.ChatClient localhost 9083 YourName");
			return;
		}
		String host = args[0];
		int port = MixedUtils.parseInt(args[1]);
		String sender = args[2];
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
		
		MyActionClient client = new MyActionClient();
		client.connect(host, port);
		System.out.println("Please input some thing: ");
		StringBuffer sb = new StringBuffer();
		while(true) {
			int c = System.in.read();
			sb.append((char)c);
			if(c == '\n') {
				Request request = new Request();
				request.setPath("/action/chat/chat.server");
				request.setParameter("sender",sender);
				request.setParameter("content",sb.toString());
				Response response = client.post(request);
				if(response.getError() != 0) {
	//				System.err.println("Response error: "+response.get
					System.err.println("Response error: "+response.getError()+", reason: "+response.getReason());
				}
				sb = new StringBuffer();
			}
		}
		
	}
	

}
