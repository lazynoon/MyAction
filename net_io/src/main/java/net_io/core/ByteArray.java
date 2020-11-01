package net_io.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import net_io.msg.MsgReadWrite;

public class ByteArray {
	/**
	 * 低字节在前。
	 */
	private static final ByteOrder ENDIAN = ByteOrder.LITTLE_ENDIAN;
	/**
	 * ByteArray编码类型。
	 */
	public static final String CHARSET = "utf-8";
	/** 最大单次读取长度 **/
	public static final int MAX_LENGTH = 1024 * 1024 * 8; 

	private ByteBuffer buff;
	private int maxCapacity;
		
	public ByteArray() {
		buff = ByteBuffer.allocate(8192);
		buff.order(ENDIAN);
		this.maxCapacity = MAX_LENGTH;		
	}
	
	public ByteArray(int capacity) {
		buff = ByteBuffer.allocate(capacity);
		buff.order(ENDIAN);
		this.maxCapacity = MAX_LENGTH;
	}
	
	public ByteArray(int capacity, int maxCapacity) {
		buff = ByteBuffer.allocate(capacity);
		buff.order(ENDIAN);
		this.maxCapacity = maxCapacity;
	}

	public ByteArray(ByteBuffer buff) {
		this.buff = buff;
		buff.order(ENDIAN);
		this.maxCapacity = buff.capacity();
	}
	
	public ByteBuffer getByteBuffer() {
		return buff;
	}
	
	public void setByteBuffer(ByteBuffer buff) {
		buff.order(this.buff.order());
		this.buff = buff;
	}
	
	/** 获取字节顺序 **/
	public ByteOrder getOrder() {
		return buff.order();
	}
	
	/** 设置字节顺序 **/
	public void setOrder(ByteOrder order) {
		buff.order(order);
	}
	
	public static ByteArray wrap(String str, String charset) throws UnsupportedEncodingException {
		byte[] bytes = str.getBytes(charset);
		ByteArray arr = new ByteArray(ByteBuffer.wrap(bytes));
		return arr;
	}
	
	/**
	 * 复制 源ByteBuffer 中的剩余字节到
	 * @param src 源ByteBuffer
	 * @return 非空ByteArray
	 */
	public static ByteArray copy(ByteBuffer src) {
		int srcPostion = src.position();
		ByteBuffer dst = ByteBuffer.allocate(src.remaining());
		dst.put(src);
		src.position(srcPostion);
		return new ByteArray(dst);
	}
	
	public int size() {
		return buff.limit();
	}
	public int position() {
		return buff.position();
	}
	public void position(int position) {
		buff.position(position);
	}
	public void append(ByteArray data) {
		this.append(data.getByteBuffer());
	}
	public void append(ByteBuffer buff) {
		int remainingNum = buff.remaining();
		autoIncreaseBuff(remainingNum); //检查并自动扩充数组
		this.buff.put(buff);
	}
	public void rewind() {
		buff.rewind();
	}
	public boolean hasRemaining() {
		return buff.hasRemaining();
	}
	/**
	 * 写入完成时调用
	 */
	public void finishWrite() {
		buff.flip();
	}
	public byte readInt8() {
		return buff.get();
	}
	public byte readByte() {
		return buff.get();
	}
	
	public short readInt16() {
		return buff.getShort();
	}
	
	public int readInt32() {
		return buff.getInt();
	}
	
	public long readInt64() {
		return buff.getLong();
	}
	
	public short readUInt8() {
		return (short)(buff.get() & 0xFF);
	}
	
	public int readUInt16() {
		return buff.getShort() & 0xFFFF;
	}
	
	public long readUInt32() {
		return buff.getInt() & 0xFFFFFFFFL;
	}
	
	public long readUInt64() {
		return buff.getLong();
	}
	public boolean readBoolean() {
		return buff.get() != 0;
	}
	
	public byte[] readBytes() {
		return readBytesDirect(buff.getInt());
	}
	
