package net_io.myaction.tool;

import java.io.UnsupportedEncodingException;

public interface Encryption {
	/**
	 * 加密
	 * @param bts
	 * @return byte[]
	 */
	public byte[] encrypt(byte[] bts) throws UnsupportedEncodingException;
	public byte[] decrypt(byte[] bts) throws UnsupportedEncodingException;
}
