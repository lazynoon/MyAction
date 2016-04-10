package net_io.myaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.core.NetChannel;
import net_io.myaction.ActionFactory.ActionClassMethod;
import net_io.myaction.server.CommandMsg;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class ActionProcessor implements Runnable {
	//按SOCKET接口运行
	public final int MODE_SOCKET = 1;
	//按HTTP接口运行
	public final int MODE_HTTP = 2;
	//启动的nano time
	private long startTime = System.currentTimeMillis();
	//运行模式
	private int runMode = 0;
	private NetChannel channel;
	private CommandMsg msg;
	private HttpExchange httpExchange;
	
	public ActionProcessor(NetChannel channel, CommandMsg msg) {
		this.runMode = MODE_SOCKET;
		this.channel = channel;
		this.msg = msg;
	}
	
	public ActionProcessor(HttpExchange httpExchange) {
		this.runMode = MODE_HTTP;
		this.httpExchange = httpExchange;
	}
	
	/**
	 * 消息处理进程
	 */
	public void run() {
		if(this.runMode == MODE_SOCKET) {
			this.runAsSocket();
		} else if(this.runMode == MODE_HTTP) {
			this.runAsHttp();
		} else {
			NetLog.logError("[ActionProcessor] unkwown mode: "+this.runMode);
		}
	}

	protected void runAsSocket() {
		//解析请求消息
		Request request = Request.parse(msg);
		Response response = Response.clone(request);
		request.setChannel(channel);
		request.startTime = this.startTime;
		//处理Action请求
		executeAction(request, response);
		//输出结果
		//TODO：错误码机制
		if(response.isDisabled() == false) {
			//组装返回消息
			ByteArray sendBuff = new ByteArray(ByteBufferPool.malloc(ByteBufferPool.MAX_BUFFER_SIZE));
			try {
				response.writeSendBuff(sendBuff);
				//发送消息
				channel.send(sendBuff.getByteBuffer());;
			} catch (IOException e) {
				NetLog.logWarn(e);
			} finally {
				ByteBufferPool.free(sendBuff.getByteBuffer()); //发送消息后，立即回收缓存区
				sendBuff = null;
			}
		}
	}
	
	public void runAsHttp() {
		OutputStream out = httpExchange.getResponseBody(); // 获得输出流
		InputStream in = httpExchange.getRequestBody();
		Request request = null;
		Response response = new Response(httpExchange);
		try {
			request = Request.parse(httpExchange);
			request.startTime = this.startTime;
			//解析POST对象
			request.parseBody(MyActionServer.MAX_POST_LENGTH); //Post Max Length: 2M 
			//处理Action请求
			executeAction(request, response);
		} catch (Exception e) {
			response.setHttpCode(500);
			response.setError(500, "Internat Server Error!");
			NetLog.logError(e);
		} finally {
			try {
				in.close(); //关闭输入流
				byte[] body = response.getBodyBytes();
				Headers headers = httpExchange.getResponseHeaders();
				headers.set("Content-Type", "text/html; charset=UTF-8");
				headers.set("Connection", "close");
				httpExchange.sendResponseHeaders(response.getHttpCode(), body.length); // 设置响应头属性及响应信息的长度
				out.write(body);
			} catch (Exception e) {
				NetLog.logError(e);
			} finally {
				httpExchange.close();
			}
		}
	}
	
	/**
	 * 底层 MyAction 框架调用
	 * @param request
	 * @param response
	 */
	private void executeAction(Request request, Response response) {
		try {
			//调用业务处理类（Action）
			ActionClassMethod actionInfo = ActionFactory.get(request.getPath());
			if(actionInfo != null) {
				BaseMyAction actionObj = actionInfo.createInstance();
				actionObj.request = request;
				actionObj.response = response;
				try {
					actionObj.preExecute();
					if(actionObj.check()) {
						actionInfo.executeUserMethod(actionObj);
					} else {
						response.setHttpCode(403);
						response.setError(403, "Input check error.");
					}
				} catch (CheckException e) {
					response.setError(e.getError(), e.getReason());
				} finally {
					actionObj.afterExecute();
				}
				
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
			if(e instanceof InvocationTargetException && e.getCause() != null
					&& MixedUtils.isEmpty(e.getMessage())) {
				Throwable causeException = e.getCause();
				if(causeException != null) {
					if(causeException instanceof CheckException) {
						CheckException checkE = (CheckException) e.getCause();
						response.setError(checkE.getError(), checkE.getReason());
					} else {
						NetLog.logWarn(e.getCause());
						String className = causeException.getClass().getName();
						int pos = className.lastIndexOf('.');
						if(pos >= 0 && pos+1 < className.length()) {
							className.substring(pos+1);
						}
						response.setError(500, "["+className+"] "+causeException.getMessage());
					}
				}
			} else {
				NetLog.logWarn(e);
			}
		}
		
	}
	
}
