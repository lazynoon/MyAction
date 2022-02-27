package net_io.myaction.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net_io.myaction.ActionProcessor;
import net_io.myaction.MyActionServer;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.myaction.http.HttpRequest;
import net_io.utils.NetLog;

public class HttpActionProcessor extends ActionProcessor {
	private HttpExchange httpExchange;
	public HttpActionProcessor(HttpExchange httpExchange) {
		this.runMode = MODE_HTTP;
		this.httpExchange = httpExchange;
	}

	protected void processRequest() {
		OutputStream out = httpExchange.getResponseBody(); // 获得输出流
		InputStream in = httpExchange.getRequestBody();
		Request request = null;
		Response response = new Response();
		try {
			request = HttpRequest.parse(httpExchange);
			//处理Action请求
			executeAction(request, response);
		} catch (Exception e) {
			response.setHttpCode(500);
			response.setError(500, "Internat Server Error!");
			NetLog.logError(e);
		} finally {
			try {
				in.close(); //关闭输入流
				Headers headers = httpExchange.getResponseHeaders();
				for(String name : response.getHeaderNames()) {
//					if("Connection".equalsIgnoreCase(name)) {
//						continue;
//					}
					for(String value : response.getHeaders(name)) {
						headers.add(name, value);
					}
				}
				headers.set("Connection", "close");
				
				int contentLength = 0;
				byte[] body = response.getBodyBytes();
				if(body != null) {
					contentLength += body.length;
				}
				byte[] attachment = response.getAttachment();
				if(attachment != null) {
					contentLength += attachment.length;
				}
				headers.set("Content-Length", String.valueOf(contentLength));
				httpExchange.sendResponseHeaders(response.getHttpCode(), contentLength); // 设置响应头属性及响应信息的长度
				//总是输出内容
				if(body != null) {
					out.write(body);
				}
				if(attachment != null) {
					out.write(attachment);
				}
			} catch (Exception e) {
				NetLog.logError(e);
			} finally {
				httpExchange.close();
			}
		}
	}
	

}
