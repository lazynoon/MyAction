import net_io.utils.JSONUtils;


public class EscapeTest {
	public static void main(String[] argv) {
		String str = "aaa\nbbb\"ccc\n中文来一段";
		System.out.println(JSONUtils.escape(str));
		
		str = "<input type=\\\"hidden\\\" id=\\\"stock_status\\\" \\r\\n     <span  id=\\\"select_add_span\\\">\\\u8bf7\\\u9009\\\u62e9\\\u914d\\\u9001\\\u5730\\\u533a";
		System.out.println(JSONUtils.unescape(str));
	}
}
