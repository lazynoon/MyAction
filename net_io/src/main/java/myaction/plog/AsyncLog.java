/**
 * 异步保存日志
 * 支持：同一个数据库，MySQL数据库
 */
package myaction.plog;

import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import myaction.extend.BaseDao;
import myaction.extend.AppConfig;
import myaction.utils.DateUtil;
import net.sf.jsqlx.DB;
import net_io.myaction.CheckException;
import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;

public class AsyncLog {
	// LOG记录表的模式
	public enum MODE {
		DIRECT, //直接保存到表
		SPLIT_DAY, //按天分表
		SPLIT_MONTH, //按月分表
		SPLIT_YEAR, //按年分表
	}
	private static ConcurrentHashMap<String, MyLog> logObjectPool = new ConcurrentHashMap<String, MyLog>();
	private static Object single = new Object();
	
	/**
	 * 异步记录日志到DB中
	 * @param dsnName DSN配置的名称
	 * @param tplTable 模版表名
	 * @param saveMode 保存模式（参考MODE）
	 * @param logInfo 保存的信息
	 * @throws CheckException
	 */
	public static void log(String dsnName, String tplTable, MODE saveMode, Mixed logInfo) throws CheckException {
		String key = dsnName + "-|-" + tplTable;
		MyLog logObj = logObjectPool.get(key);
		if(logObj == null) {
			synchronized(single) {
				logObj = logObjectPool.get(key);
				if(logObj == null) {
					//非直接存表模式，模版表名必须以“zz_”开头
					String dstTable = tplTable;
					if(saveMode != MODE.DIRECT) {
						if(tplTable.startsWith("zz_") == false) {
							throw new CheckException(510, "LOG template table("+tplTable+") must start with 'zz_'");
						}
						//目标表名的格式：log_原名[去_log]_日期
						dstTable = "log_" + tplTable.substring(3);
						if(dstTable.endsWith("_log")) {
							dstTable = dstTable.substring(0, dstTable.length()-4);
						}
						dstTable += "_";
					}
					logObj = new MyLog(dsnName, tplTable, dstTable, saveMode);
					logObjectPool.put(key, logObj);
				}
			}
		}
		logObj.addLogRow(logInfo);
	}
	
	protected static class MyLog extends LogMySQL {
		
		private String dsnName;
		private String srcTable;
		private String dstTable;
		private MODE saveMode;
				
		protected MyLog(String dsnName, String srcTable, String dstTable, MODE saveMode) {
			this.dsnName = dsnName;
			this.srcTable = srcTable;
			this.dstTable = dstTable;
			this.saveMode = saveMode;
			AppConfig.logService.addRowSet(this);
		}
		
		
		@Override
		protected String getCreateTableSQL(String tableName) throws SQLException {
			String createSql = BaseDao.getCreateTable(DB.instance(dsnName), srcTable);
			createSql = createSql.replace(srcTable, tableName);
			return createSql;
		}

		@Override
		protected DB choiceDB(LogRow row) throws SQLException {
			return DB.instance(dsnName);
		}

		@Override
		protected String choiceTable(LogRow row) {
			if(saveMode == MODE.DIRECT) {
				return dstTable;
			}
			String id = row.getData().get("log_id");
			Date date;
			if(id != null && id.length() == 16) {
				date = new Date(EncodeUtils.parseRandTime(id));
			} else {
				date = new Date();
			}
			String table = dstTable;
			if(saveMode == MODE.SPLIT_YEAR) {
				table += DateUtil.formatLikePHP(date, "Y");
			} else if(saveMode == MODE.SPLIT_MONTH) {
				table += DateUtil.formatLikePHP(date, "Ym");
			} else if(saveMode == MODE.SPLIT_DAY) {
				table += DateUtil.formatLikePHP(date, "Ymd");
			} else {
				//不支持的类型，保存到模版表中
			}
			return table;
		}
		
	}

}
