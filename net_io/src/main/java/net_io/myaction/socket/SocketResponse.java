package net_io.myaction.socket;

import net_io.myaction.Response;
import net_io.utils.Mixed;

public class SocketResponse extends Response {
	public static final int MSG_ID = 0x8302;
	
	public Mixed getBodyMixed() {
		Mixed result = new Mixed();
		result.set("error", error);
		result.set("reason", reason);
		if(mode == MODE.JSON) {
			result.set("data", data);
		} else {
			result.set("data", body.toString());
		}
		return result;
	}

}
