package net_io.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net_io.utils.NetLog;

public class ByteBufferPool {
	/** 最大行数（动态可调） **/
	private static final int MAX_ROW_NUM = 256;
	/** 行内记录数（固定） **/
	private static final int MAX_COLUMN_NUM = 256;
	/** 不同内存块大小的个数 **/
	private static final int BLOCK_SIZE_COUNT = 3;
	/** 小块内存大小（1KB） **/
	private static final int BLOCK_SIZE_1K = 1024;
	/** 中块内存大小（8KB） **/
	private static final int BLOCK_SIZE_8K = 1024 * 8;
	/** 大块内存大小（64KB） **/
	private static final int BLOCK_SIZE_64K = 1024 * 64;
	/** 垂直方向总大小（小中大3个内存大小的总和） **/
	private static final int TOTAL_VERTICAL_SIZE = BLOCK_SIZE_1K + BLOCK_SIZE_8K + BLOCK_SIZE_64K;
	/** 可用行数（仅读写主存） **/
	private static volatile int limitRowNum = 0; 
	/** 多行多列缓存对象[BLOCK, 行号, 列号] **/
	private static BufferInfo[][][] listPool = new BufferInfo[BLOCK_SIZE_COUNT][][];
	/** HashMap缓存对象 **/
	private static ConcurrentHashMap<ByteBuffer, BufferInfo> mapPool = new ConcurrentHashMap<ByteBuffer, BufferInfo>(MAX_ROW_NUM * MAX_COLUMN_NUM * 6 / 5, 1);
	/** 管理线程 **/
	private static ManagerThread managerThread = null;
	/** 默认字节顺序 **/
	private static ByteOrder defaultOrder = ByteOrder.LITTLE_ENDIAN;
	@Deprecated
	public static int MAX_BUFFER_SIZE = 512 * 1024; //默认buffer大小最大为512K
	
	// 初始化
	static {
		//默认最大内存的30%
		long maxMemory = Runtime.getRuntime().maxMemory();
		long maxApplyMemory = maxMemory * 3 / 10;
		if(maxApplyMemory < TOTAL_VERTICAL_SIZE * MAX_COLUMN_NUM) {
			maxApplyMemory = maxMemory * 5 / 10; //内存不足，则扩展至最大可用内存的50%
		}
		for(int i=0; i<BLOCK_SIZE_COUNT; i++) {
			if(listPool[i] == null) { //仅首次扩展
				listPool[i] = new BufferInfo[MAX_ROW_NUM][];
			}
		}
		configMaxApplyMemory(maxApplyMemory);
	}
	
	synchronized public static void configMaxApplyMemory(long maxApplyMemory) {
		long limit = maxApplyMemory / TOTAL_VERTICAL_SIZE;
		int newRowNum = (int)Math.min(limit, MAX_ROW_NUM);
		int oldRowNum = limitRowNum;
		if(newRowNum > oldRowNum) { //需要扩展
			for(int i=0; i<BLOCK_SIZE_COUNT; i++) {
				if(listPool[i] == null) { //仅首次扩展
					listPool[i] = new BufferInfo[MAX_ROW_NUM][];
				}
				for(int j=oldRowNum; j<newRowNum; j++) {
					listPool[i][j] = new BufferInfo[MAX_COLUMN_NUM];
					for(int k=0; k<MAX_COLUMN_NUM; k++) {
						listPool[i][j][k] = null;
					}
				}
			}			
			limitRowNum = newRowNum; //扩展完了之后再更新
		} else if(newRowNum < oldRowNum) { //需要收缩
			limitRowNum = newRowNum; //收缩前先更新
			for(int i=0; i<BLOCK_SIZE_COUNT; i++) {
				for(int j=newRowNum; j<oldRowNum; j++) {
					for(int k=0; k<MAX_COLUMN_NUM; k++) {
						BufferInfo item = listPool[i][j][k];
						if(item != null) {
							mapPool.remove(item.buff);
							listPool[i][j][k] = null;
						}
					}
					listPool[i][j] = null;
				}
			}			
		}
		//启动管理线程
		if(managerThread == null) {
			managerThread = new ManagerThread();
			managerThread.start();
		}
	}
	
	@Deprecated
	public static ByteBuffer malloc(int capacity) {
		if(capacity > MAX_BUFFER_SIZE) {
			throw new IllegalArgumentException("malloc capacity is too big: "+capacity);
		}
		return ByteBuffer.allocate(capacity);
	}
	
	//状态：空闲 + LOCK，锁定超期，则从缓存池中移除
	
	//TODO: 创建固定大小的buffer
	public static ByteBuffer mallocSmall() {
		return malloc(0, BLOCK_SIZE_1K);
	}
	public static ByteBuffer mallocMiddle() {
		return malloc(1, BLOCK_SIZE_8K);
	}
	public static ByteBuffer mallocLarge() {
		return malloc(2, BLOCK_SIZE_64K);
	}
	
