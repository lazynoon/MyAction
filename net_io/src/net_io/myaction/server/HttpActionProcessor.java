package net_io.myaction.server;

import java.io.InputStream;
import java.io.OutputStream;

import net_io.myaction.ActionProcessor;
import net_io.myaction.MyActionServer;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.MixedUtils;
import net_io.utils.NetLog;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

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
		Response response = new Response(httpExchange);
		try {
			request = Request.parse(httpExchange);
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
				for(String name : response.getHeaderNames()) {
//					if("Connection".equalsIgnoreCase(name)) {
//						continue;
//					}
					headers.set(name, response.getHeader(name));
				}
				headers.set("Connection", "close");
				httpExchange.sendResponseHeaders(response.getHttpCode(), body.length); // 设置响应头属性及响应信息的长度
//				//若未被重定向，则输出内容。
//				if(MixedUtils.isEmpty(response.getHeader("Location"))) {
//					out.write(body);
//				}
				//总是输出内容
				out.write(body);
			} catch (Exception e) {
				NetLog.logError(e);
			} finally {
				httpExchange.close();
			}
		}
	}
	

}
