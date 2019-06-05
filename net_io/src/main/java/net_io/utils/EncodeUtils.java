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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class EncodeUtils {
	/** 支持http协议 **/
	private static byte[] encode64Map = toBytesUTF8("-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
	private static final byte[] decode64Map = new byte[127];
	private static byte[] encode32Map = toBytesUTF8("0123456789abcdefghjkmnpqrstuvxyz");
	private static final byte[] decode32Map = new byte[127];
	private static int[] bitMask = {0, 1, 3, 7, 15, 31, 63, 127};
	private static AtomicLong autoIncrement = new AtomicLong(0);
	
	static {
		for(int i=0; i<decode64Map.length; i++) {
			decode64Map[i] = -1;
		}
		for(int i=0; i<encode64Map.length; i++) {
			decode64Map[encode64Map[i]] = (byte)i;
		}
		for(int i=0; i<decode32Map.length; i++) {
			decode32Map[i] = -1;
		}
		for(int i=0; i<encode32Map.length; i++) {
			decode32Map[encode32Map[i]] = (byte)i;
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
	 * 预知可靠byte[]数据流量，按UTF-8编码转换为String
	 */
	public static String toStringUTF8(byte[] bts) {
		if(bts == null) return null;
		try {
			return new String(bts, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 预知可靠String类型，按UTF-8编码转换为byte[]数据流
	 */
	public static byte[] toBytesUTF8(String str) {
		if(str == null) return null;
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

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
		int[] ret = new int[2];
		byte[] buff = decodeHttp64(toBytesUTF8(randId));
		ret[0] = EncodeUtils.bytesToInt(buff, 0);
		ret[1] = EncodeUtils.bytesToInt(buff, 4);
		long time = ((long)ret[0] & 0xFFFFFFFFL) * 1000;
		time += (ret[1] >>> 22) & 0x3FF;
		return time;
	}

	/**
	 * 转换bigint为12字节的字符串
	 * @param unknown $bigintArr 格式为 array(int, int)
	 * @param unknown $fixnum 值范围[0-255]。超出此范围，则随机取值
	 * @return string
	 */
	public static byte[] encodeHttp64(byte[] bts) {
		if(bts == null) {
			return null;
		}
		int buffSize = bts.length * 4 / 3;
		if(bts.length * 4 % 3 > 0) {
			buffSize++;
		}
		int offset = 0;
		byte[] buff = new byte[buffSize];
		int prevBitCount = 0;
		int prevBitNum = 0;
		for(int i=0; i<bts.length; i++) {
			int nextBitCount = 2 + prevBitCount;
			int nextBitNum = bts[i] & bitMask[nextBitCount];
			int index = (bts[i] >>> (2 + prevBitCount)) & bitMask[6-prevBitCount];
			index |= prevBitNum << (6 - prevBitCount);
			buff[offset++] = encode64Map[index];
			if(nextBitCount >= 6) {
				buff[offset++] = encode64Map[nextBitNum];
				prevBitCount = 0;
				prevBitNum = 0;
			} else {
				prevBitCount = nextBitCount;
				prevBitNum = nextBitNum;				
			}
		}
		if(prevBitCount > 0) {
			buff[offset++] = encode64Map[prevBitNum];
		}
		return buff;
	}

	public static byte[] decodeHttp64(byte[] bts) {
		if(bts == null) {
			return null;
		}
		int buffSize = bts.length * 3 / 4;
		int mode = bts.length % 4;
		int lastBitCount = 0;
		int lastPosition = bts.length - 1;
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
		for(int i=0; i<bts.length; i++) {
			int ch = bts[i] & 0xFF;
			int num = 0; //默认为0
			if(ch < decode64Map.length && decode64Map[ch] > 0) {
				num = decode64Map[ch];
			}
			if(prevBitCount == 0) {
				buff[offset] = (byte) (num << 2);
				prevBitCount = 2;
			} else if(prevBitCount == 6) {
				buff[offset++] |= (byte)num;
				prevBitCount = 0;
			} else if(i == lastPosition && lastBitCount > 0) {
				buff[offset++] |= num & bitMask[lastBitCount];
			} else {
				buff[offset++] |= (byte) (num >>> (6-prevBitCount));
				buff[offset] = (byte) ((num & bitMask[6-prevBitCount]) << 2+prevBitCount);
				prevBitCount = 2 + prevBitCount;
			}
		}
		return buff;
	}
	
	/**
	 * 转换bigint为12字节的字符串
	 * @param unknown $bigintArr 格式为 array(int, int)
	 * @param unknown $fixnum 值范围[0-255]。超出此范围，则随机取值
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
			buff[offset++] = encode32Map[numIndex];
			if(nextBitCount > 5) {
				numIndex = (nextBitNum >>> (nextBitCount-5)) & bitMask[7];
				buff[offset++] = encode32Map[numIndex];
				prevBitCount = nextBitCount - 5;
				prevBitNum = nextBitNum & bitMask[prevBitCount];
			} else if(nextBitCount == 5) {
				buff[offset++] = encode32Map[nextBitNum];
				prevBitCount = 0;
				prevBitNum = 0;
			} else {
				prevBitCount = nextBitCount;
				prevBitNum = nextBitNum;				
			}
		}
		if(prevBitCount > 0) {
			buff[offset++] = encode32Map[prevBitNum << (5 - prevBitCount)];
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
			if(ch < decode32Map.length && decode32Map[ch] > 0) {
				num = decode32Map[ch];
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

	/**
	 * 二进制数组，转为16进制表示字符串
	 * @param bts
	 * @return
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
	 * @param charset 编码格式
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
	 * @param charset 编码格式
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
	
	public static String base64Encode(byte[] bts) {
		return new BASE64Encoder().encode(bts);
	}
	
	public static byte[] base64Decode(String str) {
		try {
			return new BASE64Decoder().decodeBuffer(str);
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
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
	

}
