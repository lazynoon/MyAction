import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.StreamSocket;
import net_io.utils.MixedUtils;


public class AgentServer {

	private static ConcurrentHashMap<NetChannel, LinkInfo> map = new ConcurrentHashMap<NetChannel, LinkInfo>();
	private static ConcurrentHashMap<Integer, TargetAddress> portMap = new ConcurrentHashMap<Integer, TargetAddress>();
	private static int tgwPort = 0;
	public static class LinkInfo {
		NetChannel client = null;
		NetChannel server = null;
	}
	
	public static class TargetAddress {
		String host = null;
		int port = 0;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		StreamSocket socket = new StreamSocket() {

			public void onConnect(NetChannel channel) throws Exception {
				LinkInfo link = map.get(channel);
				if(link == null) { //Client Side 
					link = new LinkInfo();
					link.client = channel;
					map.put(channel, link);
					System.out.println("OnConnect client side");
					//本地端口在映射列表中，则连接目标服务器
					TargetAddress target = portMap.get(channel.getLocalPort());
					if(target != null) {
						NetChannel connectChannel = this.connect(target.host, target.port);
						link.server = connectChannel;
						map.put(connectChannel, link);
					}
				} else { //Server Side
					link.server = channel;
					System.out.println("OnConnect server side");
				}
			}
			
			public void onClose(NetChannel channel) throws Exception {
				LinkInfo link = map.get(channel);
				if(link == null) {
					return;
				}
				if(channel == link.server) { //Server side close
					if(link.client != null) {
						this.closeChannel(link.client);
						link.client = null;
					}
				} else if(channel == link.client) { //Client side close
					if(link.server != null) {
						this.closeChannel(link.server);
						link.server = null;
					}
				} else {
					System.out.println("Maybe is the last OnClose");
				}
			}
			
			public void onReceive(NetChannel channel, ByteArray data) throws Exception {
				LinkInfo link = map.get(channel);
				if(link == null) {
					this.closeChannel(channel);
					return;
				}
				if(channel == link.server) { //Server side close
					if(link.client == null) {
						this.closeChannel(channel);
						return;
					}
					this.send(link.client, data);
				} else {
					if(link.server == null) { //first package as TGW head
						String str = data.toString();
						String headers[] = str.split("\r\n");
						if(headers[0].equalsIgnoreCase("tgw_l7_forward") == false) {
							System.out.println("Not TGW head: "+str);
							this.closeChannel(channel);
							return;
						}
						String host = "127.0.0.1";
						int port = 0;
						for(int i=1; i<headers.length; i++) {
							if("subhost:".equalsIgnoreCase(headers[i])) {
								host = headers[i].substring(8).trim();
							} else if("subport:".equalsIgnoreCase(headers[i])) {
								port = MixedUtils.parseInt(headers[i].substring(8).trim());
							}
						}
						NetChannel connectChannel = this.connect(host, port);
						link.server = connectChannel;
						map.put(connectChannel, link);
					} else {
						this.send(link.server, data);
					}
				}
				
				
			}
			
		};
		//load config
        Properties prop = new Properties();
        InputStream in = AgentServer.class.getResourceAsStream("/agent.properties");
        prop.load(in);
        Enumeration names = prop.keys();
        while(names.hasMoreElements()) {
        	String name = (String)names.nextElement();
        	String value = prop.getProperty(name);
        	if(value == null) {
        		continue; 
        	}
       		value = value.trim();
        	if("tgw_port".equals(name)) {
        		tgwPort = Integer.parseInt(value);
        	} else if(name.startsWith("forward_")) {
        		int port = MixedUtils.parseInt(name.substring(8).trim());
        		String arr[] = value.split(":");
        		TargetAddress target = new TargetAddress();
        		target.host = arr[0];
        		target.port = MixedUtils.parseInt(arr[1].trim());
        		portMap.put(port, target);
        		//listen port
        		socket.bind(port);
        		System.out.println("Port: "+port+", Target: "+value);
        	}
        }
        in.close();
		
        if(tgwPort > 0) {
			socket.bind(tgwPort);
			System.out.println("AgentServer listen on "+tgwPort);
        } else {
        	System.out.println("AgentServer only forward.");
        }
	}

	
}
