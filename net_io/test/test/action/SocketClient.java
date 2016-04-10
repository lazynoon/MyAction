package test.action;

import net_io.myaction.ActionFactory;
import net_io.myaction.MyActionClient;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.NetLog;

public class SocketClient {

	public static void main(String[] args) throws Exception {
		StringBuffer sb = new StringBuffer();
		
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
				System.out.println("Response: "+response.getData());
				System.out.println("Please input some thing");
				sb = new StringBuffer();
			}
		}
		
	}
	

}
