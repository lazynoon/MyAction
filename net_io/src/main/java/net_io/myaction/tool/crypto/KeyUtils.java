package net_io.myaction.tool.crypto;

import net_io.myaction.tool.exception.CryptoException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

public class KeyUtils {
	/**
	 * 根据pfx证书得到私钥
	 *
	 * @param pfxData
	 * @param password
	 * @throws Exception
	 */
	public static PrivateKey getPrivateKeyFromPfx(byte[] pfxData, String password) throws CryptoException {
		try {
			PrivateKey privateKey = null;
			KeyStore keystore = getKeyStoreFromPfx(pfxData, password);
			Enumeration<String> enums = keystore.aliases();
			String keyAlias = "";
			while (enums.hasMoreElements()) {
				keyAlias = enums.nextElement();
				if (keystore.isKeyEntry(keyAlias)) {
					privateKey = (PrivateKey) keystore.getKey(keyAlias, password.toCharArray());
				}
			}
			return privateKey;
		} catch (KeyStoreException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (UnrecoverableKeyException e) {
			throw new CryptoException(e);
		}
	}

	public static PublicKey getPublicKeyFromCer(byte[] cerData) throws CryptoException {
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate c = cf.generateCertificate(new ByteArrayInputStream(cerData));
			PublicKey publicKey = c.getPublicKey();
			return publicKey;
		} catch (CertificateException e) {
			throw new CryptoException(e);
		}
	}

	public static RSAPrivateKey getRSAPrivateKeyFromPfx(byte[] pfxData, String password) throws CryptoException {
		return (RSAPrivateKey) getPrivateKeyFromPfx(pfxData, password);
	}

	public static RSAPublicKey getRSAPublicKeyFromCer(byte[] cerData) throws CryptoException {
		return (RSAPublicKey) getPublicKeyFromCer(cerData);
	}

	/**
	 * 根据pfx证书获取证书对象
	 *
	 * @param pfxData  pfx的字节数组
	 * @param password pfx证书密码
	 * @return
	 * @throws Exception
	 */
	public static X509Certificate getX509CertificateFromPfx(byte[] pfxData, String password) throws CryptoException {
		try {
			KeyStore keystore = getKeyStoreFromPfx(pfxData, password);
			Enumeration<String> enums = keystore.aliases();
			String keyAlias = "";
			while (enums.hasMoreElements()) {
				keyAlias = enums.nextElement();
				if (keystore.isKeyEntry(keyAlias)) {
					Certificate cert = keystore.getCertificate(keyAlias);
					return (X509Certificate) cert;
				}
			}
			throw new CryptoException("Can not find cert from pfx");
		} catch (KeyStoreException e) {
			throw new CryptoException(e);
		}
	}


	/**
	 * 根据私钥、公钥证书、密码生成pkcs12
	 *
	 * @param privateKey      私钥
	 * @param x509Certificate 公钥证书
	 * @param password        需要设置的密钥
	 * @return
	 * @throws Exception
	 */
	public static byte[] generatorPKCS12(PrivateKey privateKey, X509Certificate x509Certificate, String password)
			throws CryptoException {
		try {
			Certificate[] chain = {x509Certificate};
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(null, password.toCharArray());
			keystore.setKeyEntry(x509Certificate.getSerialNumber().toString(), privateKey, password.toCharArray(), chain);
			ByteArrayOutputStream bytesos = new ByteArrayOutputStream();
			keystore.store(bytesos, password.toCharArray());
			byte[] bytes = bytesos.toByteArray();
			return bytes;
		} catch (KeyStoreException e) {
			throw new CryptoException(e);
		} catch (IOException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (CertificateException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * 根据pfx证书获取keyStore
	 *
	 * @param pfxData
	 * @param password
	 * @return
	 * @throws Exception
	 */
	private static KeyStore getKeyStoreFromPfx(byte[] pfxData, String password) throws CryptoException {
		try {
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(new ByteArrayInputStream(pfxData), password.toCharArray());
			return keystore;
		} catch (KeyStoreException e) {
			throw new CryptoException(e);
		} catch (IOException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (CertificateException e) {
			throw new CryptoException(e);
		}
	}

}
