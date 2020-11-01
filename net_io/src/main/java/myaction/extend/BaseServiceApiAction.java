package myaction.extend;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import myaction.plog.LogCsvFile;
import myaction.utils.DateUtil;
import net.sf.jsqlx.JSQLException;
import net_io.core.StatNIO;
import net_io.myaction.CheckException;
import net_io.myaction.Request;
import net_io.myaction.Response.MODE;
import net_io.utils.DateUtils;
import net_io.utils.JSONUtils;
import net_io.utils.Mixed;
import net_io.utils.MixedUtils;

/**
 * 所有 Action 的基类
 * 进行共通的权限检查
 * @author Hansen
 */
abstract public class BaseServiceApiAction extends BaseServiceAction {
	protected String glbAppID = null;
	protected int glbUserID = 0;
	protected String glbAccount = null;
	protected long glbClientTime = 0;
	protected String glbClientIP = null;
	protected String glbAgentID = null;
	protected String glbClickID = null;
	protected String glbFromURL = null;
	protected Mixed args = new Mixed();
		
	/**
	 * 权限验证
	 * 参数:
	 *   g_platform 调用中间件的平台。系统配置
	 *   g_uid 用户ID。可为空，默认为0
	 *   g_accout 帐号名，默认为空
	 *   g_agent 浏览器ID(agent_id)，不为空
	 *   g_ip 客户端IP地址，可为空
	 *   g_time 接口调用时间戳，北京时间，单位 秒。请求时间必须在前后10分钟内
	 *   g_sign 签名。默认签名：g_platform + g_agent + g_time + {secret_key}
	 */
	@Override
	final public void beforeExecute() throws Exception {
		super.beforeExecute();
		response.setMode(MODE.JSON); //设置接口的返回，均已JSON格式
		glbAppID = request.getParameter("g_appid");
		if(MixedUtils.isEmpty(glbAppID)) {
			glbAppID = request.getParameter("g_platform"); //兼容旧版（20180910）
		}
		glbUserID = MixedUtils.parseInt(request.getParameter("g_uid")); //可为空，不做签名验证
		glbAccount = request.getParameter("g_account");
		glbAgentID = request.getParameter("g_agent");
		String sTime = request.getParameter("g_time");
		glbClientIP = request.getParameter("g_ip");
		if(glbClientIP == null) {
			glbClientIP = "";
		}
		glbClickID = request.getParameter("g_click");
		if(glbClickID == null) {
			glbClickID = "";
		}
		glbFromURL = request.getParameter("g_fromurl");
		if(glbFromURL == null) {
			glbFromURL = "";
		}
		glbClientTime = MixedUtils.parseLong(sTime) * StatNIO.ONE_THOUSAND_LONG;
		ThreadSessionPool.setCommonParams(this.getCommonParams()); //设置Thread级别的session配置
		// 除 g_appid, g_time, g_sign 之外的非空检查
		if(MixedUtils.isEmpty(glbAgentID)) {
			throw new CheckException(602, "The parameter of 'g_agent' is empty.");
		}
		String appKey = getAppKey(glbAppID);
		if(appKey == null || appKey.length() < 6) {
			throw new CheckException(613, "The AppID("+glbAppID+") is not exists.");
		}
		//验证签名
		HashMap<String, String> params = new HashMap<String, String>();
		for(String name : request.getParameterNames()) {
			params.put(name, request.getParameter(name));
		}
		checkApiSign(params, appKey);
		//提取业务参数
		String strArgs = request.getParameter("g_args");
		if(MixedUtils.isEmpty(strArgs) == false) {
			Mixed tmpArgs = JSONUtils.parseJSON(strArgs);
			if(tmpArgs != null) {
				args = tmpArgs;
			}
		}
		//在执行Action前调用
		beforeAction();
	}
	
	public void beforeAction() throws Exception {}
	
	final public void afterExecute() throws Exception {
		//MessageLogFileBean.updateWriter(); //更新消息系统消息记录文件地址
		Exception lastE = this.getLastActionException();
		if(lastE != null) {
			if(lastE instanceof JSQLException) {
				String lastSQL = ((JSQLException)lastE).getRunSQL();
				System.err.println(DateUtil.getDateTime()+" - Exception SQL: "+lastSQL);
			}
		}
		try {
			super.afterExecute();
		} finally {
			ThreadSessionPool.removeCommonParams(); //移除Thread级别的session配置
			this.logStat(request, response.getError());
		}
	}
	
	protected String getAppKey(String appID) {
		return AppConfig.getAppKey(appID);
	}
	
	private void logStat(Request request, int error) {
		Date requestDate = new Date(request.getStartTime());
		long costTime = System.currentTimeMillis() - requestDate.getTime();
		//long waitTime = glbStartDate.getTime() - requestDate.getTime();
		String ip = request.getClientIP();
		if(!ip.equals(request.getRemoteIP())) {
			ip += "/" + request.getRemoteIP();
		}
		String logFile = AppConfig.getLogDir()+"visit/visit_.log";
		Mixed logInfo = new Mixed();
		logInfo.put("AgentID", glbAgentID);
		logInfo.put("ClickID", glbClickID);
		logInfo.put("VisitTime", DateUtils.getDateTime(requestDate));
		logInfo.put("CostTime", (costTime/1000.0));
		logInfo.put("ErrorCode", error);
		logInfo.put("IP", ip);
		logInfo.put("URI", request.getPath());
		logInfo.put("AppID", glbAppID);
		logInfo.put("Account", glbAccount);
		logInfo.put("ClientIP", glbClientIP);
		try {
			LogCsvFile.log(logFile, LogCsvFile.MODE.SPLIT_DAY, logInfo);
		} catch(Exception e) {
			System.err.println("Write Visit Log Error: "+e+". "+logInfo.toJSON());
		}
	}
	
	/**
	 * 保存基础日志参数（用于重要接口请求）
	 * @return Mixed
	 */
	protected Map<String, Object> createLogParams() {
		Map<String, Object> logInfo = new LinkedHashMap<String, Object>();
		logInfo.put("log_id", this.getPageVisitId());
		logInfo.put("platform", glbAppID);
		logInfo.put("agent_id", glbAgentID);
		logInfo.put("click_id", glbClickID);
		logInfo.put("client_ip", glbClientIP);
		logInfo.put("from_url", glbFromURL);
		logInfo.put("offset_time", System.currentTimeMillis() - request.getStartTime());
		logInfo.put("create_time", DateUtil.getDateTime());
		return logInfo;
	}
	
	/**
	 * 获取共通参数
	 * @return 非空 CommonParams
	 */
	protected CommonParams getCommonParams() {
		CommonParams params = new CommonParams();
		params.pageVisitId = this.getPageVisitId();
		params.startTime = this.request.getStartTime();
		params.appID = glbAppID;
		params.userID = glbUserID;
		params.account = glbAccount;
		params.clientTime = glbClientTime;
		params.clientIP = glbClientIP;
		params.agentID = glbAgentID;
		params.clickID = glbClickID;
		params.fromURL = glbFromURL;
		params.args = args;
		return params;
	}

}
