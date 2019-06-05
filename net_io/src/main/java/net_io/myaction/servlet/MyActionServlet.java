package net_io.myaction.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net_io.utils.FindClassUtils;
import net_io.utils.MixedUtils;

public class MyActionServlet extends HttpServlet {

	private static final long serialVersionUID = -2285022876503453694L;

	@SuppressWarnings("rawtypes")
	public void init(ServletConfig config) throws ServletException {
		
		Map<String, String> initParams = new LinkedHashMap<String, String>();
		Enumeration<String> data = config.getInitParameterNames();
		while(data.hasMoreElements()) {
			String name = data.nextElement();
			initParams.put(name, config.getInitParameter(name));
		}
		ClassLoader oldClassLoader = FindClassUtils.getDefaultClassLoader();
		FindClassUtils.setDefaultClassLoader(config.getServletContext().getClassLoader());
		String startupClassName = initParams.get("startup");
		try {
			if(MixedUtils.isEmpty(startupClassName) == false) {
				Class cls = Class.forName(startupClassName);
				Object obj = cls.newInstance();
				if(obj instanceof ServletStartup) {
					((ServletStartup) obj).init(initParams); 
					System.out.println("MyAction startup success...");
				} else {
					System.err.println("MyAction startup error. not support class: "+startupClassName);					
				}
			} else {
				System.err.println("MyAction startup error. undefined init-param: startup");
			}
		} catch (Exception e) {
			System.err.println("MyAction startup error. "+e);
			throw new ServletException(e);
		} finally {
			FindClassUtils.setDefaultClassLoader(oldClassLoader);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request,	response);
	}

	@Override
	protected void doPost(HttpServletRequest request,	HttpServletResponse response) throws ServletException, IOException {
		doRequest(request,	response);
	}
	
	private void doRequest(HttpServletRequest servletRequest,	HttpServletResponse servletResponse) throws ServletException, IOException {
		ServletActionProcessor processor = new ServletActionProcessor(servletRequest, servletResponse);
		processor.run();
	}

}
