package myaction.extend;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import myaction.utils.DateUtil;
import myaction.utils.MD5Util;
import myaction.utils.SortUtil;
import net.sf.jsqlx.DB;
import net.sf.jsqlx.JSQLException;
import net_io.core.StatNIO;
import net_io.myaction.BaseMyAction;
import net_io.myaction.CheckException;
import net_io.utils.EncodeUtils;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

abstract public class BaseServiceAction extends BaseMyAction {
	//签名过期时间：10分钟
	private static final long SIGN_EXPIRE_TIME = 600 * StatNIO.ONE_THOUSAND_LONG;
	private static AtomicLong sRunCount = new AtomicLong(0);
	private static AtomicLong sRunUsTime = new AtomicLong(0);
	private String glbVisitId = EncodeUtils.createTimeRandId(); //创建唯一的日志ID
	
	/** 获取运行次数 **/
	public static long getTotalRunCount() { return sRunCount.get(); }
	/** 获取运行时间（单位：毫秒） **/
	public static long getTotalRunTime() { return sRunUsTime.get() / StatNIO.ONE_THOUSAND_LONG; }
	/** 获取运行时间 **/
	public String getPageVisitId() { return glbVisitId; }
	
	public BaseServiceAction() {
		sRunCount.incrementAndGet(); //运行次数总计
	}

	/**
	 * 获取参与签名的字段名（参与签名字段不允许为空）
	 * @return 返回null或空数组，则按默认校验签名
	 */
	protected String[] getSignFieldNames() {
		return null;
	}
	/** Action执行前过滤器（支持异常，并中断进程） **/
	protected void filterStart() throws Exception {}
	/** Action执行后过滤器（若有异常，不影响主进程） **/
	protected void filterEnd() {}
	

	/**
	 * 权限验证
	 * 参数：
	 *   g_platform 调用中间件的平台。系统配置
	 *   g_uid 用户ID。可为空，默认为0
	 *   g_accout 帐号名，默认为空
	 *   g_agent 浏览器ID(agent_id)，不为空
	 *   g_ip 客户端IP地址，可为空
	 *   g_time 接口调用时间戳，北京时间，单位 秒。请求时间必须在前后10分钟内
	 *   g_sign 签名。默认签名：g_platform + g_agent + g_time + {secret_key}
	 */
	@Override
	protected void beforeExecute() throws Exception {
		filterStart(); //执行前过滤器
	}
	
	@Override
	final public void defaultExecute(String methodName) throws Exception {
		service(methodName);
	}

	@Override
	protected void afterExecute() throws Exception {
		//MessageLogFileBean.updateWriter(); //更新消息系统消息记录文件地址
		try {
			filterEnd(); //执行后过滤器
		} catch(Exception e) {
			NetLog.logError(e);
		} finally {
			super.afterExecute();
			Exception lastE = this.getLastActionException();
			if(lastE != null) {
				if(lastE instanceof JSQLException) {
					String lastSQL = ((JSQLException)lastE).getRunSQL();
					System.err.println(DateUtil.getDateTime()+" - Exception SQL: "+lastSQL);
				}
			}
			//回收连接资源
			DB.forceReleaseCurrentInstance();
			long costNsTime = System.nanoTime() - request.getStartNanoTime();
			sRunUsTime.addAndGet(costNsTime / StatNIO.ONE_THOUSAND_LONG); //运行时间总计
		}
	}

	
	
	abstract public void service(String methodName) throws Exception;
	
	protected void throwNotFoundException(String methodName) throws CheckException {
		throw new CheckException(404, "Not Found Method: "+methodName);
	}
	
	/**
	 * 检查API请求的签名
	 * 		必要字段：g_appid, g_time, g_sign
	 * @param params
	 */
	protected void checkApiSign(Map<String, String> params, String appKey) throws CheckException {
		String appID = params.get("g_appid");
		String sTime = params.get("g_time");
		String sign = params.get("g_sign");
		boolean checkOldSign = false;
		if(MixedUtils.isEmpty(appID)) {
			appID = params.get("g_platform"); //兼容旧版（20180910）
			checkOldSign = true;
		}
		if(MixedUtils.isEmpty(appID)) {
			throw new CheckException(621, "The parameter of 'g_appid' is empty.");
		}
		if(MixedUtils.isEmpty(sTime)) {
			throw new CheckException(622, "The parameter of 'g_time' is empty.");
		}
		if(MixedUtils.isEmpty(sign)) {
			throw new CheckException(623, "The parameter of 'g_sign' is empty.");
		}
		long glbClientTime = MixedUtils.parseLong(sTime) * StatNIO.ONE_THOUSAND_LONG;
		//当前时间
		long curTime = new Date().getTime();
		if(curTime-glbClientTime > SIGN_EXPIRE_TIME || glbClientTime-curTime > SIGN_EXPIRE_TIME) {
			throw new CheckException(624, "The sign had been expire.");
		}
		//验证签名
		String[] signNames = getSignFieldNames();
		if (signNames != null && signNames.length > 0) {
			for (String name : signNames) {
				if (MixedUtils.isEmpty(params.get(name))) {
					throw new CheckException(625, "The parameter of '" + name + "' is empty.");
				}
			}
		} else {
			signNames = new String[params.size()];
			int index = 0;
			for (String name : params.keySet()) {
				signNames[index++] = name;
			}
			signNames = SortUtil.sort(signNames, SortUtil.ASC);
		}
		StringBuilder build = new StringBuilder();
		for(String name : signNames) {
			if("g_sign".equals(name)) {
				continue;
			}
			build.append(params.get(name));
		}
		build.append(appKey);			
		String rightSign = EncodeUtils.md5(build.toString());
		if(sign.equals(rightSign) == false) {
			if(checkOldSign) {
				String agentID = params.get("g_agent");
				if(MixedUtils.isEmpty(agentID)) {
					throw new CheckException(625, "The parameter of 'g_agent' is empty.");
				}
				build = new StringBuilder();
				build.append(appID);
				build.append(agentID);
				build.append(sTime);
				if(params.containsKey("g_args")) {
					build.append(params.get("g_args"));			
				}
				build.append(appKey);
				String rightOldSign = MD5Util.md5(build.toString());
				if(sign.equals(rightOldSign) == false) {
					if(params.containsKey("args")) {
						build.append(params.get("args"));
						rightOldSign = MD5Util.md5(build.toString());
						if(sign.equals(rightOldSign) == false) {
							throw new CheckException(626, "The sign not match.");
						}
					} else {
						throw new CheckException(627, "The sign not match.");						
					}
				}
			} else {
				throw new CheckException(628, "The sign not match.");
			}
		}
	
	}
}
