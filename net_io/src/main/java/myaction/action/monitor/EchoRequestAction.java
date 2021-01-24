package myaction.action.monitor;

import myaction.action.MyMonitorBaseAction;

public class EchoRequestAction extends MyMonitorBaseAction {

	@Override
	public void service(String methodName) throws Exception {
		if("Index".equalsIgnoreCase(methodName)) {
			this.doIndex();
		} else {
			this.throwNotFoundException(methodName);
		}
		
	}

	public void doIndex() {
		StringBuffer sb = new StringBuffer();
		sb.append("<STRONG>");
		sb.append(request.getPath());
		sb.append("</STRONG>\n");
		for(String name : request.getParameterNames()) {
			sb.append("\t");
			sb.append(name);
			sb.append(": ");
			sb.append(request.getParameter(name));
			sb.append("\n");
		}
		response.println("<PRE>");
		response.println(sb.toString());
	}
}
