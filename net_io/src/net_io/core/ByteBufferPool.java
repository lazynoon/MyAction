package net_io.core;

import java.nio.ByteBuffer;

public class ByteBufferPool {
	public static int MAX_BUFFER_SIZE = 1024 * 1024; //默认buffer大小最大为1M
	final public static int FREE_POOL_SIZE = 100;
	public static ByteBuffer[] freePool = new ByteBuffer[FREE_POOL_SIZE];
	
	public static ByteBuffer malloc(int capacity) {
		int i = 0;
		ByteBuffer buff = null;
		synchronized(freePool) {
			//查找是否有可用buff
			int start = (int)(Math.random() * FREE_POOL_SIZE);
			int nullPos = -1;
			//正向搜索
			for(i=start; i<FREE_POOL_SIZE; i++) {
				if(freePool[i] == null) {
					if(nullPos < 0) {
						nullPos = 0;
					}
					continue;
				}
				if(freePool[i] != null && freePool[i].capacity() >= capacity) {
					buff = freePool[i];
					buff.limit(capacity);
					freePool[i] = null;
					return buff;
				}
			}
			//反向搜索
			for(i=start-1; i>=0; i--) {
				if(freePool[i] != null && freePool[i].capacity() >= capacity) {
					buff = freePool[i];
					buff.limit(capacity);
					freePool[i] = null;
					return buff;
				}
			}
			//没搜索到，重新创建
			buff = ByteBuffer.allocate(capacity);
//			buff.order(ByteOrder.LITTLE_ENDIAN); //小字节序的ByteOrder
			return buff;
		}
	}
	
	public static void free(ByteBuffer buff) {
		if(buff == null) {
			return;
		}
		int i = 0;
		synchronized(freePool) {
			//查找是否有空位置
			int start = (int)(Math.random() * FREE_POOL_SIZE);
			int nullPos = -1;
			//正向搜索
			for(i=start; i<FREE_POOL_SIZE; i++) {
				if(freePool[i] == null) {
					nullPos = i;
					break;
				}
			}
			//反向搜索
			if(nullPos < 0) {
				for(i=start-1; i>=0; i--) {
					if(freePool[i] == null) {
						nullPos = i;
						break;
					}
				}
			}
			//没搜索到，重新创建
			if(nullPos >= 0) {
				buff.clear();
				freePool[nullPos] = buff;
			}
		}
	}
	
	
	
	
}
