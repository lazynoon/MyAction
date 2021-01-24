package myaction.extend;

import java.util.HashMap;
import java.util.Map;

public class ErrorConstant {
	//HTTP ERROR
	public static int FORBIDDEN = 403;
	public static int PAGE_NOT_FOUND = 404;
	public static int INTERNAL_SERVER_ERROR = 500;
	//共通错误
	public static int UNDEFINED_ERROR = 1001;
	public static int URI_FORMAT_ERROR = 1002;
	/**POST表单超长*/
	public static int POST_LENGTH = 1003; 
	/**未定义的输入参数检查错误*/
	public static int UNKNOWN_INPUT_ERROR = 1004; 
	/**参数缺失*/
	public static int LOSE_PARAMETER = 1101; 
	/**参数类型错误*/
	public static int PARAMETER_TYPE_ERROR = 1102; 
	/**参数检查不通过*/
	public static int PARAMETER_CHECK_ERROR = 1108;
	/**游戏服务器不存在或未激活*/
	public static int SERVER_NOT_ACTIVE = 1103;
	/**签名错误*/
	public static int SIGN_ERROR = 1104;
	/**签名时间过期*/
	public static int SIGN_TIME_EXPIRED = 1105;
	/**IP受限*/
	public static int IP_LIMIT = 1106;
	/**请求的游戏不存在*/
	public static int GAME_NOT_EXIST = 1107;
	//平台接口应用层错误
	/**充值失败*/
	public static int RECHARGE_ERROR = 2001;
	/**订单号重复，但充值金额不同*/
	public static int ORDER_ID_DUPLICATE = 2002;
	/**订单号重复，且已成功充值*/
	public static int ALREADY_RECHARGE = 2003;
	/**服务器暂停服务中*/
	public static int SERVER_PAUSE_ERROR = 2011;
	/**该帐号下，没有激活的角色*/
	public static int NO_ACTIVE_ROLE = 2021;
	/**角色名不存在*/
	public static int ROLE_NOT_EXIST = 2022;
	/**更新失败*/
	public static int UPDATE_ERROR = 2023;
	//后台接口应用层错误
	/**帮会不存在*/
	public static int GUILD_NOT_EXIST = 3010;
	
	
	private static Map<Integer, String> map = new HashMap<Integer, String>();
	static {
		//HTTP ERROR
		map.put(PAGE_NOT_FOUND, "Page Not Found");
		map.put(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
		map.put(FORBIDDEN, "FORBIDDEN");
		//框架解析错误
		map.put(UNDEFINED_ERROR, "UNDEFINED_ERROR");
		map.put(URI_FORMAT_ERROR, "URI_FORMAT_ERROR");
		map.put(POST_LENGTH, "Post form over max length");
		map.put(UNKNOWN_INPUT_ERROR, "Unknown error for the input parameter");
		map.put(PARAMETER_TYPE_ERROR, "Parameter type error");
		map.put(SIGN_TIME_EXPIRED, "Sign time have been expired");
		map.put(IP_LIMIT, "Your IP restricted");
		map.put(GAME_NOT_EXIST, "The game is not exists");
		//平台接口应用层错误
		map.put(RECHARGE_ERROR, "RECHARGE_ERROR");
		map.put(ALREADY_RECHARGE, "Already recharge success");
		map.put(ORDER_ID_DUPLICATE, "Same Order ID but the money|account is not equal");
		map.put(NO_ACTIVE_ROLE, "The account has not active role");
		map.put(SERVER_PAUSE_ERROR, "Server Maintenance");
		map.put(SERVER_NOT_ACTIVE, "The server not exists or not active");
		map.put(SIGN_ERROR, "Sign Error");
		map.put(ROLE_NOT_EXIST, "Role not exists.");
		map.put(UPDATE_ERROR, "Update Error.");
		//后台接口应用层错误
		map.put(GUILD_NOT_EXIST, "Guild not exists.");
	}
	
	public static String getError(int errno) {
		if(errno == 0) {
			return "";
		}
		String error = map.get(errno);
		if(error != null) {
			return error;
		} else {
			return map.get(UNDEFINED_ERROR);
		}
	}
}
