package net_io.msg;

import java.util.HashMap;
import java.util.Map;

public class MsgClass {
	private Map<Integer, Class<BaseMsg>> msgClassMap = new HashMap<Integer, Class<BaseMsg>>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void register(int msgID, Class msgClass) {
		if(BaseMsg.class.isAssignableFrom(msgClass) == false) {
			throw new RuntimeException("The msg class("+msgClass.getName()+") is not extends of BaseMsg.");
		}
		msgClassMap.put(msgID, msgClass);
	}
	
	public BaseMsg createMsg(int msgID) throws InstantiationException,IllegalAccessException {
		Class<BaseMsg> cls = msgClassMap.get(msgID);
		if(cls == null) {
			return null;
		}
		BaseMsg msg = cls.newInstance();
		msg.resetMsgID(msgID);
		return msg;
	}
	

}
