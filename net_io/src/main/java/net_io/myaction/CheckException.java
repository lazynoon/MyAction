package net_io.myaction;

import net_io.utils.Mixed;

public class CheckException extends Exception {
	/** 错误码 **/
	private int error = 0;
	/** 错误原因 **/
	private String reason = null;
	/** 附带数据 **/
	private Mixed data = null;

	private static final long serialVersionUID = -7674340404540683643L;

	/**
	 * 创建 CheckException 对象
	 * @param error 错误码
	 * @param reason 错误原因
	 */
	public CheckException(int error, String reason) {
		super(reason);
		this.error = error;
		this.reason = reason;
	}

	/**
	 * 创建 CheckException 对象
	 * @param error 错误码
	 * @param reason 错误原因
	 * @param data 附带数据
	 */
	public CheckException(int error, String reason, Mixed data) {
		super(reason);
		this.error = error;
		this.reason = reason;
		this.data = data;
	}

	/**
	 * 获取错误码
	 * @return 错误码
	 */
	public int getError() {
		return error;
	}

	/**
	 * 获取错误原因
	 * @return 错误原因
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * 获取附带数据
	 * @return Mixed类型数据，不存在返回null
	 */
	public Mixed getData() {
		return data;
	}

	@Override
	public String toString() {
		return "CheckException(" + error + ") " + reason;
	}

}
