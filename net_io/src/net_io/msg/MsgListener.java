package net_io.msg;



public interface MsgListener {
	/**
	 * 消息分发
	 * @param msg
	 * @return true 表示可继续执行下条消息处理器，false执行结束
	 */
	public boolean dispatch(int chID, BaseMsg msg) throws Exception;
}