	public void readBytesTo(byte[] dst) {
		buff.get(dst);
	}
	
	public byte[] readBytesDirect(int size) {
		if(size > MAX_LENGTH) {
			throw new IllegalArgumentException("read size overflow: "+size);
		}
		byte[] data = new byte[size];
		for(int i=0; i<size; i++) {
			data[i] = buff.get();
		}
		return data;		
	}

	/**
	 * Returns the byte array that backs this buffer  (optional operation). 
	 * Modifications to this buffer's content will cause the returned array's content to be modified, and vice versa.
	 * Invoke the hasArray method before invoking this method in order to ensure that this buffer has an accessible backing array. 
	 * @return byte[]
	 */
	public byte[] readBytesBack() {
		return buff.array();		
	}
	
	public void writeBytes(byte[] data) {
		autoIncreaseBuff(4); //检查并自动扩充数组
		if(data == null || data.length == 0) {
			buff.putInt(0);
			return;
		}
		autoIncreaseBuff(data.length); //检查并自动扩充数组
		buff.putInt(data.length);
		buff.put(data);
	}
	
	public void writeBytesDirect(byte[] data) {
		if(data == null || data.length == 0) {
			return;
		}
		autoIncreaseBuff(data.length); //检查并自动扩充数组
		buff.put(data);
	}
	
	public void writeInt8(byte p) {
		autoIncreaseBuff(1); //检查并自动扩充数组
		buff.put(p);
	}
	public void writeByte(byte p) {
		autoIncreaseBuff(1); //检查并自动扩充数组
		buff.put(p);
	}
	public void writeByte(int p) {
		autoIncreaseBuff(1); //检查并自动扩充数组
		buff.put((byte)p);
	}
	
	public void writeInt16(int p) {
		autoIncreaseBuff(2); //检查并自动扩充数组
		buff.putShort((short)p);
	}
	public void writeInt16(short p) {
		autoIncreaseBuff(2); //检查并自动扩充数组
		buff.putShort(p);
	}
	
	public void writeInt32(int p) {
		autoIncreaseBuff(4); //检查并自动扩充数组
		buff.putInt(p);
	}
	
	public void writeInt64(long p) {
		autoIncreaseBuff(8); //检查并自动扩充数组
		buff.putLong(p);
	}
	
	public void writeUInt8(int p) {
		autoIncreaseBuff(1); //检查并自动扩充数组
		buff.put((byte)p);
	}
	
	public void writeUInt16(int p) {
		autoIncreaseBuff(2); //检查并自动扩充数组
		buff.putShort((short)p);
	}
	
	public void writeUInt32(long p) {
		autoIncreaseBuff(4); //检查并自动扩充数组
		buff.putInt((int)p);
	}
	
	public void writeUInt64(long p) {
		autoIncreaseBuff(8); //检查并自动扩充数组
		buff.putLong(p);
	}
	
