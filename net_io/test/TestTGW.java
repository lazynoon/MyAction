import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.StreamSocket;


public class TestTGW {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		StreamSocket socket = new StreamSocket() {
			public void onConnect(NetChannel channel) throws Exception {
				System.out.println("OnConnect");
				String str = "tgw_l7_forward\r\nHost:testapi.app1101578295.twsapp.com:8000\r\nsubport:8005\r\n\r\n ";
				ByteArray data = ByteArray.wrap(str, "UTF-8");
				this.send(channel, data);
			}
			
			public void onClose(NetChannel channel) throws Exception {
				System.out.println("OnClose");
			}
			
			public void onReceive(NetChannel channel, ByteArray data) throws Exception {
				System.out.println("onReceive: "+data.size()+data);
			
			}
			
		};
		//socket.connect("testapi.app1101578295.twsapp.com", 8000);
		socket.connect("127.0.0.1", 381);
		//socket.connect("192.168.0.240", 8088);
	}

}
