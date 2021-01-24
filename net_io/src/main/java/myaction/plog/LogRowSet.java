package myaction.plog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net_io.utils.Mixed;

public abstract class LogRowSet {
	private Object single = new Object();
	private List<LogRow> data = new ArrayList<LogRow>();
	private List<LogRow> emptyData = new ArrayList<LogRow>();
	
	public void addRow(Map<String, Object> row) {
		LogRow logRow = new LogRow(row);
		synchronized(single) {
			data.add(logRow);
		}
	}
	public void addLogRow(Mixed row) {
		LogRow logRow = new LogRow(row);
		synchronized(single) {
			data.add(logRow);
		}
	}
	protected List<LogRow> reset() {
		List<LogRow> ret = emptyData;
		synchronized(single) {
			if(data.size() > 0) {
				ret = data;
				data = new ArrayList<LogRow>();
			}
		}
		return ret;
	}
	abstract protected int runOnce();
}