	public void writeBoolean(boolean p) {
		autoIncreaseBuff(1); //检查并自动扩充数组
		buff.put(p ? (byte)1 : (byte)0);
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> readArray(Class<T> itemType) {
		int count = buff.getInt();
		if(count <= 0) {
			return null;
		}
		List<T> list = new ArrayList<T>();
		for(int i=0; i<count; i++) {
			T obj;
			if(MsgReadWrite.class.isAssignableFrom(itemType)) {
				try {
					obj = itemType.newInstance();
					((MsgReadWrite)obj).readData(this);
				} catch (InstantiationException e) {
					throw new RuntimeException("[InstantiationException] "+e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("[IllegalAccessException] "+e);
				}
			} else if(itemType.isInstance(Byte.class)) {
				obj = (T)Byte.valueOf(buff.get());
			} else if(itemType.isInstance(Short.class)) {
				obj = (T)Short.valueOf(buff.get());
			} else if(itemType.isInstance(Integer.class)) {
				obj = (T)Integer.valueOf(buff.get());
			} else if(itemType.isInstance(Long.class)) {
				obj = (T)Long.valueOf(buff.get());
			} else {
				throw new RuntimeException("Un-support class type: "+itemType.getName());
			}
			list.add(obj);
		}
		return list;
	}
	
	public <T> void writeArray(List<T> list) {
		if(list == null || list.size() == 0) {
			writeInt32(0);
			return;
		}
		buff.putInt(list.size());
		for(int i=0; i<list.size(); i++) {
			T obj = list.get(i);
			if(obj instanceof MsgReadWrite) {
				((MsgReadWrite)obj).writeData(this);
			} else if(obj instanceof Byte) {
				writeByte(((Byte)obj).byteValue());
			} else if(obj instanceof Short) {
				writeUInt16(((Short)obj).shortValue());
			} else if(obj instanceof Integer) {
				writeInt32(((Integer)obj).intValue());
			} else if(obj instanceof Long) {
				writeInt64(((Long)obj).byteValue());
			} else {
				throw new RuntimeException("Un-support class type: "+obj.getClass().getName());
			}

		}
	}
	
	protected String readString(int bytes) {
		if(bytes > MAX_LENGTH) {
			throw new IllegalArgumentException("read size overflow: "+bytes);
		}
		byte[] arr = new byte[bytes];
		buff.get(arr);
//		int i = 0;
//		for(byte b : arr) {
//			if(b == 0) {
//				break;
//			}
//			i++;
//		}
		try {
//			return new String(arr, 0, i, CHARSET);
			return new String(arr, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UnsupportedEncodingException: "+e.getMessage());
		}
	}
	
	public String readString() {
		int bytes = buff.getInt(); //字符数量
		if(bytes <= 0) {
			return null;
		}
		return readString(bytes);
	}
	
	public void writeString(String str) {
		autoIncreaseBuff(4); //检查并自动扩充数组
		try {
			//空字符串，只写入数量字段
			if(str == null || str.length() == 0) {
				buff.putInt(0);
				return;
			}
			byte[] arr = str.getBytes(CHARSET);
			buff.putInt(arr.length); //字符数量
			autoIncreaseBuff(arr.length); //检查并自动扩充数组
			for(int i = 0; i<arr.length; i++) {
				buff.put(arr[i]);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UnsupportedEncodingException: "+e.getMessage());
		}
	}

//	protected void writeString(String str, int bytes) {
//		try {
//			int i = 0;
//			if(str != null) {
//				byte[] arr = str.getBytes(CHARSET);
//				int cnt = Math.min(arr.length, bytes);
//				for(; i<cnt; i++) {
//					buff.put(arr[i]);
//				}
//			}
//			//补0
//			for(; i<bytes; i++) {
//				buff.put((byte)0);
//			}
//		} catch (UnsupportedEncodingException e) {
//			throw new RuntimeException("UnsupportedEncodingException: "+e.getMessage());
//		}
//	}
//	
	public String toString() {
		return new String(buff.array());
	}

	private void autoIncreaseBuff(int needSize) {
		int capacity = buff.capacity();
		int position = buff.position();
		int avaliable = capacity - position;
		if(avaliable >= needSize) {
			return;
		}
		int minIncrease = needSize - avaliable;
		int maxIncrease = this.maxCapacity - capacity;
		if(minIncrease > maxIncrease) {
			throw new ArrayIndexOutOfBoundsException("ByteArray capacity: "+capacity+", need increase: "+minIncrease);
		}
		int increaseNum = Math.max(Math.round(minIncrease * 1.2f), 1024);
		byte[] dstBytes = new byte[capacity+increaseNum];
		byte[] srcBytes = buff.array();
		System.arraycopy(srcBytes, 0, dstBytes, 0, position);
		ByteBuffer dstBuff = ByteBuffer.wrap(dstBytes);
		dstBuff.position(position);
		dstBuff.order(buff.order());
		buff = dstBuff;
	}
}
