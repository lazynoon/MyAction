package myaction.extend;

import net.sf.jsqlx.DB;
import net.sf.jsqlx.PrepareSQL;
import net.sf.jsqlx.Record;
import net.sf.jsqlx.Row;
import net_io.utils.Mixed;

import java.sql.SQLException;
import java.util.List;

public class BaseDao {
	/** MySQL中的text类型最大长度：65535字节，最大存储UTF-8，3字节数量：21845 **/
	public static final int MYSQL_TEXT_MAX_LENGTH = 21845;
	public static final String COMMON_LOG_DB = "LOG_DB";
	
	private String coreDsn = null;
	private String coreTable = null;
	protected BaseDao() { 	}
	public BaseDao(String dsnName, String table) {
		this.coreDsn = dsnName;
		this.coreTable = table;
	}
	protected void config(String dsn, String table) {
		this.coreDsn = dsn;
		this.coreTable = table;
	}

	/**
	 * 获取当前实例的数据库连接名
	 * @return 未配置连接时，返回null
	 */
	public String getDsnName() {
		return coreDsn;
	}

	/**
	 * 获取当前实例的表名
	 * @return 未配置表名时，返回null
	 */
	public String getTableName() {
		return coreTable;
	}
	
	/**
	 * 执行插入操作
	 * @param row Key-Value结构的Mixed对象
	 * @return 影响的行数
	 * @throws SQLException
	 */
	public int runInsert(Mixed row) throws SQLException {
		if(row.type() != Mixed.ENTITY_TYPE.MAP || row.size() <= 0) {
			throw new SQLException("The insert key-value data is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setTableList(coreTable);
		for(String key : row.keys()) {
			Mixed value = row.get(key);
			if(value.isSelfNull()) {
				psql.addFieldValue(key, "NULL", false);
			} else if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addFieldValue(key, (byte[])value.getCoreObject());
			} else {
				psql.addFieldValue(key, value.toString());
			}
		}
		return DB.instance(coreDsn).runInsert(psql);
	}

	/**
	 * 批量插入操作（不执行二进制）
	 * @param rows Key-Value结构的Mixed对象
	 * @return 影响的行数
	 * @throws SQLException
	 */
	public int batchInsert(List<Mixed> rows) throws SQLException {
		if(rows == null || rows.size() == 0) {
			throw new SQLException("rows is empty.");
		}
		Mixed firstRow = rows.get(0);
		if(firstRow.type() != Mixed.ENTITY_TYPE.MAP || firstRow.size() <= 0) {
			throw new SQLException("The insert key-value data is error.");
		}
		String[] keys =  firstRow.keys();
		DB db = DB.instance(coreDsn);
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO ");
		sb.append(coreTable);
		sb.append(" (");
		for(int i=0; i<keys.length; i++) {
			if(i > 0) {
				sb.append(",");
			}
			sb.append(keys[i]);
		}
		sb.append(") VALUES ");
		for(int index=0; index<rows.size(); index++) {
			if(index > 0) {
				sb.append(",");
			}
			sb.append("(");
			Mixed row = rows.get(index);
			for(int i=0; i<keys.length; i++) {
				if(i > 0) {
					sb.append(",");
				}
				if(row.isNull(keys[i])) {
					sb.append("NULL");
				} else {
					sb.append("'");
					sb.append(db.escape(row.getString(keys[i])));
					sb.append("'");
				}
			}
			sb.append(")");
		}
		
		return db.executeUpdate(sb.toString());
	}

	/**
	 * 执行更新操作
	 * @param row Key-Value结构的Mixed对象
	 * @param where Key-Value结构的Mixed对象
	 * @return 影响的行数
	 * @throws SQLException
	 */
	public int runUpdate(Mixed row, Mixed where) throws SQLException {
		if(row.type() != Mixed.ENTITY_TYPE.MAP || row.size() <= 0) {
			throw new SQLException("The update key-value data is error.");
		}
		if(where.type() != Mixed.ENTITY_TYPE.MAP || where.size() <= 0) {
			throw new SQLException("The condition parameter is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setTableList(coreTable);
		for(String key : row.keys()) {
			Mixed value = row.get(key);
			if(value.isSelfNull()) {
				psql.addFieldValue(key, "NULL", false);
			} else if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addFieldValue(key, (byte[])value.getCoreObject());
			} else {
				psql.addFieldValue(key, value.toString());
			}
		}
		for(String key : where.keys()) {
			Mixed value = where.get(key);
			if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addCondition(key, (byte[])value.getCoreObject());
			} else {
				psql.addCondition(key, value.toString());
			}
		}
		return DB.instance(coreDsn).runUpdate(psql);
	}

	/**
	 * 执行更新操作
	 * @param row Key-Value结构的Mixed对象
	 * @param conditionField where条件字段名
	 * @param conditionValue where条件字段值
	 * @return 影响的行数
	 * @throws SQLException
	 */
	public int runUpdate(Mixed row, String conditionField, String conditionValue) throws SQLException {
		if(row.type() != Mixed.ENTITY_TYPE.MAP || row.size() <= 0) {
			throw new SQLException("The update key-value data is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setTableList(coreTable);
		for(String key : row.keys()) {
			Mixed value = row.get(key);
			if(value.isSelfNull()) {
				psql.addFieldValue(key, "NULL", false);
			} else if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addFieldValue(key, (byte[])value.getCoreObject());
			} else {
				psql.addFieldValue(key, value.toString());
			}
		}
		psql.addCondition(conditionField, conditionValue);
		return DB.instance(coreDsn).runUpdate(psql);
	}

	/**
	 * 执行删除操作
	 * @param where Key-Value结构的Mixed对象
	 * @return 影响的行数
	 * @throws SQLException
	 */
	public int runDelete(Mixed where) throws SQLException {
		if(where.type() != Mixed.ENTITY_TYPE.MAP || where.size() <= 0) {
			throw new SQLException("The condition parameter is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setTableList(coreTable);
		for(String key : where.keys()) {
			Mixed value = where.get(key);
			if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addCondition(key, (byte[])value.getCoreObject());
			} else {
				psql.addCondition(key, value.toString());
			}
		}
		return DB.instance(coreDsn).runDelete(psql);		
	}

	public Row[] getAll(Mixed where) throws SQLException {
		return getAll("*", where);
	}
	
	public Row[] getAll(String fields, Mixed where) throws SQLException {
		if(where.type() != Mixed.ENTITY_TYPE.MAP || where.size() <= 0) {
			throw new SQLException("The condition parameter is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setFieldList(fields);
		psql.setTableList(coreTable);
		for(String key : where.keys()) {
			Mixed value = where.get(key);
			if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addCondition(key, (byte[])value.getCoreObject());
			} else {
				psql.addCondition(key, value.toString());
			}
		}
		return DB.instance(coreDsn).getAll(psql);		
	}
	
	public Row getRow(String conditionField, String conditionValue) throws SQLException {
		return getRow("*", conditionField, conditionValue);
	}
	
	public Row getRow(String selectFields, String conditionField, String conditionValue) throws SQLException {
		PrepareSQL psql = new PrepareSQL();
		psql.setFieldList(selectFields);
		psql.setTableList(coreTable);
		psql.addCondition(conditionField, conditionValue);
		return DB.instance(coreDsn).getRow(psql);
	}
	
	public Row getRow(Mixed where) throws SQLException {
		return getRow("*", where);
	}
	
	public Row getRow(String fields, Mixed where) throws SQLException {
		if(where.type() != Mixed.ENTITY_TYPE.MAP || where.size() <= 0) {
			throw new SQLException("The condition parameter is error.");
		}
		PrepareSQL psql = new PrepareSQL();
		psql.setFieldList(fields);
		psql.setTableList(coreTable);
		for(String key : where.keys()) {
			Mixed value = where.get(key);
			if(value.type() == Mixed.ENTITY_TYPE.BYTES) {
				psql.addCondition(key, (byte[])value.getCoreObject());
			} else {
				psql.addCondition(key, value.toString());
			}
		}
		psql.setLimit(1, 0);
		return DB.instance(coreDsn).getRow(psql);		
	}
	public String getOne(String field, Mixed where) throws SQLException {
		Row row = getRow(field, where);
		if(row == null) {
			return null;
		}
		return row.getString(field);
	}

	/**
	 * 检查数据行是否存在
	 * @param where {feildName : fieldValue}
	 * @return boolean
	 * @throws SQLException
	 */
	public boolean isExist(Mixed where) throws SQLException {
		Row row = getRow("'1' AS 'num'", where);
		if(row != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String getCreateTable(DB db, String tableName) throws SQLException {
		String sql = "SHOW CREATE TABLE `"+db.escape(tableName)+"`";
		Record row = db.getRow(sql);
		if(row == null) {
			return null;
		}
		String str = row.getString("Create Table");
		return str;
	}
	
}
