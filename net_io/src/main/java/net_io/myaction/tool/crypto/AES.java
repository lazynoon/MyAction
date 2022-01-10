package net_io.myaction.tool.crypto;

import myaction.utils.StringUtil;
import net_io.myaction.tool.exception.CryptoException;
import net_io.utils.EncodeUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AES {
	/** 加密算法名称 **/
	public static final String CRYPTO_ALGORITHM = "AES";

	/** 加密模式：电码本模式（Electronic Codebook Book (ECB) **/
	public static final String CRYPTO_MODE_ECB = "ECB";
	/** 加密模式：密码分组链接模式（Cipher Block Chaining (CBC) **/
	public static final String CRYPTO_MODE_CBC = "CBC";
	/** 加密模式：计算器模式（Counter (CTR)） **/
	public static final String CRYPTO_MODE_CTR = "CTR";
	/** 加密模式：密码反馈模式（Cipher FeedBack (CFB)） **/
	public static final String CRYPTO_MODE_CFB = "CFB";
	/** 加密模式：输出反馈模式（Output FeedBack (OFB)） **/
	public static final String CRYPTO_MODE_OFB = "OFB";

	/** 默认的加密模式 **/
	private static final String DEFAULT_CRYPTO_MODE = CRYPTO_MODE_ECB;
	/** 默认的填充方式 **/
	private static final String DEFAULT_CRYPTO_PADDING = "PKCS5Padding";

	/** 密钥转换算法 **/
	public enum KeyAlgorithm {
		/** 直接使用输入密钥（密钥超长则截取，位数不足则截取） **/
		PLAIN,
		/** 基于SHA1哈希算法的密钥转换 **/
		SHA1PRNG
	}
	/** 加密算法（默认算法：AES/ECB/PKCS5Padding） **/
	private String cipherAlgorithm = CRYPTO_ALGORITHM + "/" + DEFAULT_CRYPTO_MODE + "/" + DEFAULT_CRYPTO_PADDING;
	/** 密钥转换算法 **/
	private KeyAlgorithm keyAlgorithm = KeyAlgorithm.PLAIN;
	/** 密钥 **/
	private byte[] secretKey;

	/** 加密模式 **/
	private String cryptoMode = DEFAULT_CRYPTO_MODE;
	/** 填充方式 **/
	private String cryptoPadding = DEFAULT_CRYPTO_PADDING;

	private int maxKeySize = 128;
	private int blockByteSize = maxKeySize / 8;
	/** 向量数量（首个加密的附加密钥） **/
	private byte[] blockDataIV = null;
	/** 是否需要向量参数（CBC/CFB/OFB需要向量） **/
	private boolean isNeedIV = false;

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
		return EncodeUtils.encodeBase64ToString(bts);
	}

	/**
	 * AES 解密操作
	 *
	 * @param str 待解密内容
	 * @throws CryptoException
	 */
	public String decrypt(String str) throws CryptoException {
		byte[] bts = EncodeUtils.decodeBase64(str);
		bts = decrypt(bts);
		return new String(bts, EncodeUtils.Charsets.UTF_8);
	}

	public byte[] encrypt(byte[] bts) throws CryptoException {
		try {
			// 创建密码器
			Cipher cipher = Cipher.getInstance(cipherAlgorithm);

			// 初始化为加密模式的密码器
			if (isNeedIV) {
				IvParameterSpec iv = new IvParameterSpec(blockDataIV);
				cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), iv);
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			}
			
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
		} catch (InvalidAlgorithmParameterException e) {
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
			if (isNeedIV) {
				IvParameterSpec iv = new IvParameterSpec(blockDataIV);
				cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), iv);
			} else {
				cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			}
	
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
		} catch (InvalidAlgorithmParameterException e) {
			throw new CryptoException(e);
		}
	}

	/** 获取加密算法 **/
	public String getCipherAlgorithm() {
		return this.cipherAlgorithm;
	}

	/** 更新加密算法 **/
	public void setCipherAlgorithm(String algorithm) throws CryptoException {
		if (algorithm == null && algorithm.isEmpty()) {
			throw new CryptoException("algorithm is empty");
		}
		String[] arr = StringUtil.split(algorithm, '/');
		if (arr.length != 3) {
			throw new CryptoException("algorithm(" + algorithm + ") format error. Must start with AES and contain 3 parts.");
		}
		if (!CRYPTO_ALGORITHM.equalsIgnoreCase(arr[0].trim())) {
			throw new CryptoException("algorithm(" + algorithm + ") format error. Algorithm must start with AES.");
		}
		String cryptoMode = arr[1].toUpperCase();
		String cryptoPadding = arr[2].trim();
		if (CRYPTO_MODE_CBC.equals(cryptoMode)
			|| CRYPTO_MODE_CFB.equals(cryptoMode)
			|| CRYPTO_MODE_OFB.equals(cryptoMode)) {
			this.isNeedIV = true;
		} else if (CRYPTO_MODE_ECB.equals(cryptoMode)
				|| CRYPTO_MODE_CTR.equals(cryptoMode)) {
			this.isNeedIV = false;
		} else {
			throw new CryptoException("Not support algorithm: " + algorithm + ".");
		}
		algorithm = CRYPTO_ALGORITHM + "/" + cryptoMode + "/" + cryptoPadding;
		try {
			this.maxKeySize = Cipher.getMaxAllowedKeyLength(algorithm);
			this.cipherAlgorithm = algorithm;
			if (maxKeySize > 0xFFFF) {
				this.blockByteSize = this.secretKey.length;
			} else {
				this.blockByteSize = maxKeySize / 8;
				if (maxKeySize % 8 != 0) {
					this.blockByteSize++;
				}
			}
			if (this.isNeedIV) {
				resetZeroIV();
			} else {
				this.blockDataIV = null;
			}
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		}
	}

	public void resetZeroIV() {
		blockDataIV = new byte[blockByteSize];
		for (int i=0; i<blockByteSize; i++) {
			blockDataIV[i] = 0;
		}
	}

	/**
	 * 设置加密向量数组
	 *   舍弃超长部分
	 *   用0填充不足部分
	 * @param blockData 向量数组
	 */
	public void setCryptoIV(String blockData) {
		setCryptoIV(blockData.getBytes(EncodeUtils.Charsets.UTF_8));
	}

	/**
	 * 设置加密向量数组
	 *   舍弃超长部分
	 *   用0填充不足部分
	 * @param blockData 向量数组
	 */
	public void setCryptoIV(byte[] blockData) {
		if (blockData == null || blockData.length == 0) {
			throw new IllegalArgumentException("blockData is empty");
		}
		this.blockDataIV = new byte[blockByteSize];
		if (blockData.length >= blockByteSize) {
			System.arraycopy(blockData, 0, this.blockDataIV, 0, blockByteSize);
		} else {
			System.arraycopy(blockData, 0, this.blockDataIV, 0, blockData.length);
			for (int i=blockData.length; i<blockByteSize; i++) {
				blockDataIV[i] = 0;
			}
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
			kg = KeyGenerator.getInstance(CRYPTO_ALGORITHM);
			// AES 要求密钥长度为 128
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(secretKey);
			kg.init(maxKeySize, secureRandom);
			// 生成一个密钥
			encodedKey = kg.generateKey().getEncoded();
		} else { //KeyAlgorithm.PLAIN
			if(blockByteSize == secretKey.length) {
				encodedKey = secretKey;
			} else {
				encodedKey = new byte[blockByteSize];
				if(blockByteSize > secretKey.length) {
					System.arraycopy(secretKey, 0, encodedKey, 0, secretKey.length);
					for(int i=secretKey.length; i<blockByteSize; i++) {
						encodedKey[i] = 0;
					}
				} else {
					System.arraycopy(secretKey, 0, encodedKey, 0, blockByteSize);
				}
			}

		}
		// 转换为AES专用密钥
		return new SecretKeySpec(encodedKey, CRYPTO_ALGORITHM);
	}


}
