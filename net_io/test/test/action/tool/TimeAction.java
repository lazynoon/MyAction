package test.action.tool;

import net_io.myaction.BaseMyAction;
import net_io.utils.DateUtils;

public class TimeAction extends BaseMyAction {
	public void doIndex() {
		response.println(DateUtils.getDateTime());
	}
}
