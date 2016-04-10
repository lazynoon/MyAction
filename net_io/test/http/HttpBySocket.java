package http;

import java.nio.ByteBuffer;

import mssql.MD5Util;
import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.StreamSocket;
import net_io.utils.DateUtils;

public class HttpBySocket extends StreamSocket {
	
	public void onConnect(NetChannel channel) throws Exception {
		ByteArray data = new ByteArray(8192);
//		String s1 = "POST /Interface/POS/processor.php HTTP/1.1\r\nHost: www.lecake.com\r\nContent-Type: application/x-www-form-urlencoded; charset=utf8\r\nContent-Length: 380\r\n\r\n";
//		String s2 = "context=<?xml version='1.0'  encoding='UTF-8'  ?><transaction><transaction_header><version>1.0</version><transtype>P002</transtype><userCode>0036</userCode><terminal>00000001</terminal><request_time>20150601101356</request_time><mac>3CE6EF48110659F0B29A289DA19B3F1A</mac></transaction_header><transaction_body><orderno>20150526SHKF005779</orderno></transaction_body></transaction>";
		//String s1 = "POST /Interface/POS/processor.php HTTP/1.1\r\nHost: www.lecake.com\r\nContent-Type: application/x-www-form-urlencoded; charset=utf8\r\nContent-Length: 500\r\n\r\n";
		//String s2 = "context=%3C%3Fxml+version%3D%271.0%27++encoding%3D%27UTF-8%27++%3F%3E%3Ctransaction%3E%3Ctransaction_header%3E%3Cversion%3E1.0%3C%2Fversion%3E%3Ctranstype%3EP002%3C%2Ftranstype%3E%3CuserCode%3E0036%3C%2FuserCode%3E%3Cterminal%3E00000001%3C%2Fterminal%3E%3Crequest_time%3E20150601101356%3C%2Frequest_time%3E%3Cmac%3E3CE6EF48110659F0B29A289DA19B3F1A%3C%2Fmac%3E%3C%2Ftransaction_header%3E%3Ctransaction_body%3E%3Corderno%3E20150526SHKF005779%3C%2Forderno%3E%3C%2Ftransaction_body%3E%3C%2Ftransaction%3E";

//		String s1 = "POST /Interface/POS/processor.php HTTP/1.1\r\nHost: www.lecake.com\r\nContent-Type: application/x-www-form-urlencoded; charset=utf8\r\nContent-Length: 376\r\n\r\n";
//		String s2 = "context=<?xml version='1.0'  encoding='UTF-8'  ?><transaction><transaction_header><version>1.0</version><transtype>P005</transtype><userCode>0036</userCode><terminal>00000001</terminal><request_time>20150602141547</request_time><mac>59D0984813485DF308A3EDB3BB54136D</mac></transaction_header><transaction_body><username>测试—pda</username></transaction_body></transaction>";
		
//		String s1 = "POST /Interface/POS/processor.php HTTP/1.1\r\nHost: www.lecake.com\r\nContent-Type: application/x-www-form-urlencoded; charset=utf8\r\nContent-Length: 514\r\n\r\n";
//		String s2 = "context=%3C%3Fxml+version%3D%271.0%27++encoding%3D%27UTF-8%27++%3F%3E%3Ctransaction%3E%3Ctransaction_header%3E%3Cversion%3E1.0%3C%2Fversion%3E%3Ctranstype%3EP005%3C%2Ftranstype%3E%3CuserCode%3E0036%3C%2FuserCode%3E%3Cterminal%3E00000001%3C%2Fterminal%3E%3Crequest_time%3E20150602141547%3C%2Frequest_time%3E%3Cmac%3E59D0984813485DF308A3EDB3BB54136D%3C%2Fmac%3E%3C%2Ftransaction_header%3E%3Ctransaction_body%3E%3Cusername%3E%E6%B5%8B%E8%AF%95%E2%80%94pda%3C%2Fusername%3E%3C%2Ftransaction_body%3E%3C%2Ftransaction%3E";

		String s2 = "context=<?xml version='1.0'  encoding='UTF-8'  ?><transaction><transaction_header><version>1.0</version><transtype>P005</transtype><userCode>0036</userCode><terminal>0000001</terminal><request_time>20150603124931</request_time><mac>AB15C58EC274E0991B075E0AC667317B</mac></transaction_header><transaction_body><username>测试—pda</username></transaction_body></transaction>";
		String s1 = "POST /Interface/POS/processor.php HTTP/1.1\r\nHost: www.lecake.com\r\nContent-Type: application/x-www-form-urlencoded; charset=utf8\r\nContent-Length: "+s2.getBytes("UTF-8").length+"\r\n\r\n";
		
		System.out.println("send header: "+s1);
		
		data.writeBytes(s1.getBytes("UTF-8"));
		data.writeBytes(s2.getBytes("UTF-8"));
		ByteBuffer b1 = ByteBuffer.allocate(8192);
		ByteBuffer b2 = ByteBuffer.allocate(8192);
		b1.put(s1.getBytes("UTF-8"));
		b1.flip();
		channel.send(b1);
		
		b2.put(s2.getBytes("UTF-8"));
		b2.flip();
		channel.send(b2);
	}
	
	public void onClose(NetChannel channel) throws Exception {
	}
	
	public void onReceive(NetChannel channel, ByteArray data) throws Exception {
		ByteBuffer b1 = ByteBuffer.allocate(8192);
		System.out.println(DateUtils.getDateTime() +" - ChID: "+channel.getChannelID()+", Buff: "+data.size()+", MD5: "+MD5Util.md5(data));
		System.out.println(new String(data.getByteBuffer().array(), "UTF-8"));

	}
	

	public static void main(String[] args) throws Exception {
		HttpBySocket socket = new HttpBySocket();
		//socket.connect("192.168.0.20", 8019);
		socket.connect("127.0.0.1", 80);
		System.out.println("type any key to stop thread.");
		System.in.read();
	}

}
