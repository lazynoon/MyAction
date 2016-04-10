package test.action.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net_io.core.NetChannel;
import net_io.myaction.BaseMyAction;
import net_io.myaction.MyActionClient;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.myaction.server.MyActionSocket;
import net_io.utils.DateUtils;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

public class ChatAction extends BaseMyAction {
	private static HashMap<NetChannel, Integer> channelMap = new HashMap<NetChannel, Integer>(102400);
	private static long sequence = 0;
	public void doServer() {
		String sender = request.getParameter("sender");
		String content = request.getParameter("content");
		if(MixedUtils.isEmpty(sender)) {
			response.setError(1001, "The parameter of 'sender' is empty.");
			return;
		}
		if(content != null) {
			content = content.trim();
		}
		if(MixedUtils.isEmpty(content)) {
			response.setError(1001, "The parameter of 'content' is empty.");
			return;
		}		
		String time = DateUtils.getDateTime();
		//注册帐号
		NetChannel channel = request.getChannel();
		if(channelMap.containsKey(channel) == false) {
			channelMap.put(channel, 1);
		}
		ArrayList<NetChannel> invalidList = new ArrayList<NetChannel>();
		synchronized(channelMap) {
			sequence++;
			//发送消息给所有人
			Request request = new Request();
			request.setPath("/action/chat/chat.client");
			request.setParameter("time", time);
			request.setParameter("sender", sender);
			request.setParameter("content", content);
			request.setParameter("sequence", String.valueOf(sequence));
			request.setParameter("online", String.valueOf(channelMap.size()));
			int loop = 0;
			for(NetChannel ch : channelMap.keySet()) {
				if(ch.isActive() == false) {
					invalidList.add(ch);
					continue;
				}
				MyActionClient client = new MyActionClient(ch);
				try {
					loop++;
					final int num = loop;
					client.anyncPost(request, new MyActionSocket.Reader() {
						public void callback(Response response) {
							System.out.println(num+". client response: "+response.toString());
						}
					});
				} catch (IOException e) {
					NetLog.logError(e);
				}
			}
			//移除失效的channel
			for(NetChannel ch : invalidList) {
				channelMap.remove(ch);
			}
		}
		response.println("send success! client numer: "+channelMap.size());
		if(invalidList.size() > 0) {
			System.err.println(DateUtils.getDateTime()+" - closed channel number: "+invalidList.size());
		}
	}
	
	public void doClient() {
		String sender = request.getParameter("sender");
		String time = request.getParameter("time");
		String content = request.getParameter("content");
		String sequence = request.getParameter("sequence");
		String online = request.getParameter("online");
		System.out.println("["+online+","+sequence+"]  "+time+"  "+sender+" said: "+content);
	}

}
