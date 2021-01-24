package myaction.action;

import myaction.action.monitor.LifeConsoleAction;
import myaction.action.monitor.EchoRequestAction;
import myaction.action.monitor.JsqlxAction;
import myaction.action.monitor.RunStatusAction;
import myaction.action.monitor.ServerStatusAction;
import myaction.action.monitor.ServerTimeAction;
import myaction.extend.ActionRegister;
import myaction.extend.BaseServiceAction;

public class MyMonitorActionRegister extends ActionRegister {
	public static final ActionRegister instance = new MyMonitorActionRegister("myaction/monitor/");

	private MyMonitorActionRegister(String pathName) {
		super(pathName);
	}

	@Override
	protected void onRegister() {
		this.register("manager/lifeConsole", LifeConsoleAction.class);
		this.register("myaction/echoRequest", EchoRequestAction.class);
		this.register("myaction/jsqlx", JsqlxAction.class);
		this.register("myaction/runStatus", RunStatusAction.class);
		this.register("myaction/serverStatus", ServerStatusAction.class);
		this.register("myaction/serverTime", ServerTimeAction.class);
		this.register(BaseServiceAction.class);
		this.register(BaseServiceAction.class);

	}
}
