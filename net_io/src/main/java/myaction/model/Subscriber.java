package myaction.model;

import net_io.utils.Mixed;

public interface Subscriber {
	public void onMessage(String path, Mixed data);
}