	public static ByteBuffer malloc1K() {
		return malloc(0, BLOCK_SIZE_1K);
	}
	public static ByteBuffer malloc8K() {
		return malloc(1, BLOCK_SIZE_8K);
	}
	public static ByteBuffer malloc64K() {
		return malloc(2, BLOCK_SIZE_64K);
	}
	
	private static ByteBuffer malloc(int sizeIndex, int capacity) {
		ByteBuffer result;
		int randNum = (int)(Math.random() * Integer.MAX_VALUE);
		int rowNo = randNum % limitRowNum;
		int colNo = randNum % MAX_COLUMN_NUM;
		BufferInfo[] row = listPool[sizeIndex][rowNo];
		if(row == null) { //取到了已被收缩了的行
			result = ByteBuffer.allocate(capacity);
			result.order(defaultOrder);
			return result;
		}
		BufferInfo freeBuff = null;
		synchronized(row) {
			for(int i=colNo; i<MAX_COLUMN_NUM; i++) {
				if(row[i] == null) {
					ByteBuffer buff = ByteBuffer.allocate(capacity);
					row[i] = new BufferInfo(buff, sizeIndex, rowNo, i);
				}
				if(row[i].free && !row[i].lock) {
					freeBuff = row[i];
					break;
				}
			}
			if(freeBuff == null) {
				for(int i=colNo-1; i>=0; i--) {
					if(row[i] == null) {
						ByteBuffer buff = ByteBuffer.allocate(capacity);
						row[i] = new BufferInfo(buff, sizeIndex, rowNo, i);
					}
					if(row[i].free && !row[i].lock) {
						freeBuff = row[i];
						break;
					}
				}
			}
			if(freeBuff != null) {
				freeBuff.activeTime = System.currentTimeMillis();
				freeBuff.free = false;
				freeBuff.buff.clear();
				mapPool.put(freeBuff.buff, freeBuff);
			}
		}
		if(freeBuff != null) {
			result = freeBuff.buff;
		} else {
			result = ByteBuffer.allocate(capacity);
		}
		result.order(defaultOrder);
		return result;
	}
	
	public static void free(List<ByteBuffer> list) {
		if(list == null || list.size() == 0) {
			return;
		}
		for(ByteBuffer buff : list) {
			free(buff);
		}
		
	}
	public static void free(ByteBuffer buff) {
		if(buff == null) {
			return;
		}
		BufferInfo busyBuff = mapPool.get(buff);
		if(busyBuff == null) {
			return;
		}
		BufferInfo[] row = listPool[busyBuff.sizeIndex][busyBuff.rowNo];
		if(row == null || busyBuff != row[busyBuff.colNo]) { //Map存在，List不存在。可能并发创建的
			mapPool.remove(buff); //从Map缓存中移除
		}
		busyBuff.activeTime = System.currentTimeMillis();
		busyBuff.free = true;
	}
	
	private static void scan() {
		int limit = limitRowNum;
		long lastTime = System.currentTimeMillis() - 15000;
		for(int i=0; i<BLOCK_SIZE_COUNT; i++) {
			for(int j=0; j<limit; j++) {
				BufferInfo[] row = listPool[i][j];
				if(row == null) {
					continue;
				}
				for(int k=0; k<MAX_COLUMN_NUM; k++) {
					BufferInfo info = row[k];
					if(info == null) {
						continue;
					}
					if(info.activeTime >= lastTime) {
						continue;
					}
					mapPool.remove(info.buff);
					listPool[i][j] = null;
				}
			}
		}
	}
	
	
	private static class BufferInfo {
		/** 最后激活时间（过期后，将从缓存次中删除而不是复用） **/
		long activeTime = 0;
		/** 是否空闲 **/
		volatile boolean free = true;
		/** 是否被锁定（空闲+未锁定，才能被申请） **/
		volatile boolean lock = false;
		/** 缓存对象 **/
		ByteBuffer buff;
		/** 按缓存的：大中小，分别为：0，1，2 **/
		int sizeIndex;
		/** 行号 **/
		int rowNo;
		/** 数据索引（行内位置） **/
		int colNo;
		
		BufferInfo(ByteBuffer buff, int sizeIndex, int rowNo, int colNo) {
			this.buff = buff;
			this.sizeIndex = sizeIndex;
			this.rowNo = rowNo;
			this.colNo = colNo;
		}
	}
	
	private static class ManagerThread extends Thread {
		ManagerThread() {
			setName("ByteBufferPool");
			setDaemon(true);
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					scan();
				} catch(Exception e) {
					NetLog.logWarn(e);
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					NetLog.logWarn(e);
				}
			}
		}
	}
}
