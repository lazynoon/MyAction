package net_io.core.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import net_io.core.ByteBufferPool;

class SSLSocketEngine {
	protected SSLEngine sslEngine = null;
	// 四个buffer缓冲区
//	private ByteBuffer myNetData;
//	private ByteBuffer myAppData;
////	private ByteBuffer peerNetData;
//	private ByteBuffer peerAppData;
	
	private static final ByteBuffer dummy = ByteBuffer.allocate(0);
	private static final int MAX_LOOP_IN_HANDSHAKE = 1000;

	private boolean finished = false;
	private boolean isFirstPacket = true;
	

	protected SSLSocketEngine() {
	}
	
	
	public boolean isHandshakeFinish() {
		return finished;
	}
	
	public ByteBuffer decrypt(ByteBuffer buff) throws SSLException {
		ByteBuffer quickBuff = ByteBufferPool.malloc64K();
		try {
			SSLEngineResult result = sslEngine.unwrap(buff, quickBuff);// 调用SSLEngine进行unwrap操作
			Status status = result.getStatus();
			if(status != Status.OK) {
				throw new SSLException("SSL unwrap error: "+status);
			}
			quickBuff.flip();
			ByteBuffer newBuff = ByteBuffer.allocate(quickBuff.limit());
			newBuff.put(quickBuff);
			newBuff.rewind();
			return newBuff;
		} finally {
			ByteBufferPool.free(quickBuff);
		}
	}
	
	public ByteBuffer encrypt(ByteBuffer buff) throws SSLException {
		ByteBuffer quickBuff = ByteBufferPool.malloc64K();
		try {
			SSLEngineResult result = sslEngine.wrap(buff, quickBuff);// 调用SSLEngine进行unwrap操作
			Status status = result.getStatus();
			if(status != Status.OK) {
				throw new SSLException("SSL wrap error: "+status);
			}
			quickBuff.flip();
			ByteBuffer newBuff = ByteBuffer.allocate(quickBuff.limit());
			newBuff.put(quickBuff);
			newBuff.rewind();
			return newBuff;
		} finally {
			ByteBufferPool.free(quickBuff);
		}
	}
	
	public boolean isFirstPacket() {
		return isFirstPacket;
	}
	
	// 这个方法就是服务器端的握手
	public void doHandshake(SSLChannel channel, ByteBuffer peerNetData, ByteBuffer peerAppData) throws IOException {
		if(isFirstPacket) {
			isFirstPacket = false;
			sslEngine.beginHandshake();// 开始begin握手
		}
		System.out.println("receive size: "+peerNetData.remaining()+", channel: "+channel);
		SSLEngineResult result;
		Status status = null;// SSLEngineResult.Status
		//握手阶段
		HandshakeStatus hsStatus  = sslEngine.getHandshakeStatus();
		int loop = 0;
		for(; loop<MAX_LOOP_IN_HANDSHAKE; loop++) {
//			NetLog.logInfo("1111111111111 handshake status: " + hsStatus);
			if(hsStatus == HandshakeStatus.FINISHED) {
				finished =  true;
				break;
			}
			switch (hsStatus) {// 判断handshakestatus，下一步的动作是什么？
			case NEED_TASK:// 指定delegate任务
				Runnable runnable;
				while ((runnable = sslEngine.getDelegatedTask()) != null) {
					runnable.run();// 因为耗时比较长，所以需要另起一个线程
				}
				hsStatus = sslEngine.getHandshakeStatus();
				break;
			case NEED_UNWRAP:// 需要进行入站了，说明socket缓冲区中有数据包进来了
//				do {
//					result = sslEngine.unwrap(peerNetData, peerAppData);// 调用SSLEngine进行unwrap操作
//					System.out.println("Unwrapping1:" + result + " remaining: "+peerNetData.hasRemaining());
//					// During an handshake renegotiation we might need to
//					// perform
//					// several unwraps to consume the handshake data.
//				} while (result.getStatus() == SSLEngineResult.Status.OK// 判断状态
//						&& result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP
//						&& result.bytesProduced() == 0);
				result = sslEngine.unwrap(peerNetData, peerAppData);// 调用SSLEngine进行unwrap操作
//				if (peerAppData.position() == 0 && status == SSLEngineResult.Status.OK
//						&& peerNetData.hasRemaining()) {
//					result = sslEngine.unwrap(peerNetData, peerAppData);
//					NetLog.logInfo("Unwrapping2:\n" + result);
//				}
				hsStatus = result.getHandshakeStatus();
				status = result.getStatus();
				// Prepare the buffer to be written again.
//					peerNetData.compact();
				// And the app buffer to be read.
				if(status == Status.BUFFER_OVERFLOW) {
					throw new IOException("SSL Handshake peer app data buffer overflow. capation: "+peerAppData.capacity());
				} else if(!peerNetData.hasRemaining() && status == Status.BUFFER_UNDERFLOW) { //需要更多数据
					return;
				}
				break;
			case NEED_WRAP:// 需要出栈
				ByteBuffer myNetData = ByteBufferPool.malloc64K();
				try {
					result = sslEngine.wrap(dummy, myNetData);// 意味着从应用程序中发送数据到socket缓冲区中，先wrap
					hsStatus = result.getHandshakeStatus();
					if(result.getStatus() != Status.OK) {
						throw new IOException("SSL Handshake wrap error: " + "channel: "+channel+", status: "+hsStatus);
					}
					myNetData.flip();
					channel.sendHandshakeData(myNetData);// 最后再发送socketchannel
				} finally {
					ByteBufferPool.free(myNetData);
				}
				break;
			default:
				throw new IOException("SSL Handshake status error: " + hsStatus);
			}
		}
		//返回结果
		peerAppData.flip();
		//协议错误检查
		if(!finished && loop < MAX_LOOP_IN_HANDSHAKE) {
			throw new IOException("SSL Handshake reach max loop error: " + loop);
		}
	}
	
	

	
}
