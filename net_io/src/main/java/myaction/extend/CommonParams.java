package myaction.extend;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import myaction.utils.DateUtil;
import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;

public class CommonParams {
	protected String pageVisitId = null;
	protected long startTime = 0;
	protected String appID = null;
	protected int userID = 0;
	protected String account = null;
	protected long clientTime = 0;
	protected String clientIP = null;
	protected String agentID = null;
	protected String clickID = null;
	protected String fromURL = null;
	protected Mixed args = null;
	
	public static InetAddress getLocalHostAddress() throws IOException {
		InetAddress candidateAddress = null;
		// 遍历所有的网络接口
		for(Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
			NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
			// 在所有的接口下再遍历IP
			for(Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
				InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
				if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
					if (inetAddr.isSiteLocalAddress()) {
						// 如果是site-local地址，就是它了
						return inetAddr;
					} else if (candidateAddress == null) {
						// site-local类型的地址未被发现，先记录候选地址
						candidateAddress = inetAddr;
					}
				}
			}
		}
		if (candidateAddress != null) {
			return candidateAddress;
		}
		// 如果没有发现 non-loopback地址.只能用最次选的方案
		InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
		return jdkSuppliedAddress;
	}

	public String getAppID() {
		return appID;
	}
	public int getUserID() {
		return userID;
	}
	public String getAccount() {
		if(account == null) {
			return "";
		}
		return account;
	}
	public long getClientTime() {
		return clientTime;
	}
	public String getClientIP() {
		if(clientIP == null) {
			return "";
		}
		return clientIP;
	}
	public String getAgentID() {
		if(agentID == null) {
			return "";
		}
		return agentID;
	}
	public String getClickID() {
		if(clickID == null) {
			return "";
		}
		return clickID;
	}
	public String getFromURL() {
		if(fromURL == null) {
			return "";
		}
		return fromURL;
	}
	public String getPageVisitId() {
		if(pageVisitId == null) {
			pageVisitId = EncodeUtils.createTimeRandId();
		}
		return pageVisitId;
	}
	public Mixed getArgs() {
		return args;
	}
	/**
	 * 保存基础日志参数（用于重要接口请求）
	 * @return Mixed
	 */
	public Map<String, Object> createLogParams() {
		Map<String, Object> logInfo = new LinkedHashMap<String, Object>();
		logInfo.put("log_id", getPageVisitId());
		if(appID != null) {
			logInfo.put("platform", appID);
		} else {
			logInfo.put("platform", "");
		}
		if(startTime > 0) {
			logInfo.put("offset_time", (System.currentTimeMillis()-startTime));
		} else {
			logInfo.put("offset_time", -1);
		}
		if(agentID != null) {
			logInfo.put("agent_id", agentID);
		} else {
			logInfo.put("agent_id", "");
		}
		if(clickID != null) {
			logInfo.put("click_id", clickID);
		} else {
			logInfo.put("click_id", "");
		}
		if(clientIP != null) {
			logInfo.put("client_ip", clientIP);
		} else {
			logInfo.put("client_ip", "");
		}
		if(fromURL != null) {
			logInfo.put("from_url", fromURL);			
		} else {
			logInfo.put("from_url", "");
		}
		logInfo.put("create_time", DateUtil.getDateTime());
		return logInfo;
	}

}
