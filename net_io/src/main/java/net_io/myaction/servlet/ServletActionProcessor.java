package net_io.myaction.servlet;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net_io.myaction.ActionProcessor;
import net_io.myaction.MyActionServer;
import net_io.myaction.Request;
import net_io.myaction.Response;
import net_io.utils.NetLog;

public class ServletActionProcessor extends ActionProcessor {
	private HttpServletRequest servletRequest;
	private HttpServletResponse servletResponse;
	public ServletActionProcessor(HttpServletRequest servletRequest,	HttpServletResponse servletResponse) {
		this.runMode = MODE_SERVLET;
		this.servletRequest = servletRequest;
		this.servletResponse = servletResponse;
	}
	
	protected void processRequest() {
		OutputStream out = null;
		Request request = null;
		Response response = new Response();
		try {
			request = ServletRequest.parse(servletRequest);
			//处理Action请求
			String tplName = executeAction(request, response);

			//执行Action完毕，检查是否需要运行JSP
			for(String attrName : request.getAttributeNames()) {
				servletRequest.setAttribute(attrName, request.getAttribute(attrName));
			}
			//TODO: 支持非JSP文件
			if(tplName != null) {
				if(tplName.startsWith("/")) {
					tplName = "/WEB-INF/jsp" + tplName;
				} else {
					tplName = "/WEB-INF/jsp/" + tplName;
				}
				servletRequest.getRequestDispatcher(tplName).forward(servletRequest, servletResponse);
			} else {
				out = servletResponse.getOutputStream(); // 获得输出流
			}
		} catch (Exception e) {
			response.setHttpCode(500);
			response.setError(500, "Internat Server Error!");
			NetLog.logError(e);
		} finally {
			try {
				int contentLength = 0;
				byte[] body = response.getBodyBytes();
				if(body != null) {
					contentLength += body.length;
				}
				byte[] attachment = response.getAttachment();
				if(attachment != null) {
					contentLength += attachment.length;
				}
				// 设置响应头属性及响应信息的长度
				servletResponse.setStatus(response.getHttpCode());
				servletResponse.setContentType(response.getFirstHeader("Content-Type"));
				servletResponse.setContentLength(contentLength);
				for(String name : response.getHeaderNames()) {
					if("Content-Type".equalsIgnoreCase(name)
							|| "Content-Length".equalsIgnoreCase(name)) {
						continue;
					}
					for(String value : response.getHeaders(name)) {
						servletResponse.addHeader(name, value);
					}
				}
//				String location = response.getHeader("Location");
//				if(MixedUtils.isEmpty(location) == false) {
//					servletResponse.sendRedirect(location);
//				}
				if(out != null) {
					if(body != null) {
						out.write(body);
					}
					if(attachment != null) {
						out.write(attachment);
					}
					out.close();
				}
			} catch (Exception e) {
				NetLog.logError(e);
			}
		}
	}
	

}
