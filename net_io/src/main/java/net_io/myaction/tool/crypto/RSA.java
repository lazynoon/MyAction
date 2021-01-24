package net_io.myaction.tool.crypto;

import net_io.myaction.tool.exception.CryptoException;
import net_io.utils.EncodeUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSA {
	public enum KeyMode {PRIVATE_KEY, PUBLIC_KEY}
	private static final String RSA_ALGORITHM = "RSA";
	private KeyMode keyMode;
	RSAPublicKey publicKey = null;
	RSAPrivateKey privateKey = null;
	
	public RSA(KeyMode keyMode, String secretKey) {
		this.keyMode = keyMode;
		initKey(EncodeUtils.base64Decode(secretKey));
	}

	public RSA(KeyMode keyMode, byte[] secretKey) {
		this.keyMode = keyMode;
		initKey(secretKey);
	}
	
	private void initKey(byte[] secretKey) {
		try {
			if(this.keyMode == KeyMode.PRIVATE_KEY) {
				privateKey = parsePrivateKey(secretKey);
			} else {
				publicKey = parsePublicKey(secretKey);
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static KeyGroup createKeys(int keySize) {
		// 为RSA算法创建一个KeyPairGenerator对象
		KeyPairGenerator kpg;
		try {
			kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("No such algorithm-->[" + RSA_ALGORITHM + "]");
		}

		// 初始化KeyPairGenerator对象,密钥长度
		kpg.initialize(keySize);
		// 生成密匙对
		KeyPair keyPair = kpg.generateKeyPair();
		// 得到公钥
		Key publicKey = keyPair.getPublic();
		// 得到私钥
		Key privateKey = keyPair.getPrivate();
		return new KeyGroup(privateKey.getEncoded(), publicKey.getEncoded());
	}

	/**
	 * 得到公钥
	 * 
	 * @param publicKey 密钥字符串（经过base64编码）
	 * @throws Exception
	 */
	public static RSAPublicKey parsePublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return parsePublicKey(EncodeUtils.base64Decode(publicKey));
	}

	public static RSAPublicKey parsePublicKey(byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// 通过X509编码的Key指令获得公钥对象
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKey);
		RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(x509KeySpec);
		return key;
	}

	/**
	 * 得到私钥
	 * 
	 * @param privateKey 密钥字符串（经过base64编码）
	 * @throws Exception
	 */
	protected static RSAPrivateKey parsePrivateKey(String privateKey)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		return parsePrivateKey(EncodeUtils.base64Decode(privateKey));
	}

	protected static RSAPrivateKey parsePrivateKey(byte[] privateKey)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// 通过PKCS#8编码的Key指令获得私钥对象
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKey);
		RSAPrivateKey key = (RSAPrivateKey) keyFactory.generatePrivate(pkcs8KeySpec);
		return key;
	}
	
	public String encrypt(String str) throws CryptoException {
		return EncodeUtils.base64Encode(encrypt(str.getBytes(EncodeUtils.Charsets.UTF_8)));
	}
	
	public String decrypt(String str) throws CryptoException {
		return new String(decrypt(EncodeUtils.base64Decode(str)), EncodeUtils.Charsets.UTF_8);
	}

	public byte[] encrypt(byte[] bts) throws CryptoException {
		try {
			if(this.keyMode == KeyMode.PRIVATE_KEY) {
				return privateEncrypt(bts, privateKey);
			} else {
				return publicEncrypt(bts, publicKey);
			}
		} catch(IllegalBlockSizeException e) {
			throw new CryptoException(e);
		} catch(NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch(BadPaddingException e) {
			throw new CryptoException(e);
		} catch(NoSuchPaddingException e) {
			throw new CryptoException(e);
		} catch(InvalidKeyException e) {
			throw new CryptoException(e);
		} catch(IOException e) {
			throw new CryptoException(e);
		}
	}
	
	public byte[] decrypt(byte[] bts) throws CryptoException {
		try {
			if(this.keyMode == KeyMode.PRIVATE_KEY) {
				return privateDecrypt(bts, privateKey);
			} else {
				return publicDecrypt(bts, publicKey);
			}		
		} catch(IllegalBlockSizeException e) {
			throw new CryptoException(e);
		} catch(NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch(BadPaddingException e) {
			throw new CryptoException(e);
		} catch(NoSuchPaddingException e) {
			throw new CryptoException(e);
		} catch(InvalidKeyException e) {
			throw new CryptoException(e);
		} catch(IOException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * 公钥加密
	 * 
	 * @param bts
	 * @param publicKey
	 * @return byte[]
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	protected static byte[] publicEncrypt(byte[] bts, RSAPublicKey publicKey)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, bts, publicKey.getModulus().bitLength());
	}

	/**
	 * 私钥解密
	 * 
	 * @param bts
	 * @param privateKey
	 * @return byte[]
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */

	protected static byte[] privateDecrypt(byte[] bts, RSAPrivateKey privateKey)
			throws InvalidKeyException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		
		return rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, bts, privateKey.getModulus().bitLength());
	}

	/**
	 * 私钥加密
	 * 
	 * @param bts
	 * @param privateKey
	 * @return byte[]
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	protected static byte[] privateEncrypt(byte[] bts, RSAPrivateKey privateKey)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, privateKey);
		return rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, bts, privateKey.getModulus().bitLength());
	}

	/**
	 * 公钥解密
	 * 
	 * @param bts
	 * @param publicKey
	 * @return byte[]
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	protected static byte[] publicDecrypt(byte[] bts, RSAPublicKey publicKey)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		return rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, bts, publicKey.getModulus().bitLength());
	}

	private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] datas, int keySize)
			throws IOException, IllegalBlockSizeException, BadPaddingException {
		int maxBlock = 0;
		if (opmode == Cipher.DECRYPT_MODE) {
			maxBlock = keySize / 8;
		} else {
			maxBlock = keySize / 8 - 11;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] buff;
		int i = 0;
		while (datas.length > offSet) {
			if (datas.length - offSet > maxBlock) {
				buff = cipher.doFinal(datas, offSet, maxBlock);
			} else {
				buff = cipher.doFinal(datas, offSet, datas.length - offSet);
			}
			out.write(buff, 0, buff.length);
			i++;
			offSet = i * maxBlock;
		}
		byte[] resultDatas = out.toByteArray();
		out.close();
		return resultDatas;
	}
	
	public static class KeyGroup {
		private byte[] privateKey;
		private byte[] publicKey;
		public KeyGroup(byte[] privateKey, byte[] publicKey) {
			this.privateKey= privateKey;
			this.publicKey= publicKey;
		}
		public byte[] getPrivateKeyBytes() {
			return privateKey;
		}
		public byte[] getPublicKeyBytes() {
			return publicKey;
		}
		public String getPrivateKeyString() {
			return EncodeUtils.base64Encode(privateKey);
		}
		public String getPublicKeyString() {
			return EncodeUtils.base64Encode(publicKey);
		}
	}

}
