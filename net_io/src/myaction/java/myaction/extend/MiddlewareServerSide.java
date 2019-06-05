package myaction.extend;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import myaction.extend.AppConfig;
import myaction.extend.MiddlewareClient;
import net_io.core.NetChannel;
import net_io.myaction.MyActionClient;
import net_io.utils.Mixed;

public abstract class MiddlewareServerSide {
	private String appID = null;
	private String appKey = null;
	private NetChannel channel = null;
	private MyActionClient client = null;
	private String rootPath = null;
	private long loginTime = System.currentTimeMillis();
	private long lastActiveTime = System.currentTimeMillis();
	private AtomicLong requestCount = new AtomicLong(0);
	private AtomicLong responseCount = new AtomicLong(0);
	
	public MiddlewareServerSide(String appID, String appKey, NetChannel channel, String rootPath) {
		this.appID = appID;
		this.appKey = appKey;
		this.channel = channel;
		this.client = new MyActionClient(AppConfig.getDefaultActionSocket(), channel);
		this.rootPath = rootPath;
	}

	public String getAppID() {
		return appID;
	}

	public String getAppKey() {
		return appKey;
	}

	public NetChannel getChannel() {
		return channel;
	}
	
	public int sendRequest(String path, Mixed args) throws IOException {
		Mixed params = new Mixed();
		params.put("g_args", args);
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		path = rootPath + path;
		return _sendRequest(path, params, appID, appKey);
	}
	
	protected int _sendRequest(String path, Mixed params, String appID, String appKey) throws IOException {
		MiddlewareClient.setCommonParams(params, appID, appKey); //处理共通参数
		int requestID = client.sendRequest(path, params);
		lastActiveTime = System.currentTimeMillis(); //激活时间
		return requestID;		
	}

	/**
	 * 根据 requestID 读取返回的结果
	 * @param requestID sendRequest返回的请求ID
	 * @return 找不到返回 null
	  */
	public Mixed readResult(int requestID) throws IOException {
		return client.readResult(requestID);
	}

	/**
	 * 根据 requestID 读取返回的结果
	 * @param requestID sendRequest返回的请求ID
	 * @param timeout 等待超时时间（单位：ms）
	 * @return 等待超时，则抛出异常
	  */
	public Mixed readResult(int requestID, long timeout) throws IOException {
		return client.readResult(requestID, timeout);
	}
	
	/** 获取登录（连接）时间 **/
	public long getLoginTime() {
		return loginTime;
	}
	
	/** 获取最后活动时间 **/
	public long getLastActiveTime() {
		return lastActiveTime;
	}

	/** 获取请求次数 **/
	public long getRequestCount() {
		return requestCount.get();
	}
	
	/** 获取响应次数（被请求） **/
	public long getResponseCount() {
		return responseCount.get();
	}
	
	/** 响应次数加一并返回（同时更新最后活动时间） **/
	public long incrementResponseCount() {
		lastActiveTime = System.currentTimeMillis();
		return responseCount.incrementAndGet();
	}
	
	
}
