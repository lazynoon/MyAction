package net_io.myaction.tool.crypto;

import net_io.myaction.tool.exception.CryptoException;
import net_io.utils.EncodeUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AES {

	/** 默认的加密算法 **/
	public static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	/** 密钥转换算法 **/
	public static enum KeyAlgorithm {
		/** 直接使用输入密钥（密钥超长则截取，位数不足则截取） **/
		PLAIN,
		/** 基于SHA1哈希算法的密钥转换 **/
		SHA1PRNG
	}
	/** 加密算法名称 **/
	private static final String KEY_ALGORITHM = "AES";
	/** 最小密钥长度 **/
	private int MIN_KEY_SIZE = 64;
	/** 密钥长度 **/
	private int maxKeySize = 128;
	/** 加密算法 **/
	private String cipherAlgorithm = DEFAULT_CIPHER_ALGORITHM;
	/** 密钥转换算法 **/
	private KeyAlgorithm keyAlgorithm = KeyAlgorithm.PLAIN;

	/** 密钥 **/
	private byte[] secretKey;

	/**
	 *
	 * @param password 加密密码（按UTF-8编码取值）
	 */
	public AES(String password) {
		this.secretKey = password.getBytes(EncodeUtils.Charsets.UTF_8);
	}

	public AES(byte[] secretKey) {
		this.secretKey = secretKey;
	}

	public AES(byte[] secretKey, String algorithm) throws CryptoException {
		this.secretKey = secretKey;
		this.setCipherAlgorithm(algorithm);
	}

	/**
	 * AES 加密操作
	 *
	 * @param str 待加密内容
	 * @return 返回Base64转码后的加密数据
	 * @throws CryptoException
	 */
	public String encrypt(String str) throws CryptoException {
		byte[] bts = str.getBytes(EncodeUtils.Charsets.UTF_8);
		bts = encrypt(bts);
		return EncodeUtils.base64Encode(bts);
	}

	/**
	 * AES 解密操作
	 *
	 * @param str 待解密内容
	 * @throws CryptoException
	 */
	public String decrypt(String str) throws CryptoException {
		byte[] bts = EncodeUtils.base64Decode(str);
		bts = decrypt(bts);
		return new String(bts, EncodeUtils.Charsets.UTF_8);
	}

	public byte[] encrypt(byte[] bts) throws CryptoException {

		try {
			// 创建密码器
			Cipher cipher = Cipher.getInstance(cipherAlgorithm);

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
	 * @param bts
	 * @throws CryptoException
	 */
	public byte[] decrypt(byte[] bts)	throws CryptoException {
		try {
			// 实例化
			Cipher cipher = Cipher.getInstance(cipherAlgorithm);
	
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

	/** 获取加密算法 **/
	public String getCipherAlgorithm() {
		return this.cipherAlgorithm;
	}

	/** 更新加密算法 **/
	public void setCipherAlgorithm(String algorithm) throws CryptoException {
		try {
			this.maxKeySize = Cipher.getMaxAllowedKeyLength(algorithm);
			this.cipherAlgorithm = algorithm;
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		}
	}

	/** 获取密钥转换算法 **/
	public KeyAlgorithm getKeyAlgorithm() {
		return keyAlgorithm;
	}

	/** 设置密钥转换算法 **/
	public void setKeyAlgorithm(KeyAlgorithm keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
	}

	/** 最大密钥长度（以位为单位） 或 Integer.max_value **/
	public int getMaxKeySize() {
		return maxKeySize;
	}

	/**
	 * 生成加密秘钥
	 *
	 * @return SecretKeySpec
	 * @throws NoSuchAlgorithmException
	 */
	private SecretKeySpec getSecretKey() throws NoSuchAlgorithmException {
		byte[] encodedKey;
		if(keyAlgorithm == KeyAlgorithm.SHA1PRNG) {
			// 返回生成指定算法密钥生成器的 KeyGenerator 对象
			KeyGenerator kg = null;
			kg = KeyGenerator.getInstance(KEY_ALGORITHM);
			// AES 要求密钥长度为 128
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(secretKey);
			kg.init(maxKeySize, secureRandom);
			// 生成一个密钥
			encodedKey = kg.generateKey().getEncoded();
		} else { //KeyAlgorithm.PLAIN
			int keyBytes = maxKeySize / 8;
			if(maxKeySize % 8 != 0) {
				keyBytes++;
			}
			if(keyBytes == secretKey.length) {
				encodedKey = secretKey;
			} else {
				encodedKey = new byte[keyBytes];
				if(keyBytes > secretKey.length) {
					System.arraycopy(secretKey, 0, encodedKey, 0, secretKey.length);
					for(int i=secretKey.length; i<keyBytes; i++) {
						encodedKey[i] = 0;
					}
				} else {
					System.arraycopy(secretKey, 0, encodedKey, 0, keyBytes);
				}
			}

		}
		// 转换为AES专用密钥
		return new SecretKeySpec(encodedKey, KEY_ALGORITHM);
	}


}
