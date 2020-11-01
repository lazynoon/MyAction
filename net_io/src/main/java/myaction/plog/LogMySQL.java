package myaction.plog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import myaction.utils.DateUtil;
import net.sf.jsqlx.DB;
import net.sf.jsqlx.JSQLException;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

abstract public class LogMySQL extends LogRowSet {
	final public static int MAX_MD5CODE_SIZE = 10000;
	final public static int MAX_INSERT_BUFF_SIZE = 1000000;
	private HashMap<String, Boolean> existTables = new HashMap<String, Boolean>();
	abstract protected String getCreateTableSQL(String tableName) throws SQLException;
	abstract protected DB choiceDB(LogRow row) throws SQLException;
	abstract protected String choiceTable(LogRow row);
	
	protected boolean isTableExists(DB db, String tableName) throws SQLException {
		if(existTables.containsKey(tableName)) {
			return true;
		}
		String one = db.getOne("SHOW TABLES LIKE '"+db.escape(tableName)+"'");
		boolean ret = ! MixedUtils.isEmpty(one);
		if(ret) {
			existTables.put(tableName, new Boolean(true));
		}
		return ret;
	}
		
	//TODO: 异常，转存文件
	@Override
	protected int runOnce() {
		List<LogRow> data = this.reset();
		HashMap<String, ArrayList<LogRow>> rowSet = new HashMap<String, ArrayList<LogRow>>();
		try {
			for(LogRow row : data) {
				DB db;
				try {
					db = choiceDB(row);
				} catch(SQLException e) {
					NetLog.logWarn(e.toString());
					NetLog.logWarn("Not Record: "+row.getData());
					return 0;
				}
				String tableName = choiceTable(row);
				row.db = db;
				row.tableName = tableName;
				String md5Code = row.getMd5Code();
				ArrayList<LogRow> list = rowSet.get(md5Code);
				if(list == null) {
					list = new ArrayList<LogRow>();
					rowSet.put(md5Code, list);
				}
				list.add(row);
				if(rowSet.size() >= MAX_MD5CODE_SIZE) {
					rowSet = save(rowSet); //先保存一部分
				}
			}
			rowSet = save(rowSet); //先保存一部分
		} catch (JSQLException e) {
			System.err.println(DateUtil.getDateTime()+ " "+e);
			System.err.println("LastSQL: "+e.getRunSQL());
			e.printStackTrace();			
		} catch (Exception e) {
			System.err.println(DateUtil.getDateTime()+ " "+e);
			e.printStackTrace();
		}
		return data.size();
	}
	
	private HashMap<String, ArrayList<LogRow>> save(HashMap<String, ArrayList<LogRow>> rowSet) throws SQLException {
		if(rowSet.size() == 0) {
			return rowSet;
		}
		for(String key : rowSet.keySet()) {
			StringBuffer head = new StringBuffer();
			StringBuffer buff = new StringBuffer();
			ArrayList<LogRow> list = rowSet.get(key);
			int size = 0;
			Set<String> headSet = null;
			DB db = null;
			for(int i=0; i<list.size(); i++) {
				LogRow row = list.get(i);
				if(i == 0) {
					LinkedHashMap<String, String> headMap = row.getData();
					headSet = headMap.keySet();
					addInsertHead(head, row.tableName, headSet);
					//检查数据表是否存在，若不存在，自动创建
					if(this.isTableExists(row.db, row.tableName) == false) {
						row.db.executeUpdate(this.getCreateTableSQL(row.tableName));
					}
					db = row.db;
				}
				addInsertStr(buff, headSet, row);
				size += row.getValueLength();
				if(size >= MAX_INSERT_BUFF_SIZE) {
					head.append(buff);
					row.db.executeUpdate(head.toString());
					head = new StringBuffer();
					buff = new StringBuffer();
					addInsertHead(head, row.tableName, headSet);
				}
			}
			//执行SQL
			if(db != null && buff.length() > 0) {
				head.append(buff);
				db.executeUpdate(head.toString());
			}
		}
		return new HashMap<String, ArrayList<LogRow>>();
	}
	
	private void addInsertHead(StringBuffer buff, String tableName, Set<String> keys) {
		buff.append("INSERT INTO `"+tableName+"` ");
		buff.append("(");
		boolean first = true;
		for(String key : keys) {
			if(first) {
				first = false;
			} else {
				buff.append(",");
			}
			buff.append("`");
			buff.append(key);
			buff.append("`");
		}
		buff.append(") VALUES ");
		
	}
	private void addInsertStr(StringBuffer buff, Set<String> keys, LogRow row) {
		LinkedHashMap<String, String> map = row.getData();
		if(map.size() == 0) {
			return;
		}
		if(buff.length() > 0) {
			buff.append(",");
		}
		boolean first = true;
		buff.append("(");
		for(String key : keys) {
			String value = map.get(key);
			if(first) {
				first = false;
			} else {
				buff.append(",");
			}
			if(value != null) {
				buff.append("'");
				buff.append(row.db.escape(value));
				buff.append("'");
			} else {
				buff.append("NULL");
			}
		}
		buff.append(")");
	}

	public class SplitData {
		DB db;
		String tableName;
	}
}
