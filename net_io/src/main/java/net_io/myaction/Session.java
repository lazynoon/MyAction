package net_io.myaction;

import net_io.utils.ByteUtils;
import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Session class
 */
public class Session {
	/** Session ID 明文长度 **/
	private static final int SESSION_ID_PLAINTEXT_LENGTH = 20;
	/** Session ID 编码后的长度 **/
	private static final int SESSION_ID_ENCODED_LENGTH = 28;
	/** 自增量 **/
	private static AtomicLong autoIncrement = new AtomicLong(0);

	/** 生成哈希值的密钥（可通过 config 修改） **/
	private static long defaultHashCodeKey = 0x123456789ABCDEF0l;
	/** 生成哈希值的密钥ID（可通过 config 修改）  **/
	private static long defaultHashCodeKeyId = 0;
	/** 是否校验末尾3字节哈希值 **/
	private static boolean defaultCheckHashCode = false;

	/** Session数据集 **/
	private Mixed data = new Mixed();
	/** Session Id 解析错误（仅在创建Session对象时使用，null表示成功） **/
	private String parseError = null;
	/** Session序列化签名密钥 **/
	private long hashCodeKey;
	/** 字符串类型的Session ID **/
	private String sessionIdString;
	/** 字节数组类型的Session ID **/
	private byte[] sessionIdBytes;
	/** 标记的会话数据长度 **/
	private int markedSessionDataLength = -1;
	/** 标记的会话数据长度 **/
	private String markedSessionDataHash = null;

	private Session() {}

	/**
	 * 全新创建Session对象
	 * @return 空的会话对象
	 */
	public static Session newInstance() {
		Session session = new Session();
		session.sessionIdBytes = generateSessionIdBytes();
		session.sessionIdString = EncodeUtils.myBase62Encode(session.sessionIdBytes);
		return session;
	}

	/**
	 * 根据已有会话ID，创建Session对象
	 * @param sessionId 会话ID（编码错误，则抛出异常）
	 * @return 空数据集的会话对象
	 */
	public static Session newInstance(String sessionId) {
		return newInstance(sessionId, defaultCheckHashCode);
	}

	/**
	 * 根据已有会话ID，创建Session对象
	 * @param sessionId 会话ID（编码错误，则抛出异常）
	 * @param checkHashCode 是否检验会话ID的哈希值
	 * @return 空数据集的会话对象
	 */
	public static Session newInstance(String sessionId, boolean checkHashCode) {
		Session session = uncheckParseSessionId(sessionId, defaultHashCodeKey, checkHashCode);
		if(session.parseError != null) {
			throw new RuntimeException(session.parseError);
		}
		return session;
	}

	/**
	 * 配置生成Hash编码使用的密钥
	 * @param hashCodeKey long型密钥
 	 */
	public static void configHashCodeKey(long hashCodeKey) {
		defaultHashCodeKey = hashCodeKey;
	}

	/**
	 * 配置是否忽略HashCode检验错误
	 * @param checkHashCode true 忽略， false HashCode检验错误时，不可用现有会话ID创建Session
	 */
	public static void configCheckHashCode(boolean checkHashCode) {
		defaultCheckHashCode = checkHashCode;
	}

	/**
	 * 生成哈希值的密钥ID
	 * @param keyId 可用值范围 [0-255]
	 */
	public static void configHashCodeKeyId(int keyId) {
		defaultHashCodeKeyId = keyId;
	}

	/**
	 * Session对象是否包含指定key
	 * @param key KEY
	 * @return 存在返回 true，或者返回 false
	 */
	public boolean containsKey(String key) {
		return data.containsKey(key);
	}

	/**
	 * 从Session对象取值
	 * @param key KEY
	 * @return 不存在返回 null
	 */
	public Mixed getValue(String key) {
		if (!data.containsKey(key)) {
			return null;
		}
		return data.get(key);
	}

	/**
	 * 设置Session对象的值
	 * @param key KEY
	 * @param value 值
	 */
	public void setValue(String key, Object value) {
		data.put(key, value);
	}

	/**
	 * 从Session移除KEY
	 * @param key KEY
	 */
	public void removeValue(String key) {
		if(key == null) {
			return;
		}
		if (!data.containsKey(key)) {
			return;
		}
		Mixed newData = new Mixed();
		for(String k : data.keys()) {
			if(key.equals(k)) {
				continue;
			}
			newData.put(k, data.get(k));
		}
		data = newData;
	}

