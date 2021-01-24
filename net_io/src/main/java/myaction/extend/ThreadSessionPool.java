package myaction.extend;

import java.util.concurrent.ConcurrentHashMap;

public class ThreadSessionPool {
	private static ConcurrentHashMap<Thread, CommonParams> pool = new ConcurrentHashMap<Thread, CommonParams>();
	public static CommonParams getCommonParams() {
		return pool.get(Thread.currentThread());
	}
	public static CommonParams getCommonParamsNotNull() {
		CommonParams params = pool.get(Thread.currentThread());
		if(params == null) {
			params = new CommonParams();
		}
		return params;
	}
	protected static CommonParams removeCommonParams() {
		return pool.remove(Thread.currentThread());
	}
	protected static void setCommonParams(CommonParams params) {
		pool.put(Thread.currentThread(), params);
	}
}
