package test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net_io.core.NetChannel;
import net_io.myaction.BaseMyAction;
import net_io.myaction.MyActionClient;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.DateUtils;

public class HelloAction extends BaseMyAction {
	private static ArrayList<NetChannel> channelList = new ArrayList<NetChannel>();
	private static HashMap<NetChannel, Integer> channelMap = new HashMap<NetChannel, Integer>();
	public void doIndex() {
		response.println("hello");
		response.println("your post: "+request.getParameter("val"));
		NetChannel channel = request.getChannel();
		if(channelMap.containsKey(channel) == false) {
			channelMap.put(channel, 1);
			channelList.add(channel);
		}
		for(NetChannel ch : channelList) {
			MyActionClient client = new MyActionClient(ch);
			Request request = new Request();
			request.setPath("/action/hello.clientSide");
			request.setParameter("date", DateUtils.getDateTime());
			try {
				Response response = client.post(request);
				System.out.println("client response: "+response.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
	}
	public void doClientSide() {
		response.println("hello in ClientSide");
		System.out.println("\n\treceive server side notice: "+request.getParameter("date")+"\n");
	}
}