	/**
	 * 重设Session对象数据集
	 * @param data Session对象数据集
	 */
	public void resetSessionData(Mixed data) {
		if(data == null || data.size() == 0) {
			data = new Mixed();
		} else if (data.type() != Mixed.ENTITY_TYPE.MAP) {
			throw new IllegalArgumentException("Session data is not MAP type.");
		}
		this.data = data;
	}

	/**
	 * 获取Session创建时间
	 * @return Session创建时间
	 */
	public long getSessionCreateTime() {
		return ByteUtils.parseLongAsBigEndian(sessionIdBytes, 0, 6);
	}

	/**
	 * 获取会话ID
	 * @return 会话ID
	 */
	public String getSessionId() {
		return sessionIdString;
	}

	/**
	 * 获取Session中的KEY列表
	 * @return KEY列表（不返回null）
	 */
	public String[] getSessionKeys() {
		return data.keys();
	}

	/**
	 * 当前Session是否空的
	 * @return 不存在任何KEY，则返回true，否则返回false
	 */
	public boolean isSessionEmpty() {
		return data.isSelfEmpty();
	}

	/**
	 * 会话ID的哈希值是否正确
	 * @return true OR false
	 */
	public boolean isSessionIdHashValid() {
		return getSessionIdHashCode() == calculateHashCode(sessionIdBytes, defaultHashCodeKey);
	}

	/**
	 * 通过指定HashCodeKey计算的会话ID的哈希值是否正确
	 * @return true OR false
	 */
	public boolean isSessionIdHashValid(long hashCodeKey) {
		return getSessionIdHashCode() == calculateHashCode(sessionIdBytes, hashCodeKey);
	}

	/**
	 * 获取会话ID的哈希值
	 * @return 会话ID的哈希值（有效值范围为3字节）
	 */
	public int getSessionIdHashCode() {
		return ByteUtils.parseIntAsBigEndian(sessionIdBytes, SESSION_ID_PLAINTEXT_LENGTH - 3, 3);
	}

	/**
	 * 获取哈希密钥ID
	 * @return 哈希密钥ID（有效值范围为1字节）
	 */
	public int getSessionHashKeyId() {
		return ByteUtils.parseIntAsBigEndian(sessionIdBytes, SESSION_ID_PLAINTEXT_LENGTH - 4, 1);
	}

	/**
	 * 检查 Session ID 是否正确（当 ignoreHashCodeInvalid=false，则忽略 hashCode 检验）
	 * @param sessionId 会话ID
	 * @return true OR false
	 */
	public static boolean isSessionIdValid(String sessionId) {
		return isSessionIdValid(sessionId, defaultHashCodeKey);
	}

	/**
	 * Session数据集序列化
	 * @return JSON格式字符串，非空，空对象返回 {}
	 */
	public String serializeSessionData() {
		if(data.isSelfEmpty()) {
			return "{}";
		}
		return data.toJSON();
	}

	/**
	 * 检查 Session ID 是否正确（当 ignoreHashCodeInvalid=false，则忽略 hashCode 检验）
	 * @param sessionId 会话ID
	 * @return true OR false
	 */
	public static boolean isSessionIdValid(String sessionId, long hashCodeKey) {
		if (sessionId == null || sessionId.length() != SESSION_ID_ENCODED_LENGTH) {
			return false;
		}
		byte[] bts = EncodeUtils.myBase62Decode(sessionId);
		if(bts == null || bts.length != SESSION_ID_PLAINTEXT_LENGTH) {
			return false;
		}
		int hashCode = ByteUtils.parseIntAsBigEndian(bts, SESSION_ID_PLAINTEXT_LENGTH - 3, 3);
		return hashCode == calculateHashCode(bts, hashCodeKey);
	}

	/**
	 * 标记Session数据集的哈希值
	 * @return Session数据集的哈希值（MD5编码）
	 */
	public String markSessionDataHash() {
		String str = toMarkDataString();
		markedSessionDataLength = str.length();
		markedSessionDataHash = EncodeUtils.md5(str);
		return markedSessionDataHash;
	}

	/**
	 * 距离上次标记，Session数据是否已改变
	 * @return 从未标记或Session数据改变则返回true，否则返回false
	 */
	public boolean isSessionDataChange() {
		String str = toMarkDataString();
		if(markedSessionDataLength != str.length()) {
			return true;
		}
		if(markedSessionDataHash == null || !markedSessionDataHash.equals(EncodeUtils.md5(str))) {
			return true;
		}
		return false;
	}

