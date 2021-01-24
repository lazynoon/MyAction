package myaction.action.monitor;

import java.util.Map;

import myaction.action.MyMonitorBaseAction;
import net.sf.jsqlx.JsqlxStat;
import net_io.core.StatNIO;

public class JsqlxAction extends MyMonitorBaseAction {

	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}
		
	}

	public void doIndex() throws Exception {
		response.println("<PRE>");
		Map<String, Long> stat = JsqlxStat.getStat();
		//response.println("\r\n------------------------------------\r\n");
		response.println("<strong>jsqlx stat</strong>");
		for(String key : JsqlxStat.getFields()) {
			String name = JsqlxStat.getChineseName(key);
			response.println(name+": "+stat.get(key));
		}
		response.println("</PRE>");

	}
	
	public void doSocket() throws Exception {
		response.println("<PRE>");
		Map<String, Long> stat = StatNIO.bossClass.getStat();
		//response.println("\r\n------------------------------------\r\n");
		response.println("<strong>socket stat</strong>");
		for(String key : StatNIO.bossClass.getFields()) {
			String name = StatNIO.bossClass.getChineseName(key);
			response.println(name+": "+stat.get(key));
		}

		stat = StatNIO.packetStat.getStat();
		response.println("\r\n----------------  pactket socket stat  --------------------\r\n");
		response.println("<strong>packet socket stat</strong>");
		for(String key : StatNIO.packetStat.getFields()) {
			String name = StatNIO.packetStat.getChineseName(key);
			response.println(name+": "+stat.get(key));
		}

		stat = StatNIO.streamStat.getStat();
		response.println("\r\n----------------  stream socket stat  --------------------\r\n");
		response.println("<strong>stream socket stat</strong>");
		for(String key : StatNIO.streamStat.getFields()) {
			String name = StatNIO.streamStat.getChineseName(key);
			response.println(name+": "+stat.get(key));
		}

		stat = StatNIO.bufferPoolStat.getStat();
		response.println("\r\n----------------  byte buffer pool stat  --------------------\r\n");
		response.println("<strong>byte buffer pool stat</strong>");
		for(String key : StatNIO.bufferPoolStat.getFields()) {
			String name = StatNIO.bufferPoolStat.getChineseName(key);
			response.println(name+": "+stat.get(key));
		}
		response.println("\r\n");
		response.println("-------------------------------------------------------\r\n");
		response.println("\r\n");
		//结束符
		response.println("</PRE>");
	}
	
}
