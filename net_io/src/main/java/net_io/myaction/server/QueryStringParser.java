package net_io.myaction.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import net_io.myaction.ActionFactory;
import net_io.utils.Mixed;

public class QueryStringParser {
	public static void parse(Mixed result, String query) throws UnsupportedEncodingException {
		if(query != null && query.length() > 0) {
			for(String str : query.split("&")) {
				String[] arr = str.split("=", 2);
				String value = arr.length>1?arr[1]:"";
				value = URLDecoder.decode(value, ActionFactory.getDefaultCharset());
				result.set(arr[0], value);
			}
		}
	}

}
