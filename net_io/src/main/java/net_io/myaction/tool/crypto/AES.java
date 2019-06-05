package net_io.myaction.tool.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net_io.myaction.tool.exception.CryptoException;
import net_io.utils.EncodeUtils;

public class AES {

	private static final String KEY_ALGORITHM = "AES";
	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";// 默认的加密算法
	
	private byte[] secretKey;
	
	public AES(String password) {
		this.secretKey = password.getBytes(StandardCharsets.UTF_8);
	}

	public AES(byte[] secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * AES 加密操作
	 *
	 * @param content
	 *            待加密内容
	 * @param password
	 *            加密密码
	 * @return 返回Base64转码后的加密数据
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	public String encrypt(String str) throws CryptoException {
		byte[] bts = str.getBytes(StandardCharsets.UTF_8);
		bts = encrypt(bts);
		return EncodeUtils.base64Encode(bts);
	}

	/**
	 * AES 解密操作
	 *
	 * @param content
	 * @param password
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	public String decrypt(String str) throws CryptoException {
		byte[] bts = EncodeUtils.base64Decode(str);
		bts = decrypt(bts);
		return new String(bts, StandardCharsets.UTF_8);
	}

	public byte[] encrypt(byte[] bts) throws CryptoException {

		try {
			// 创建密码器
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

			// 初始化为加密模式的密码器
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			
			return cipher.doFinal(bts); //加密
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException(e);
		} catch (BadPaddingException e) {
			throw new CryptoException(e);
		} catch (InvalidKeyException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * AES 解密操作
	 *
	 * @param content
	 * @param password
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	public byte[] decrypt(byte[] bts)	throws CryptoException {
		try {
			// 实例化
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
	
			// 使用密钥初始化，设置为解密模式
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
	
			return cipher.doFinal(bts); //解密
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException(e);
		} catch (BadPaddingException e) {
			throw new CryptoException(e);
		} catch (InvalidKeyException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * 生成加密秘钥
	 *
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private SecretKeySpec getSecretKey() throws NoSuchAlgorithmException {
		// 返回生成指定算法密钥生成器的 KeyGenerator 对象
		KeyGenerator kg = null;

		kg = KeyGenerator.getInstance(KEY_ALGORITHM);

		// AES 要求密钥长度为 128
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(secretKey);
		kg.init(128, secureRandom);

		// 生成一个密钥
		SecretKey secretKey = kg.generateKey();

		return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
	}


}