	/** 返回用于生成数据集哈希的字符串 **/
	private String toMarkDataString() {
		String str;
		if(data.isSelfEmpty()) {
			str = "";
		} else {
			str = data.toJSON();
		}
		return str;
	}

	/**
	 * 不抛异常解析会话ID
	 * @param sessionId 会话ID
	 * @param hashCodeKey 哈希密钥
	 * @param checkHashCode
	 * @return 非空Session对象，checkError != null 表示错误，不可用
	 */
	private static Session uncheckParseSessionId(String sessionId, long hashCodeKey, boolean checkHashCode) {
		Session session = new Session();
		session.hashCodeKey = hashCodeKey;
		if (sessionId == null || sessionId.length() != SESSION_ID_ENCODED_LENGTH) {
			session.parseError = "Session Id length is not " + SESSION_ID_ENCODED_LENGTH + ".";
			return session;
		}
		session.sessionIdString = sessionId;
		session.sessionIdBytes = EncodeUtils.myBase62Decode(sessionId);
		if (session.sessionIdBytes == null || session.sessionIdBytes.length != SESSION_ID_PLAINTEXT_LENGTH) {
			session.parseError = "Session Id encoding error.";
			return session;
		}
		if (checkHashCode) {
			if (!session.isSessionIdHashValid()) {
				session.parseError = "Session Id hash code error.";
				return session;
			}
		}
		return session;
	}

	/**
	 * 生成字节数组类型的会话ID
	 * @return 会话ID
	 */
	private static byte[] generateSessionIdBytes() {
		byte[] buff = new byte[SESSION_ID_PLAINTEXT_LENGTH];
		int offset = 0;
		long createTime = System.currentTimeMillis();
		createTime <<= 4;
		createTime |= Math.round(Math.random() * 0xFF) & 0xF;
		long seqNo = autoIncrement.incrementAndGet();
		long nanoTime = System.nanoTime();
		long randNum1 = Math.round(Math.random() * Long.MAX_VALUE);
		randNum1 ^= seqNo;
		randNum1 ^= randNum1 >>> 32;
		long randNum2 = Math.round(Math.random() * Long.MAX_VALUE);
		randNum2 ^= nanoTime;
		randNum2 ^= randNum2 >>> 32;
		nanoTime ^= nanoTime >>> 16;
		nanoTime ^= nanoTime >>> 8;
		offset = ByteUtils.writeNumberAsBigEndian(createTime, buff, offset, 6);
		offset = ByteUtils.writeNumberAsBigEndian(randNum1, buff, offset, 4);
		offset = ByteUtils.writeNumberAsBigEndian(seqNo, buff, offset, 1);
		offset = ByteUtils.writeNumberAsBigEndian(nanoTime, buff, offset, 1);
		offset = ByteUtils.writeNumberAsBigEndian(randNum2, buff, offset, 4);
		offset = ByteUtils.writeNumberAsBigEndian(defaultHashCodeKeyId, buff, offset, 1);
		int hashCode = calculateHashCode(buff, defaultHashCodeKey);
		ByteUtils.writeNumberAsBigEndian(hashCode, buff, offset, 3);
		return buff;
	}

	/**
	 * 计算会话ID的哈希值
	 * @param sessionIdBytes 会话ID
	 * @param hashCodeKey 哈希密钥
	 * @return 哈希值（值范围3字节）
	 */
	private static int calculateHashCode(byte[] sessionIdBytes, long hashCodeKey) {
		long num1 = ByteUtils.parseLongAsBigEndian(sessionIdBytes, 0, 6);
		long num2 = ByteUtils.parseLongAsBigEndian(sessionIdBytes, 0, 5);
		long num3 = ByteUtils.parseLongAsBigEndian(sessionIdBytes, 0, 6);
		num1 ^= hashCodeKey;
		num2 ^= hashCodeKey;
		num3 ^= hashCodeKey;
		num1 ^= num1 >>> 32;
		num2 ^= num2 >>> 35;
		num3 ^= num3 >>> 38;
		long num = num1 ^ num2 ^ num3;
		num ^= num >>> 33;
		num &= 0xFFFFFFL;
		return (int) num;
	}
}
