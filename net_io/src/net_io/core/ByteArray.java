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
	public static final ByteOrder ENDIAN = ByteOrder.LITTLE_ENDIAN;
	/**
	 * ByteArray编码类型。
	 */
	public static final String CHARSET = "utf-8";
	/** 最大单次读取长度 **/
	public static final int MAX_LENGTH = 1024 * 1024 * 2; 

	private ByteBuffer buff;
	private int capacity;
	private int maxCapacity;
		
	public ByteArray(int capacity) {
		buff = ByteBuffer.allocate(capacity);
		buff.order(ENDIAN);
		this.capacity = capacity;
		this.maxCapacity = this.capacity;
	}
	
	public ByteArray(int capacity, int maxCapacity) {
		buff = ByteBuffer.allocate(capacity);
		buff.order(ENDIAN);
		this.capacity = capacity;
		this.maxCapacity = maxCapacity;
	}

	public ByteArray(ByteBuffer buff) {
		this.buff = buff;
		buff.order(ENDIAN);
		this.capacity = buff.capacity();
		this.maxCapacity = this.capacity;
	}
	public ByteBuffer getByteBuffer() {
		return buff;
	}
	public static ByteArray wrap(String str, String charset) throws UnsupportedEncodingException {
		byte[] bytes = str.getBytes(charset);
		ByteArray arr = new ByteArray(ByteBuffer.wrap(bytes));
		return arr;
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
		autoIncreaseBuff(buff.remaining()); //检查并自动扩充数组
		buff.order(ENDIAN);
		System.arraycopy(buff.array(), buff.position(), this.buff.array(), this.buff.position(), buff.remaining());
		this.buff.position(this.buff.position() + buff.limit());
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
		return buff.getInt() & 0xFFFFFFFF;
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
	 * @param size
	 * @return
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
			buff.putInt(0);
			return;
		}
		buff.putInt(list.size());
		for(int i=0; i<list.size(); i++) {
			T obj = list.get(i);
			if(obj instanceof MsgReadWrite) {
				((MsgReadWrite)obj).writeData(this);
			} else if(obj instanceof Byte) {
				buff.put(((Byte)obj).byteValue());
			} else if(obj instanceof Short) {
				buff.putShort(((Short)obj).shortValue());
			} else if(obj instanceof Integer) {
				buff.putInt(((Integer)obj).intValue());
			} else if(obj instanceof Long) {
				buff.putLong(((Long)obj).byteValue());
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
		int avaliable = this.capacity - buff.position();
		if(avaliable >= needSize) {
			return;
		}
		int minIncrease = needSize - avaliable;
		int maxIncrease = this.maxCapacity - this.capacity;
		if(minIncrease > maxIncrease) {
			throw new ArrayIndexOutOfBoundsException("ByteArray capacity: "+this.capacity+", need increase: "+minIncrease);
		}
		int increaseNum = Math.min(minIncrease, Math.max(Math.round(minIncrease * 1.1f), 1024));
		byte[] dstBytes = new byte[capacity+increaseNum];
		buff.get(dstBytes, 0, buff.position());
		ByteBuffer dstBuff = ByteBuffer.wrap(dstBytes);
		dstBuff.position(buff.position());
		buff = dstBuff;
	}
}
