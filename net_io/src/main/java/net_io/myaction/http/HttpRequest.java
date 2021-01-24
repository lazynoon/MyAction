package net_io.myaction.http;

import com.sun.net.httpserver.HttpExchange;
import net_io.myaction.HttpHeaders;
import net_io.myaction.Request;
import net_io.myaction.server.QueryStringParser;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

public class HttpRequest extends Request {

	protected HttpRequest() {
		super.autoParsePost = true;
	}

	@Override
	public boolean isHttpRequest() {
		return true;
	}

	public static HttpRequest parse(HttpExchange httpExchange) {
		InputStream in = httpExchange.getRequestBody();
		URI uri = httpExchange.getRequestURI();
		HttpRequest request = new HttpRequest();
		request.in = in;
		request.remoteAddress = httpExchange.getRemoteAddress();
		//取得连接端IP地址
		request.remoteIP = request.remoteAddress.getAddress().getHostAddress();
		//HTTP头部对象
		request.headers = HttpHeaders.newInstance(httpExchange.getRequestHeaders().entrySet());
		//检查代理IP
		request.clientIP = getClientIP(request.remoteIP, request.headers);
		request.scheme = uri.getScheme();
		request.path = uri.getPath();
		request.queryString = uri.getRawQuery();

		//parameter
		try {
			QueryStringParser.parse(request.params, request.queryString);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return request;
	}


}
