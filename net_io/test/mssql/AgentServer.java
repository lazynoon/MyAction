package mssql;

import net_io.utils.MixedUtils;
import net_io.utils.NetLog;


public class AgentServer {
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//NetLog.LOG_LEVEL = 1;
		if(args == null || args.length < 2 || MixedUtils.isEmpty(args[1]) || !MixedUtils.isNumeric(args[0])) {
			System.err.println("Please input the paramenter. \nFormat: \n\tjava ClassName LocalPort RemoteHost[:RemotePort] [ConnectionPoolSize=10]");
			return;
		}
		int localPort = MixedUtils.parseInt(args[0]);
		String serverHost = args[1];
		int serverPort = localPort;
		int pos = serverHost.indexOf(":");
		if(pos > 0) {
			serverPort = MixedUtils.parseInt(serverHost.substring(pos+1));
			serverHost = serverHost.substring(0, pos);
		}
		int connectionPoolSize = 3;
		if(args.length > 2) {
			if(MixedUtils.isNumeric(args[2])) {
				connectionPoolSize = MixedUtils.parseInt(args[2]);
			}
		} else {
			System.err.println("WARNING: ConnectionPoolSize is not numeric. Use the default value: "+connectionPoolSize);
		}
		
		BothSide both = new BothSide();
		both.serverSideSocket.initConnectPool(serverHost, serverPort, connectionPoolSize);
		both.clientSideSocket.setDaemon(false);
		both.clientSideSocket.bind(serverPort);
		
		
		System.out.println("AgentServer listen on "+serverHost+":"+serverPort);
	}


}
