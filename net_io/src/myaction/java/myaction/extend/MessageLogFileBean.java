package myaction.extend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import myaction.utils.MessageLogUtil;
import net_io.utils.DateUtils;

public class MessageLogFileBean {
	private static String currentDate = null;
	
	public static void updateWriter() {
		if(DateUtils.format(new Date(), "yyyyMMdd").equals(currentDate)) {
			return;
		}
		currentDate = DateUtils.format(new Date(), "yyyyMMdd");
		String logFileName = "msg_" + currentDate + ".log";
		File logFile = new File(AppConfig.getLogDir() +"msg/"+ logFileName);
		try {
			if (logFile.exists() == false) {
				logFile.createNewFile();
			}
			PrintWriter writer = new PrintWriter(new FileWriter(logFile, true));
			MessageLogUtil.setPrintWriter(writer);
		} catch (IOException e) {
			System.err.println(DateUtils.getDateTime() + " update message log writer error.");
			e.printStackTrace();
		}

	}

}
