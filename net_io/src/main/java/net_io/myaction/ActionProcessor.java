package net_io.myaction;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import net_io.myaction.ActionFactory.ActionClassMethod;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

abstract public class ActionProcessor implements Runnable {
	//按SOCKET接口运行
	public static final int MODE_SOCKET = 1;
	//按HTTP接口运行
	public static final int MODE_HTTP = 2;
	//按HTTP接口运行
	public static final int MODE_SERVLET = 3;
	//启动的毫秒时间戳
	private long startMsTime = System.currentTimeMillis();
	//启动的纳秒偏移时间
	private long startNsTime = System.nanoTime();
	//运行模式
	protected int runMode = 0;
	private Request currentRequest;
		
	abstract protected void processRequest();
	
	/**
	 * 消息处理进程
	 */
	public void run() {
		processRequest();
	}
	
	/**
	 * 底层 MyAction 框架调用
	 * @param request
	 * @param response
	 */
	protected String executeAction(Request request, Response response) {
		request.startMsTime = this.startMsTime; //注入请求时间
		request.startNsTime = this.startNsTime; //注入请求时间
		currentRequest = request; //保存当前请求状态
		try {
			//调用业务处理类（Action）
			String path = request.getPath();
			ActionClassMethod actionInfo = ActionFactory.get(path);
			if(actionInfo == null) {
				String dstPath = ActionFactory.searchRewritePath(path);
				if(dstPath == null) {
					dstPath = path;
				}
				actionInfo = ActionFactory.get(dstPath);
			}
			if(actionInfo != null) {
				BaseMyAction actionObj = actionInfo.createInstance();
				actionObj.request = request;
				actionObj.response = response;
				try {
					//构建Action
					actionObj.construct();
					//自动解析POST对象
					if(request.autoParsePost) {
						request.parseBody();
					}
					actionObj.beforeExecute();
					if(actionObj.check()) {
						actionInfo.executeUserMethod(actionObj);
					} else {
						response.setHttpCode(403);
						response.setError(403, "Input check error.");
					}
				} catch (CheckException e) {
					actionObj.setLastActionException(e); //保存最新的Action异常
					if (e.getData() != null) {
						response.resetData(e.getData());
					}
					response.setError(e.getError(), e.getReason());
				} catch (Exception e) {
					actionObj.setLastActionException(e); //保存最新的Action异常
					if(e instanceof InvocationTargetException && e.getCause() != null
							&& MixedUtils.isEmpty(e.getMessage())) {
						Throwable causeException = e.getCause();
						if(causeException != null) {
							if(causeException instanceof CheckException) {
								CheckException checkE = (CheckException) causeException;
								response.setError(checkE.getError(), checkE.getReason());
								actionObj.setLastActionException(checkE); //保存最新的Action异常
							} else {
								NetLog.logWarn(e.getCause());
								String className = causeException.getClass().getName();
								int pos = className.lastIndexOf('.');
								if(pos >= 0 && pos+1 < className.length()) {
									className.substring(pos+1);
								}
								response.setError(500, "["+className+"] "+causeException.getMessage());
								if(causeException instanceof Exception) {
									actionObj.setLastActionException((Exception)causeException); //保存最新的Action异常
								}
							}
						}
					} else {
						NetLog.logWarn(e);
					}
					//设置默认报错
					if(response.getError() == 0) {
						response.setError(500, e.toString());
					}
				} finally {
					actionObj.afterExecute();
				}
				
				//返回模版文件名称
				return actionObj.tplName;
			} else {
				NetLog.logWarn("Not exists: "+request.getPath());
				response.setHttpCode(404);
				response.setError(404, "Not Found");
				//response.setError(ErrorConstant.PAGE_NOT_FOUND);
			}
//		} catch (InvocationTargetException ie) {
//			caughtException = ie;
//			if(ie.getCause() != null) {
//				caughtException = ie.getCause();
//			}
		} catch (Exception e) {
			if(response.getError() == 0) {
				String name = e.getClass().getName();
				if(name != null) {
					int pos = name.lastIndexOf('.');
					if(pos > 0 && pos+1 < name.length()) {
						name = name.substring(pos+1);
					}
				} else {
					name = "";
				}
				String message = e.getMessage();
				if(message != null && message.length()>0) {
					message = name + ": " + message;
				} else {
					message = name;
				}
				response.setError(500, message);
			}
			NetLog.logWarn(e);
		} finally {
			currentRequest = null; //清除当前请求状态
		}
		//默认不指定模版文件
		return null;
	}
	
	public ProcessorInfo getProcessorInfo() {
		return new ProcessorInfo(currentRequest);
	}
	
	public class ProcessorInfo {
		private String path = null;
		private InetSocketAddress remoteAddress = null;
		private ProcessorInfo(Request request) {
			if(request != null) {
				path = request.getPath();
				remoteAddress = request.getRemoteAddress();
			}
		}
		public String getRunMode() {
			if(runMode == MODE_SOCKET) {
				return "SOCKET";
			} else if(runMode == MODE_HTTP) {
				return "HTTP";
			} else if(runMode == MODE_SERVLET) {
				return "SERVLET";
			} else {
				return "UNKNOWN";
			}
		}
		
		public String getPath() {
			return path;
		}
		
		public InetSocketAddress getRemoteAddress() {
			return remoteAddress;
		}

		
	}
	
}
