package test.action.chat;

import net_io.myaction.ActionFactory;
import net_io.myaction.MyActionServer;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;
import test.action.ActionPackage;
import test.action.Config;

public class ChatServer {
	public static int bindHttpPort = 8083; // 默认端口号
	public static int bindSocketPort = 9083; // 默认端口号
	public static int concurrent = 256; // 并发量

	public static void main(String[] args) throws Exception {
		if(args.length > 0 && MixedUtils.isNumeric(args[0])) {
			bindHttpPort = MixedUtils.parseInt(args[0]);
		}
		if(args.length > 1 && MixedUtils.isNumeric(args[1])) {
			bindSocketPort = MixedUtils.parseInt(args[1]);
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
		server.startHttpServer(bindHttpPort);
		server.startSocketServer(bindSocketPort);
	}
	

}
