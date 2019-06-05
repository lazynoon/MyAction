package myaction.extend;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import myaction.utils.SortUtil;
import net_io.core.NetChannel;
import net_io.myaction.MyActionClient;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.tool.DSN;
import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;


public class MiddlewareClient {
	private static Map<DSN, MiddlewareClient> clientPool = new ConcurrentHashMap<DSN, MiddlewareClient>();
	private static MyActionSocket asyncSocket = AppConfig.getDefaultActionSocket();
	private DSN dsn;
	private String rootPath;
	private MyActionClient client = null;
	
	private MiddlewareClient() {}
	
	public static MiddlewareClient instance(String connectName) throws IOException {
		DSN dsn = AppConfig.getConnectDSN(connectName.toLowerCase());
		if(dsn == null) {
			throw new IOException("The connect name is not defined - "+connectName);
		}
		return instance(dsn);
	}
	
	public static MiddlewareClient instance(DSN dsn) {
		MiddlewareClient obj = clientPool.get(dsn);
		if(obj == null) {
			obj = new MiddlewareClient();
			obj.dsn = dsn;
			obj.rootPath = dsn.getPath();
			if(obj.rootPath.endsWith("/") == false) {
				obj.rootPath += "/";
			}
			clientPool.put(dsn, obj);
		}
		obj.client = new MyActionClient(asyncSocket);
		return obj;
	}
	
	public boolean autoConnect() throws IOException {
		if(client.needConnect()) {
			client.connect(dsn.getHost(), dsn.getPort());
			return true;
		} else {
			return false;
		}
	}
	
	public void close() {
		client.close();
	}
	
	public NetChannel getNetChannel() {
		return client.getNetChannel();
	}
	
	public Mixed request(String path, Map<String, String> params, int timeout) throws IOException {
		return request(path, new Mixed(params), timeout);
	}
	
	
	public Mixed request(String path, Mixed params, long timeout) throws IOException {
		autoConnect(); //自动连接
		setCommonParams(params, dsn.getUser(), dsn.getPasswd()); //处理共通参数
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		path = rootPath + path;
		int requestID = client.sendRequest(path, params);
		Mixed result = client.readResult(requestID, timeout);
		if(result == null) {
			result = new Mixed();
			result.put("error", 31);
			result.put("reason", "The response with result is null");
		} else if(result.containsKey("error") == false) {
			result.put("error", 32);
			result.put("reason", "The result not contains error key");
		}
		return result;
	}
	
	public static void setCommonParams(Mixed params, String appID, String appKey) {
		//请求参数中的对象，转换为JSON编码的字符串
		for(String key : params.keys()) {
			Mixed value = params.get(key);
			Mixed.ENTITY_TYPE valueType = value.type();
			if(valueType == Mixed.ENTITY_TYPE.LIST || valueType == Mixed.ENTITY_TYPE.MAP) {
				params.put(key, value.toJSON());
			}
		}
		params.set("g_appid", appID);
		if(params.isEmpty("g_agent")) {
			params.set("g_agent", AppConfig.getRuntimeID());
		}
		if(params.isEmpty("g_click")) {
			CommonParams commonParams = ThreadSessionPool.getCommonParams();
			String clickId;
			if(commonParams != null) {
				clickId = commonParams.getPageVisitId();
			} else {
				clickId = EncodeUtils.createTimeRandId();
			}
			params.set("g_click", clickId);
		}
		params.set("g_time", System.currentTimeMillis() / 1000);
		if(params.isEmpty("g_ip")) {
			try {
				params.set("g_ip", InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				//ignore
			}
		}
		if(params.isEmpty("g_fromurl") == false) {
			String fromUrl = params.getString("g_fromurl");
			DSN urlDsn = DSN.parse(fromUrl);
			fromUrl = urlDsn.getScheme() + "://" + urlDsn.getHost();
			if(urlDsn.getPort() > 0 && urlDsn.getPort() != 80) {
				fromUrl += ":" + urlDsn.getPort();
			}
			fromUrl += urlDsn.getPath();
			if(fromUrl.length() > 200) {
				fromUrl = fromUrl.substring(0, 197) + "...";
			}
			params.put("g_fromurl", fromUrl);
		}
		//增加签名
		if(params.isEmpty("s_sign")) {
			//key排序
			String[] keys = params.keys();
			keys = SortUtil.sort(keys, SortUtil.ASC);
			StringBuilder build = new StringBuilder();
			for(String key : keys) {
				build.append(params.getString(key));
			}
			build.append(appKey);			
			String sign = EncodeUtils.md5(build.toString());
			params.put("g_sign", sign);
		}
	}
	

}
