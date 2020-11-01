package myaction.extend;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import myaction.utils.HttpUtil;
import myaction.utils.LogUtil;
import myaction.utils.SortUtil;
import net_io.core.NetChannel;
import net_io.core.StatNIO;
import net_io.myaction.MyActionClient;
import net_io.myaction.Response;
import net_io.myaction.server.CommandMsg;
import net_io.myaction.socket.MyActionSocket;
import net_io.myaction.socket.SocketRequest;
import net_io.myaction.tool.DSN;
import net_io.utils.EncodeUtils;
import net_io.utils.JSONException;
import net_io.utils.JSONUtils;
import net_io.utils.Mixed;
import net_io.utils.MixedUtils;


public class MiddlewareClient {
	private static AtomicLong newInstanceCount = new AtomicLong(0);
	private static AtomicLong getInstanceCount = new AtomicLong(0);
	private static AtomicLong httpRequestCount = new AtomicLong(0);
	private static Map<DSN, MiddlewareClient> clientPool = new ConcurrentHashMap<DSN, MiddlewareClient>();
	private static MyActionSocket asyncSocket = AppConfig.getDefaultActionSocket();
	private boolean isHttpClient = false;
	private MyActionClient asyncClient = null;
	private DSN dsn;
	private String rootPath;

	private MiddlewareClient() {
		newInstanceCount.incrementAndGet();
	}

	/**
	 * 创建中间件客户端对象
	 * @param dsn 连接定义字符串或配置名称
	 * @return MiddlewareClient
	 * @throws IOException
	 */
	public static MiddlewareClient instance(String dsn) throws IOException {
		if(dsn == null) {
			throw new IllegalArgumentException("Dsn name is null");
		}
		DSN obj;
		if(dsn.indexOf("://") > 0) {
			obj = DSN.parse(dsn);
		} else {
			obj = AppConfig.getConnectDSN(dsn);
		}
		if(obj == null) {
			throw new IOException("The dsn link name is not defined - "+dsn);
		}
		return instance(obj);
	}

	public static MiddlewareClient instance(DSN dsn) throws IOException {
		getInstanceCount.incrementAndGet();
		MiddlewareClient client = clientPool.get(dsn);
		if(client == null) {
			client = new MiddlewareClient();
			client.dsn = dsn;
			client.rootPath = dsn.getPath();
			if(client.rootPath.endsWith("/") == false) {
				client.rootPath += "/";
			}
			if("http".equals(dsn.getScheme()) || "https".equals(dsn.getScheme())) {
				client.isHttpClient = true;
			} else {
				client.isHttpClient = false;
				client.asyncClient = new MyActionClient(asyncSocket);
			}
			clientPool.put(dsn, client);
		}
		if(client.isHttpClient == false) {
			client.connect();
		}
		return client;
	}

	private void connect() throws IOException {
		if(asyncClient.needConnect()) {
			synchronized(this) {
				if(asyncClient.needConnect()) {
					asyncClient.connect(dsn.getHost(), dsn.getPort());
				}
			}
		}
	}

	public void closeNetChannel() {
		if(asyncClient != null) {
			asyncClient.close();
		}
	}
	
	public NetChannel getNetChannel() {
		if(asyncClient == null) {
			return null;
		}
		return asyncClient.getNetChannel();
	}
	
	public Mixed request(String path, Map<String, String> params, int timeout) throws IOException {
		return request(path, new Mixed(params), timeout);
	}

	/**
	 * 请求API
	 * @param path
	 * @param params
	 * @param timeout 单位：毫秒
	 * @return 非空 {error: 错误码, reason: 错误原因, data: 返回数据}
	 * @throws IOException
	 */
	public Mixed request(String path, Mixed params, long timeout) throws IOException {
		setCommonParams(params, dsn.getUser(), dsn.getPasswd()); //处理共通参数
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		path = rootPath + path;
		Mixed result;
		if(isHttpClient) {
			result = httpRequest(dsn, path, params, timeout);
		} else {
			int requestID = asyncClient.sendRequest(path, params);
			result = asyncClient.readResult(requestID, timeout);
			if(result == null) {
				result = new Mixed();
				result.put("error", 31);
				result.put("reason", "The response with result is null");
			} else if(result.containsKey("error") == false) {
				result.put("error", 32);
				result.put("reason", "The result not contains error key");
			}
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
		params.set("g_time", System.currentTimeMillis() / StatNIO.ONE_THOUSAND_LONG);
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
			String sign = EncodeUtils.md5(build.toString(), "UTF-8");
			params.put("g_sign", sign);
		}
	}

	/** Http 请求 API **/
	public Mixed httpRequest(DSN dsn, String fullPath, Mixed params, long timeout) throws IOException {
		httpRequestCount.incrementAndGet();
		String url = dsn.getScheme() + "://" + dsn.getHost();
		if(dsn.getPort() != 0) {
			if(("http".equalsIgnoreCase(dsn.getScheme()) && dsn.getPort() != 80)
					|| ("https".equalsIgnoreCase(dsn.getScheme()) && dsn.getPort() != 443)) {
				url += ":" + dsn.getPort();
			}
		}
		url += fullPath;
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		if(params.size() > 0) {
			for(String key : params.keys()) {
				map.put(key, params.getString(key));
			}
		}
		String content = HttpUtil.doPost(url, map);
		if(content == null || content.length() == 0) {
			throw new IOException("Return empty data");
		}
		if(!content.startsWith("{") || !content.endsWith("}")) {
			throw new IOException("Return data is not JSON format");
		}
		try {
			return JSONUtils.parseJSON(content);
		} catch (JSONException e) {
			throw new IOException(e.toString());
		}
	}

	public static long getNewInstanceCount() {
		return newInstanceCount.get();
	}

	public static long getGetInstanceCount() {
		return getInstanceCount.get();
	}

	public static long getHttpRequestCount() {
		return httpRequestCount.get();
	}


}
