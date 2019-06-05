package net_io.myaction.servlet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import net_io.myaction.ActionFactory;
import net_io.myaction.HttpHeaders;
import net_io.myaction.Request;

public class ServletRequest extends Request {
	public static ServletRequest parse(HttpServletRequest servletRequest) throws IOException {
		ServletRequest request = new ServletRequest();
		request.in = servletRequest.getInputStream();
		request.remoteAddress = new InetSocketAddress(servletRequest.getRemoteAddr(), servletRequest.getRemotePort());
		//取得连接端IP地址
		request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		request.headers = new HttpHeaders();
		Enumeration<String> headerNameEnum = servletRequest.getHeaderNames();
		while(headerNameEnum.hasMoreElements()) {
			String headerName = headerNameEnum.nextElement();
			Enumeration<String> headerValueEnum = servletRequest.getHeaders(headerName);
			while(headerValueEnum.hasMoreElements()) {
				request.headers.putHeader(headerName, headerValueEnum.nextElement());
			}
		}
		request.clientIP = getClientIP(request.remoteIP, request.headers);
		request.scheme = servletRequest.getScheme();
		request.path = servletRequest.getPathInfo();
		request.queryString = servletRequest.getQueryString();
		Enumeration<String> nameEnum = servletRequest.getParameterNames();
		while(nameEnum.hasMoreElements()) {
			servletRequest.setCharacterEncoding(ActionFactory.getDefaultCharset()); //设置参数中的字符集
			String name = nameEnum.nextElement();
			request.params.set(name, servletRequest.getParameter(name));
		}
		return request;
	}
	

}
