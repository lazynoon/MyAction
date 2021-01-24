package myaction.plog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import myaction.extend.AppConfig;
import myaction.utils.DateUtil;
import myaction.utils.StringUtil;
import net_io.myaction.CheckException;
import net_io.utils.Mixed;

abstract public class LogCsvFile extends LogRowSet {
	// LOG记录表的模式
	public enum MODE {
		DIRECT, //直接保存到表
		SPLIT_DAY, //按天分表
		SPLIT_MONTH, //按月分表
		SPLIT_YEAR, //按年分表
	}
	private static ConcurrentHashMap<String, MyLog> logObjectPool = new ConcurrentHashMap<String, MyLog>();
	private static Object single = new Object();
	
	/** 保存数据 **/
	abstract protected void save(List<LogRow> data) throws Exception;

	
	/**
	 * 异步记录日志到DB中
	 * @param tplFileName 模版文件名
	 * @param saveMode 保存模式（参考MODE）
	 * @param logInfo 保存的信息
	 * @throws CheckException
	 */
	public static void log(String tplFileName, MODE saveMode, Mixed logInfo) throws CheckException {
		if(tplFileName == null || (!tplFileName.endsWith(".csv") && !tplFileName.endsWith(".log"))) {
			throw new CheckException(510, "LOG template filename("+tplFileName+") postfix error.");
		}
		MyLog logObj = logObjectPool.get(tplFileName);
		if(logObj == null) {
			synchronized(single) {
				logObj = logObjectPool.get(tplFileName);
				if(logObj == null) {
					logObj = new MyLog(tplFileName, saveMode);
					logObjectPool.put(tplFileName, logObj);
					AppConfig.logService.addRowSet(logObj);
				}
			}
		}
		logObj.addLogRow(logInfo);
	}
		
	@Override
	protected int runOnce() {
		List<LogRow> data = this.reset();
		try {
			this.save(data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data.size();
	}
	

	private static class MyLog extends LogCsvFile {
		
		private String tplFileName;
		private MODE saveMode;
		private String lastFilename = null;
		private String[] fieldNames = null;
		private Map<String, Boolean> existFields = null;
		
		
				
		protected MyLog(String tplFileName, MODE saveMode) {
			this.tplFileName = tplFileName;
			this.saveMode = saveMode;
		}
		
		
		@Override
		protected void save(List<LogRow> data) throws Exception {
			if(data.size() == 0) {
				return;
			}
			LogRow firstRow = data.get(0);
			//获取文件名，并且自动创建目录
			String filename = getFieldName();
			File file = new File(filename);
			if(file.exists() == false) {
				String parent = file.getParent();
				if(parent != null) {
					File path = new File(parent);
					if(path.exists() == false) {
						path.mkdirs();
					}
				}
			}
			//创建写文件对象
			PrintStream writer = new PrintStream(new FileOutputStream(file, true), true, "UTF-8");
			//获取字段名
			if(filename.equals(lastFilename) == false) {
				fieldNames = null;
				existFields = null;
			}
			if(fieldNames == null) {
				fieldNames = getFieldNames(file);
				existFields = convertExistNames(fieldNames);
			}
			if(fieldNames == null) {
				fieldNames = getFieldNames(firstRow);
				existFields = convertExistNames(fieldNames);
				//保存头部
				StringBuffer head = new StringBuffer();
				int count = 0;
				for(String name : fieldNames) {
					if(count > 0) {
						head.append('\t');
					}
					count++;
					head.append(name);
				}
				writer.println(head.toString());
			}
			try {
				for(LogRow row : data) {
					String line = formatLine(fieldNames, existFields, row.getData());
					writer.println(line);
				}
			} finally {
				writer.close();
			}
		}
		
		private String formatLine(String[] names, Map<String, Boolean> exists, Map<String, String>values) {
			StringBuffer sb = new StringBuffer();
			int count = 0;
			for(String name : names) {
				String val = values.get(name);
				if(val == null) {
					val = "";
				} else {
					val = val.replace("\t", "\\t");
					val = val.replace("\r", "\\r");
					val = val.replace("\n", "\\n");
				}
				if(count > 0) {
					sb.append('\t');
				}
				count++;
				sb.append(val);
			}
			for(String name : values.keySet()) {
				String val = values.get(name);
				if(exists.containsKey(name)) {
					continue;
				}
				if(count > 0) {
					sb.append('\t');
				}
				count++;
				sb.append(val);
			}
			return sb.toString();
		}
		
		private String getFieldName() {
			if(saveMode == MODE.DIRECT) {
				return tplFileName;
			}
			int pos = tplFileName.lastIndexOf('.');
			String name1 = tplFileName;
			String name2 = "";
			if(pos >= 0) {
				name1 = tplFileName.substring(0, pos);
				name2 = tplFileName.substring(pos);
			}
			Date date = new Date();
			if(saveMode == MODE.SPLIT_YEAR) {
				name1 += DateUtil.formatLikePHP(date, "Y");
			} else if(saveMode == MODE.SPLIT_MONTH) {
				name1 += DateUtil.formatLikePHP(date, "Ym");
			} else if(saveMode == MODE.SPLIT_DAY) {
				name1 += DateUtil.formatLikePHP(date, "Ymd");
			} else {
				//不支持的类型，保存到模版表中
			}
			return name1 + name2;
		}

		
		private String[] getFieldNames(File file) throws IOException {
			if(file.exists() == false) {
				return null;
			}
			InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");
			BufferedReader reader = new BufferedReader(read);
			try {
				String line = reader.readLine();
				if(line == null) {
					return null;
				}
				line = line.trim();
				if(line.length() == 0) {
					return null;
				}
				return StringUtil.split(line, '\t');
			} finally {
				reader.close();
			}
		}
		
		private String[] getFieldNames(LogRow row) {
			LinkedHashMap<String, String> data = row.getData();
			String[] names = new String[data.size()];
			int offset = 0;
			for(String name : data.keySet()) {
				names[offset++] = name;
			}
			return names;
		}
		
		private Map<String, Boolean> convertExistNames(String[] names) {
			if(names == null) {
				return null;
			}
			HashMap<String, Boolean> map = new HashMap<String, Boolean>();
			for(String name : names) {
				map.put(name, true);
			}
			return map;
		}
	}
}
