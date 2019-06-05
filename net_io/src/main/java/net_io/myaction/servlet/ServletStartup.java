package net_io.myaction.servlet;

import java.util.Map;

public abstract class ServletStartup {	
	abstract public void init(Map<String, String> initParams) throws Exception;
}
