/**
 * 常用编码解码类
 * 
 * 
 * 
 * 64编码：
 *    字节：1      2      3
 *    原码：8      16     24
 *    编码：2(4)   3(2)   4
 * 
 * 32编码：
 *    字节：1      2      3      4      5 
 *    原码：8      16     24     32     40
 *    编码：2(3)   4(1)   5(1)   7(3)   8
 * 
 * @author Hansen
 */
package net_io.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class EncodeUtils {

	/** 支持http协议的62进制编码 **/
	private static byte[] myBase62EncodeMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes(Charsets.ISO_8859_1);
	private static final byte[] myBase62DecodeMap = new byte[256];
	private static final long[] myBase62Mode = {1L, 62L, 62L*62L, 62L*62L*62L, 62L*62L*62L*62L, 62L*62L*62L*62L*62L, 62L*62L*62L*62L*62L*62L};
	/** 支持http协议的64进制编码 **/
	private static byte[] encodeHttp64Map = "-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes(Charsets.ISO_8859_1);
	private static final byte[] decodeHttp64Map = new byte[127];
	/** 支持http协议的32进制编码 **/
	private static byte[] encodeHttp32Map = "0123456789abcdefghjkmnpqrstuvxyz".getBytes(Charsets.ISO_8859_1);
	private static final byte[] decodeHttp32Map = new byte[127];
	/** 标准base64编码 **/
	private static byte[] encodeBase64Map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(Charsets.ISO_8859_1);
	private static final byte[] decodeBase64Map = new byte[127];
	private static final byte base64BackfillChar = '=';
	/** bit位掩码 **/
	private static int[] bitMask = {0, 1, 3, 7, 15, 31, 63, 127};
	/** long型1字节未掩码 **/
	private static final long MASK_1_BYTE_LONG = 0xFFL;
	/** 自增量 **/
	private static AtomicLong autoIncrement = new AtomicLong(0);


	static {
		//32进制编码
		for(int i=0; i<decodeHttp32Map.length; i++) {
			decodeHttp32Map[i] = -1;
		}
		for(int i=0; i<encodeHttp32Map.length; i++) {
			decodeHttp32Map[encodeHttp32Map[i]] = (byte)i;
		}
		//62进制编码
		for(int i=0; i<myBase62DecodeMap.length; i++) {
			myBase62DecodeMap[i] = -1;
		}
		for(int i=0; i<myBase62EncodeMap.length; i++) {
			myBase62DecodeMap[myBase62EncodeMap[i]] = (byte)i;
		}
		//64进制编码
		for(int i=0; i<decodeHttp64Map.length; i++) {
			decodeHttp64Map[i] = -1;
		}
		for(int i=0; i<encodeHttp64Map.length; i++) {
			decodeHttp64Map[encodeHttp64Map[i]] = (byte)i;
		}
		//base64编码
		for(int i=0; i<decodeBase64Map.length; i++) {
			decodeBase64Map[i] = -1;
		}
		for(int i=0; i<encodeBase64Map.length; i++) {
			decodeBase64Map[encodeBase64Map[i]] = (byte)i;
		}
	}
	
	/** MD5使用的16字节表示字符 **/
	private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	
	private static final String MD5 = "MD5";
	private static final String SHA1 = "SHA-1";

	private static final Map<String,Queue<MessageDigest>> queues = new HashMap<String,Queue<MessageDigest>>();
	
	
	private EncodeUtils() {
		// Hide default constructor for this utility class
	}
	
	static {
		try {
			// Init commonly used algorithms
			init(MD5);
			init(SHA1);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 检查类是否存在（不初始化类的静态属性）
	 */
	public static boolean isClassExist(String className) {
		try {
			Thread.currentThread().getContextClassLoader().loadClass(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	
	/**
	 * 预知可靠byte[]数据流量，按UTF-8编码转换为String
	 */
	public static String toStringUTF8(byte[] bts) {
		if(bts == null) return null;
		return new String(bts, Charsets.UTF_8);
	}

//	/**
//	 * 预知可靠String类型，按UTF-8编码转换为byte[]数据流
//	 */
//	public static byte[] toBytesUTF8(String str) {
//		if(str == null) return null;
//		return str.getBytes(CHARSETS.UTF_8);
//	}

	/**
	 * int类型转换为byte[]类型（高位在前）
	 */
	public static int bytesToInt(byte[] buff, int offset) {
		int loopEnd = Math.min(buff.length, offset+4);
		int moveBit = (loopEnd - offset - 1) * 8;
		int ret = 0;
		for(int i=offset; i<loopEnd; i++) {
			ret |= (buff[i] & 0xFF) << moveBit;
			moveBit -= 8;
		}
		return ret;
	}
	
	/**
	 * byte[]类型转换为int类型（高位在前）
	 */
	public static void intToBytes(int num, byte[] buff, int offset) {
		int loopEnd = Math.min(buff.length, offset+4);
		int moveBit = (loopEnd - offset - 1) * 8;
		for(int i=offset; i<loopEnd; i++) {
			buff[i] = (byte)((num >>> moveBit) & 0xFF); 
			moveBit -= 8;
		}
	}
	
	/**
	 * 创建内含时间戳的随机字符串。16字节，96位空间，支持不编码http协议传输
	 */
	public static String createTimeRandId() {
		long time = System.currentTimeMillis();
		int[] ret = new int[3];
		ret[0] = (int)(time / 1000);
		long num1 = time % 1000; //毫秒
		long num2 = autoIncrement.incrementAndGet() & 0x3FF;
		long num3 = System.nanoTime() & 0x3FF;
		ret[1] = (int)num1 << 22; //1 ~ 10 bit
		ret[1] |= (int)num2 << 10;
		ret[1] |= (int)num3;
		long num4 = Math.round(Math.random() * 0x100000000L);
		ret[2] = (int)num4;
		byte[] buff = new byte[12];
		intToBytes(ret[0], buff, 0);
		intToBytes(ret[1], buff, 4);
		intToBytes(ret[2], buff, 8);
		return toStringUTF8(encodeHttp64(buff));
	}
	
	public static long parseRandTime(String randId) {
		if(randId == null) {
			return 0;
		}
		int[] ret = new int[2];
		byte[] buff = decodeHttp64(randId.getBytes(Charsets.UTF_8));
		ret[0] = EncodeUtils.bytesToInt(buff, 0);
		ret[1] = EncodeUtils.bytesToInt(buff, 4);
		long time = ((long)ret[0] & 0xFFFFFFFFL) * 1000;
		time += (ret[1] >>> 22) & 0x3FF;
		return time;
	}

	/** 随机数填充 **/
	public static void fillRandomNumber(byte[] bts) {
		if(bts == null) {
			return;
		}
		int randNum = (int)(Math.random() * Integer.MAX_VALUE);
		for(int i=0; i<bts.length; i++) {
			randNum ^= ((int)(Math.random() * 0xFFFF)) | System.nanoTime();
			bts[i] = (byte)(randNum & 0xFF);
		}
	}

	/**
	 * 转换字符串为首字母大写
	 * @param str 源字符
	 * @return 若字符串以英文单词开头则返回首字母大写，否则返回源字符串
	 */
	public static String stringUpperFirst(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		char firstChar = str.charAt(0);
		if (firstChar >= 'a' && firstChar <= 'z') {
			firstChar -= 32;
			return firstChar + str.substring(1);
		} else {
			return str;
		}
	}

	/**
	 * 转换字符串为首字母小写
	 * @param str 源字符
	 * @return 若字符串以英文单词开头则返回首字母小写，否则返回源字符串
	 */
	public static String stringLowerFirst(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		char firstChar = str.charAt(0);
		if (firstChar >= 'A' && firstChar <= 'Z') {
			firstChar += 32;
			return firstChar + str.substring(1);
		} else {
			return str;
		}
	}

	/**
	 * 按62进制编码（编码率：7/5）
	 * @param bts 二进制数组
	 * @return 编码字符范围：0-9A-Za-z
	 */
	public static String myBase62Encode(byte[] bts) {
		if(bts == null) {
			return null;
		}
		int mode = bts.length % 5;
		if(mode >= 3) {
			mode += 2;
		} else if(mode >= 1) {
			mode++;
		}
		byte[] buff = new byte[bts.length / 5 * 7 + mode];
		int offset = 0;
		for(int loop=0; loop<bts.length; loop+=5) {
			int byteCount = bts.length - loop;
			if(byteCount > 5) {
				byteCount = 5;
			}
			int modeOffset = byteCount;
			if(byteCount >= 3) {
				modeOffset += 2;
			} else {
				modeOffset++;
			}
			int moveBit = (byteCount - 1) * 8;
			long num = 0;
			for(int i=loop; i<bts.length && moveBit >= 0; i++) {
				num |= (bts[i] & MASK_1_BYTE_LONG) << moveBit;
				moveBit -= 8;
			}
			for(int i=1; i<modeOffset; i++) {
				buff[offset++] = myBase62EncodeMap[(int) (num / myBase62Mode[modeOffset - i])];
				num %= myBase62Mode[modeOffset - i];
			}
			buff[offset++] = myBase62EncodeMap[(int) (num % 62L)];
		}
		return new String(buff, Charsets.ISO_8859_1);
	}

	/**
	 * 按62进制编码（编码率：7/5）
	 * @param str 62进制编码的字字符串（编码字符范围：0-9A-Za-z）
	 * @return 解密的字节数组
	 */
	public static byte[] myBase62Decode(String str) {
		if(str == null) {
			return null;
		}
		byte[] bts = str.getBytes(Charsets.ISO_8859_1);
		int mode = bts.length % 7;
		if(mode >= 5) {
			mode -= 2;
		} else if(mode >= 1) {
			mode--;
		}
		byte[] buff = new byte[bts.length / 7 * 5 + mode];
		int offset = 0;
		for(int loop=0; loop<bts.length; loop+=7) {
			int byteCount = bts.length - loop;
			if(byteCount > 7) {
				byteCount = 7;
			}
			long num = 0;
			for(int i=0; i<byteCount; i++) {
				int chNum = myBase62DecodeMap[bts[loop+i] & 0xFF];
				if(chNum >= 0) {
					num += chNum * myBase62Mode[byteCount - i - 1];
				} else {
					//TODO
				}
			}
			int modeOffset = byteCount;
			if(byteCount >= 5) {
				modeOffset -= 2;
			} else {
				modeOffset--;
			}
			int moveBit = (modeOffset - 1) * 8;
			for(; moveBit>=0; moveBit-=8) {
				buff[offset++] = (byte) ((num >>> moveBit) & MASK_1_BYTE_LONG);
			}
		}
		return buff;
	}

	/** 支持HTTP协议的base64编码（字符升序，扩展字符用用".-"代替） **/
	public static byte[] encodeHttp64(byte[] bts) {
		return _encodeBase64(encodeHttp64Map, bts, false);
	}

	/** 支持HTTP协议的base64编码（字符升序，扩展字符用用".-"代替）**/
	public static String encodeHttp64ToString(byte[] bts) {
		bts = _encodeBase64(encodeHttp64Map, bts, false);
		if(bts == null) {
			return null;
		}
		return new String(bts, Charsets.US_ASCII);
	}

	/** base64解码（字符升序，扩展字符用用".-"代替） **/
	public static byte[] decodeHttp64(byte[] bts) {
		return _decodeBase64(decodeHttp64Map, bts, false);
	}

	/** base64解码（字符升序，扩展字符用用".-"代替） **/
	public static byte[] decodeHttp64(String str) {
		if(str == null) {
			return null;
		}
		return _decodeBase64(decodeHttp64Map, str.getBytes(Charsets.US_ASCII), false);
	}

	/** base64编码 **/
	public static byte[] encodeBase64(byte[] bts) {
		return _encodeBase64(encodeBase64Map, bts, true);
	}

	/** base64编码 **/
	public static String encodeBase64ToString(byte[] bts) {
		bts = _encodeBase64(encodeBase64Map, bts, true);
		if(bts == null) {
			return null;
		}
		return new String(bts, Charsets.US_ASCII);
	}

	/** base64解码 **/
	public static byte[] decodeBase64(byte[] bts) {
		return _decodeBase64(decodeBase64Map, bts, true);
	}

	/** base64解码 **/
	public static byte[] decodeBase64(String str) {
		if (str == null) {
			return null;
		}
		return _decodeBase64(decodeBase64Map, str.getBytes(Charsets.US_ASCII), true);
	}

	/**
	 * 转换bigint为12字节的字符串
	 * @param bts 待编码数据
	 * @return string
	 */
	public static byte[] encodeHttp32(byte[] bts) {
		if(bts == null) {
			return null;
		}
		int buffSize = bts.length * 8 / 5;
		if(bts.length * 8 % 5 > 0) {
			buffSize++;
		}
		int offset = 0;
		byte[] buff = new byte[buffSize];
		int prevBitCount = 0;
		int prevBitNum = 0;
		for(int i=0; i<bts.length; i++) {
			int nextBitCount = 3 + prevBitCount;
			int nextBitNum = bts[i] & bitMask[nextBitCount];
			int numIndex = (bts[i] >>> nextBitCount) & bitMask[8-nextBitCount];
			if(prevBitCount > 0) {
				numIndex |= (byte)(prevBitNum << (5 - prevBitCount));
			}
			buff[offset++] = encodeHttp32Map[numIndex];
			if(nextBitCount > 5) {
				numIndex = (nextBitNum >>> (nextBitCount-5)) & bitMask[7];
				buff[offset++] = encodeHttp32Map[numIndex];
				prevBitCount = nextBitCount - 5;
				prevBitNum = nextBitNum & bitMask[prevBitCount];
			} else if(nextBitCount == 5) {
				buff[offset++] = encodeHttp32Map[nextBitNum];
				prevBitCount = 0;
				prevBitNum = 0;
			} else {
				prevBitCount = nextBitCount;
				prevBitNum = nextBitNum;				
			}
		}
		if(prevBitCount > 0) {
			buff[offset++] = encodeHttp32Map[prevBitNum << (5 - prevBitCount)];
		}
		return buff;
	}

	public static byte[] decodeHttp32(byte[] bts) {
		if(bts == null) {
			return null;
		}
		int buffSize = bts.length * 5 / 8;
		int lastPosition = bts.length - 1;
		byte[] buff = new byte[buffSize]; //创建buff
		int offset = 0;
		int prevBitCount = 0;
		for(int i=0; i<bts.length; i++) {
			int ch = bts[i] & 0xFF;
			int num = 0; //默认为0
			if(ch < decodeHttp32Map.length && decodeHttp32Map[ch] > 0) {
				num = decodeHttp32Map[ch];
			}
			if(prevBitCount == 0) {
				buff[offset] = (byte) (num << 3);
				prevBitCount = 3;
			} else if(prevBitCount > 5) {
				buff[offset] |= (byte) (num << (prevBitCount - 5));
				prevBitCount = prevBitCount - 5;				
			} else if(prevBitCount == 5) {
				buff[offset++] |= (byte)num;
				prevBitCount = 0;
			} else { // 1~4
				buff[offset++] |= (byte) (num >>> (5-prevBitCount));
				if(i < lastPosition) {
					buff[offset] = (byte) ((num & bitMask[5-prevBitCount]) << 3+prevBitCount);
				}
				prevBitCount = 3 + prevBitCount;
			}
		}
		return buff;
	}

	public static byte[] encodeGZIP(byte[] bts) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(bts);
		gzip.close();
		return out.toByteArray();
	}

	public static byte[] decodeGZIP(byte[] bts) throws IOException {
		GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bts));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buff = new byte[1024];
		int size;
		while((size = gzip.read(buff)) > 0) {
			out.write(buff, 0, size);
		}
		gzip.close();
		return out.toByteArray();
	}

	/**
	 * 二进制数组，转为16进制表示字符串
	 * @param bts
	 * @return String
	 */
	public static String bin2hex(byte[] bts) {
		if(bts == null) {
			return null;
		}
		char myChar[] = new char[bts.length * 2];
		int k = 0;
		for (int i = 0; i < bts.length; i++) {
			byte byte0 = bts[i];
			myChar[k++] = hexDigits[byte0 >>> 4 & 0x0f];
			myChar[k++] = hexDigits[byte0 & 0x0f];
		}
		return new String(myChar);		
	}
	
	/**
	 * MD5编码
	 * @param str 待加密的字符串（系统默认编码）
	 */
	public static String md5(String str) {
		return md5(str, null);
	}
	/**
	 * MD5编码
	 * @param str 待加密的字符串
	 * @param charset 编码格式
	 */
	public static String md5(String str, String charset) {
		try {
			byte[] bts;
			if(charset != null) {
				bts = str.getBytes(charset);
			} else {
				bts = str.getBytes();
			}
			bts = messageDigest("MD5", bts);
			return bin2hex(bts);
		} catch(Exception e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	/**
	 * SH1编码
	 * @param str 待加密的字符串（系统默认编码）
	 */
	public static String sha1(String str) {
		return sha1(str, null);
	}
	/**
	 * SH1编码
	 * @param str 待加密的字符串
	 * @param charset 编码格式
	 */
	public static String sha1(String str, String charset) {
		try {
			byte[] bts;
			if(charset != null) {
				bts = str.getBytes(charset);
			} else {
				bts = str.getBytes();
			}
			bts = messageDigest("SHA-1", bts);
			return base64Encode(bts);
		} catch(Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * @deprecated
	 */
	public static String base64Encode(byte[] bts) {
		return encodeBase64ToString(bts);
	}

	/**
	 * @deprecated
	 */
	public static byte[] base64Decode(String str) {
		return decodeBase64(str);
	}

	/** base64编码算法实现 **/
	private static byte[] _encodeBase64(byte[] encodeMap, byte[] bts, boolean backfill) {
		if(bts == null) {
			return null;
		}
		int buffSize = bts.length * 4 / 3;
		int mode = bts.length % 3;
		if(mode > 0) {
			buffSize++;
			if(backfill) {
				if (mode == 1) {
					buffSize += 2;
				} else {
					buffSize++;
				}
			}
		}
		int offset = 0;
		byte[] buff = new byte[buffSize];
		//最后1位或2位，补填充字符“=”
		if(backfill && mode > 0) {
			buff[buffSize-1] = base64BackfillChar;
			if (mode == 1) {
				buff[buffSize - 2] = base64BackfillChar;
			}
		}
		int prevBitCount = 0;
		int prevBitNum = 0;
		for(int i=0; i<bts.length; i++) {
			int nextBitCount = 2 + prevBitCount;
			int nextBitNum = bts[i] & bitMask[nextBitCount];
			int index = (bts[i] >>> (2 + prevBitCount)) & bitMask[6-prevBitCount];
			index |= prevBitNum << (6 - prevBitCount);
			buff[offset++] = encodeMap[index];
			if(nextBitCount >= 6) {
				buff[offset++] = encodeMap[nextBitNum];
				prevBitCount = 0;
				prevBitNum = 0;
			} else {
				prevBitCount = nextBitCount;
				prevBitNum = nextBitNum;
			}
		}
		if(prevBitCount > 0) {
			if(backfill) { //标准BASE64编码，移至高位
				if (prevBitCount < 6) {
					prevBitNum <<= 6 - prevBitCount;
				}
			}
			buff[offset++] = encodeMap[prevBitNum];
		}
		return buff;
	}

	/** base64解码算法实现 **/
	private static byte[] _decodeBase64(byte[] decodeMap, byte[] bts, boolean backfill) {
		if(bts == null) {
			return null;
		}
		int srcSize = bts.length;
		if(backfill) {
			for(int i=1; i<8 && srcSize > 0; i++) {
				if(bts[srcSize - 1] != base64BackfillChar) {
					break;
				}
				srcSize--;
			}
		}
		int buffSize = srcSize * 3 / 4;
		int mode = srcSize % 4;
		int lastBitCount = 0;
		int lastPosition = srcSize - 1;
		if(mode > 0) {
			if(mode == 3) {
				lastBitCount = 4;
			} else {
				lastBitCount = 2;
			}
		}
		byte[] buff = new byte[buffSize]; //创建buff
		int offset = 0;
		int prevBitCount = 0;
		for(int i=0; i<srcSize; i++) {
			int ch = bts[i] & 0xFF;
			int num = 0; //默认为0
			if(ch < decodeMap.length && decodeMap[ch] > 0) {
				num = decodeMap[ch];
			}
			if(prevBitCount == 0) {
				buff[offset] = (byte) (num << 2);
				prevBitCount = 2;
			} else if(prevBitCount == 6) {
				buff[offset++] |= (byte)num;
				prevBitCount = 0;
			} else if(i == lastPosition && lastBitCount > 0) {
				if(backfill) { //标准BASE64编码
					buff[offset++] |= num >>> (6-lastBitCount) & bitMask[lastBitCount];
				} else {
					buff[offset++] |= num & bitMask[lastBitCount];
				}
			} else {
				buff[offset++] |= (byte) (num >>> (6-prevBitCount));
				buff[offset] = (byte) ((num & bitMask[6-prevBitCount]) << 2+prevBitCount);
				prevBitCount = 2 + prevBitCount;
			}
		}
		return buff;
	}

	private static byte[] messageDigest(String mode, byte[] bts) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance(mode);
		messageDigest.update(bts);
		return  messageDigest.digest();
	}
	
	public static byte[] digestMD5(byte[]... input) {
		return digest(MD5, input);
	}
	
	public static byte[] digestSHA1(byte[]... input) {
		return digest(SHA1, input);
	}
	
	private static byte[] digest(String algorithm, byte[]... input) {
		return digest(algorithm, 1, input);
	}
	
	
	private static byte[] digest(String algorithm, int rounds, byte[]... input) {
	
		Queue<MessageDigest> queue = queues.get(algorithm);
		if (queue == null) {
			throw new IllegalStateException("Must call init() first");
		}
	
		MessageDigest md = queue.poll();
		if (md == null) {
			try {
				md = MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				// Ignore. Impossible if init() has been successfully called
				// first.
				throw new IllegalStateException("Must call init() first");
			}
		}
	
		// Round 1
		for (byte[] bytes : input) {
			md.update(bytes);
		}
		byte[] result = md.digest();
	
		// Subsequent rounds
		if (rounds > 1) {
			for (int i = 1; i < rounds; i++) {
				md.update(result);
				result = md.digest();
			}
		}
	
		queue.add(md);
	
		return result;
	}
	
	
	/**
	 * Ensures that {@link #digest(String, byte[][])} will support the specified
	 * algorithm. This method <b>must</b> be called and return successfully
	 * before using {@link #digest(String, byte[][])}.
	 *
	 * @param algorithm The message digest algorithm to be supported
	 *
	 * @throws NoSuchAlgorithmException If the algorithm is not supported by the
	 *								  JVM
	 */
	private static void init(String algorithm) throws NoSuchAlgorithmException {
		synchronized (queues) {
			if (!queues.containsKey(algorithm)) {
				MessageDigest md = MessageDigest.getInstance(algorithm);
				Queue<MessageDigest> queue = new ConcurrentLinkedQueue<MessageDigest>();
				queue.add(md);
				queues.put(algorithm, queue);
			}
		}
	}

	public static class Charsets {
		/**
		 * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
		 * Unicode character set
		 */
		public static final Charset US_ASCII = Charset.forName("US-ASCII");
		/**
		 * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
		 */
		public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
		/**
		 * Eight-bit UCS Transformation Format
		 */
		public static final Charset UTF_8 = Charset.forName("UTF-8");
	}

}
