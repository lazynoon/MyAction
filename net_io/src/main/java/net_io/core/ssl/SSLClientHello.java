package net_io.core.ssl;

import net_io.core.ByteArray;
import net_io.core.ByteBufferPool;
import net_io.utils.EncodeUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SSLClientHello {
	/** HELLO数据库最大限长 **/
	private static final int MAX_HELLO_PACKET_LENGTH = 8192;
	/** 扩展属性：服务器名称 **/
	public static final int EXTENSION_SERVER_NAME = 0x00;
	/** 扩展属性：填充 **/
	public static final int EXTENSION_PADDING = 0x15;
	/** 扩展属性：key share **/
	public static final int EXTENSION_KEY_SHARE = 0x33;
	/** 扩展属性：supported groups **/
	public static final int EXTENSION_SUPPORTED_GROUPS = 0x0A;
	/** 扩展属性：supported versions **/
	public static final int EXTENSION_SUPPORTED_VERSIONS = 0x2B;

	/** 取指纹，忽略的扩展 **/
	private static HashMap<Integer, Boolean> fingerprintIgnoreExtension = new HashMap<Integer, Boolean>();
	/** 保留ID **/
	private static HashMap<Integer, Boolean> reservedIdMap = new HashMap<Integer, Boolean>();
	static {
		//忽略的扩展
		fingerprintIgnoreExtension.put(EXTENSION_PADDING, new Boolean(true));
		fingerprintIgnoreExtension.put(EXTENSION_KEY_SHARE, new Boolean(true));
		//保留ID
		reservedIdMap.put(0x0a0a, new Boolean(true));
		reservedIdMap.put(0x1a1a, new Boolean(true));
		reservedIdMap.put(0x2a2a, new Boolean(true));
		reservedIdMap.put(0x3a3a, new Boolean(true));
		reservedIdMap.put(0x4a4a, new Boolean(true));
		reservedIdMap.put(0x5a5a, new Boolean(true));
		reservedIdMap.put(0x6a6a, new Boolean(true));
		reservedIdMap.put(0x7a7a, new Boolean(true));
		reservedIdMap.put(0x8a8a, new Boolean(true));
		reservedIdMap.put(0x9a9a, new Boolean(true));
		reservedIdMap.put(0xaaaa, new Boolean(true));
		reservedIdMap.put(0xbaba, new Boolean(true));
		reservedIdMap.put(0xcaca, new Boolean(true));
		reservedIdMap.put(0xdada, new Boolean(true));
		reservedIdMap.put(0xeaea, new Boolean(true));
		reservedIdMap.put(0xfafa, new Boolean(true));

	}
	private short contentType;
	private int helloVersion;
	private int helloLength;
	private short handshakeType;
	private int handshakeLength;
	private int handshakeVersion;
	private byte[] random;
	private byte[] sessionId;
	private int[] cipherSuites = null;
	private short[] compressionMethods = null;
	private List<Extension> extensions = null;
	
	
	public SSLClientHello(ByteArray data) {
		ByteOrder order = data.getOrder();
		if(order != ByteOrder.BIG_ENDIAN) {
			data.setOrder(ByteOrder.BIG_ENDIAN);
		}
		parseHello(data);
		if(order != ByteOrder.BIG_ENDIAN) {
			data.setOrder(order);
		}
		data.rewind();
	}
	
	private void parseHello(ByteArray data) {
		this.contentType = data.readUInt8();
		this.helloVersion = data.readUInt16();
		this.helloLength = data.readUInt16();
		this.handshakeType = data.readUInt8();
		data.readUInt8(); //长度字节：总长2字节，握手长度3字节，忽略多余的1个字节
		this.handshakeLength = data.readUInt16();
		this.handshakeVersion = data.readUInt16();
		//创建动态数组之前，检查是否超长
		if(this.helloLength > MAX_HELLO_PACKET_LENGTH) {
			throw new HelloFormatException("ClientHello packet length error: " + this.helloLength);
		}
		if(this.handshakeLength > this.helloLength) {
			throw new HelloFormatException("ClientHello handshake packet length error: " + this.handshakeLength);
		}
		this.random = new byte[32]; //随机数组，固定32字节
		data.readBytesTo(this.random);
		int sessionIdLength = data.readUInt8();
		if(sessionIdLength > 0) {
			if(sessionIdLength > this.handshakeLength) {
				throw new HelloFormatException("ClientHello session id length error: " + sessionIdLength);
			}
			this.sessionId = new byte[sessionIdLength];
			data.readBytesTo(this.sessionId);
		}
		int cipherSuiteLenth = data.readUInt16();
		if(cipherSuiteLenth > this.handshakeLength) {
			throw new HelloFormatException("ClientHello cipher suites length error: " + cipherSuiteLenth);
		}
		cipherSuiteLenth /= 2; //每个加密套件占用2个字节
		this.cipherSuites = new int[cipherSuiteLenth];
		for(int i=0; i<cipherSuiteLenth; i++) {
			this.cipherSuites[i] = data.readUInt16();
		}
		int compressionLength = data.readUInt8();
		if(compressionLength > this.handshakeLength) {
			throw new HelloFormatException("ClientHello compression length error: " + compressionLength);
		}
		this.compressionMethods = new short[compressionLength]; //一个压缩方法，占用1个字节
		for(int i=0; i<compressionLength; i++) {
			this.compressionMethods[i] = data.readUInt8();
		}		
		int extensionsLength = data.readUInt16();
		if(extensionsLength > this.handshakeLength) {
			throw new HelloFormatException("ClientHello extension total length error: " + extensionsLength);
		}
		this.extensions = new ArrayList<Extension>();
		for(int i=0; i<extensionsLength; i+=4) {
			int type = data.readUInt16();
			int len = data.readUInt16();
			i += len;
			if(i > extensionsLength) {
				throw new HelloFormatException("ClientHello extension length error: " + extensionsLength);
			}
			byte[] bts = new byte[len];
			if(len > 0) {
				data.readBytesTo(bts);
			}
			this.extensions.add(new Extension(type, bts));
		}		
	}
	
	/** 获取主机名 **/
	public String getHostName() {
		if(this.extensions == null) {
			return null;
		}
		for(Extension extension : this.extensions) {
			if(extension.type != EXTENSION_SERVER_NAME) {
				continue;
			}
			for(Extension.ServerName serverName : extension.getServerNames()) {
				if(serverName.type == Extension.ServerName.TYPE_HOST_NAME) {
					return new String(serverName.name, EncodeUtils.Charsets.UTF_8);
				}
			}
		}
		return null;
	}
	
	public String getFingerprint() {
		ByteBuffer buff = ByteBufferPool.mallocMiddle();
		try {
			buff.order(ByteOrder.BIG_ENDIAN); //网络字节序
			buff.putShort((short)this.handshakeVersion);
			if(this.cipherSuites != null) {
				for(int i=0; i<this.cipherSuites.length; i++) {
					if(reservedIdMap.containsKey(this.cipherSuites[i])) {
						continue; //忽略Reserved (GREASE) 值例子：0x3a3a, 0x8a8a, 0xeaea
					}
					buff.putShort((short)this.cipherSuites[i]);
				}
			}
			if(this.compressionMethods != null) {
				for(int i=0; i<this.compressionMethods.length; i++) {
					buff.put((byte)this.compressionMethods[i]);
				}
			}
			if(this.extensions != null) {
				for(Extension extension : this.extensions) {
					if(fingerprintIgnoreExtension.containsKey(extension.type)) {
						continue;
					}
					if(reservedIdMap.containsKey(extension.type)) {
						continue; //忽略Reserved (GREASE) 值例子：0x3a3a, 0x8a8a, 0xeaea
					}
					if(extension.type == EXTENSION_SUPPORTED_VERSIONS) {
						for(int supportedVersion : extension.getSupportedVersions()) {
							if(reservedIdMap.containsKey(supportedVersion)) {
								continue; //忽略Reserved (GREASE) 值例子：0x3a3a, 0x8a8a, 0xeaea
							}
							buff.putShort((short)supportedVersion);
						}
						continue;
					}
					if(extension.type == EXTENSION_SUPPORTED_GROUPS) {
						for(int supportedGroup : extension.getSupportedGroups()) {
							if(reservedIdMap.containsKey(supportedGroup)) {
								continue; //忽略Reserved (GREASE) 值例子：0x3a3a, 0x8a8a, 0xeaea
							}
							buff.putShort((short)supportedGroup);
						}
						continue;
					}
					buff.putShort((short)0xEFEF);
					buff.putShort((short)extension.type);
					buff.put(extension.data);
				}
			}
			int pos = buff.position();
			byte[] bts = new byte[pos];
			System.arraycopy(buff.array(), 0, bts, 0, bts.length);

			//System.err.print("source: "+EncodeUtils.bin2hex(bts));	
			
			bts = EncodeUtils.digestMD5(bts);
			return EncodeUtils.bin2hex(bts);
		} finally {
			ByteBufferPool.free(buff);
		}
	}
	
	public short getContentType() {
		return contentType;
	}

	public int getHelloLength() {
		return helloLength;
	}

	public int getHelloVersion() {
		return helloVersion;
	}

	public short getHandshakeType() {
		return handshakeType;
	}

	public int getHandshakeLength() {
		return handshakeLength;
	}

	public int getHandshakeVersion() {
		return handshakeVersion;
	}

	public byte[] getRandom() {
		return random;
	}

	public byte[] getSessionId() {
		return sessionId;
	}
	
	public static class Extension {
		
		private int type;
		private byte[] data = null;
		
		private Extension(int type, byte[] data) {
			this.type = type;
			this.data = data;
		}
				
		private List<ServerName> getServerNames() {
			if(type != EXTENSION_SERVER_NAME) {
				throw new HelloFormatException("ClientHello extension type is not server name: " + type);
			}
			ByteBuffer buff = ByteBuffer.wrap(data);
			buff.order(ByteOrder.BIG_ENDIAN);
			int total = buff.getShort() & 0xFFFF;
			if(total > data.length) {
				throw new HelloFormatException("ClientHello server name total length error: " + total);
			}
			ArrayList<ServerName> names = new ArrayList<ServerName>();
			for(int i=0; i<total; i+=3) {
				int nameType = buff.get() & 0xFF;
				int len = buff.getShort() & 0xFFFF;
				i += len;
				if(i > total) {
					throw new HelloFormatException("ClientHello server name length error: " + len);
				}
				byte[] name = new byte[len];
				buff.get(name);
				names.add(new ServerName(nameType, name));
			}
			return names;
		}
		
		private int[] getSupportedGroups() {
			if(type != EXTENSION_SUPPORTED_GROUPS) {
				throw new HelloFormatException("ClientHello extension type is not supported groups: " + type);
			}
			ByteBuffer buff = ByteBuffer.wrap(data);
			buff.order(ByteOrder.BIG_ENDIAN);
			int total = buff.getShort() & 0xFFFF;
			if(total != data.length - 2) {
				throw new HelloFormatException("ClientHello supported groups length error: " + total);
			}
			total /= 2;
			int[] res = new int[total];
			for(int i=0; i<total; i++) {
				res[i] = buff.getShort() & 0xFFFF;
			}
			return res;
		}
		
		private int[] getSupportedVersions() {
			if(type != EXTENSION_SUPPORTED_VERSIONS) {
				throw new HelloFormatException("ClientHello extension type is not supported versions: " + type);
			}
			ByteBuffer buff = ByteBuffer.wrap(data);
			buff.order(ByteOrder.BIG_ENDIAN);
			int total = buff.get() & 0xFFFF;
			if(total != data.length - 1) {
				throw new HelloFormatException("ClientHello supported versions length error: " + total);
			}
			total /= 2;
			int[] res = new int[total];
			for(int i=0; i<total; i++) {
				res[i] = buff.getShort() & 0xFFFF;
			}
			return res;
		}
		
		private static class ServerName {
			static final int TYPE_HOST_NAME = 0x00;
			private int type = 0;
			private byte[] name = null;
			private ServerName(int type,byte[] name) {
				this.type = type;
				this.name = name;
			}
		}
	}

	public static class HelloFormatException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;
		
		public HelloFormatException(String message) {
			super(message);
		}
		
	}

}
