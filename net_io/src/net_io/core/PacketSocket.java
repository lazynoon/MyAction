package net_io.core;

import java.io.IOException;
import java.net.ServerSocket;

import net_io.msg.BaseMsg;
import net_io.msg.MsgClass;
import net_io.utils.NetLog;


public class PacketSocket extends AsyncBaseSocket {
	final public static int LENGTH_SIZE = 4;
	private ByteArray headBuff = new ByteArray(LENGTH_SIZE);
	protected MsgClass msgClass = new MsgClass();

	public PacketSocket() {
		super(initProcessor());
	}
	
	private static AsyncSocketProcessor initProcessor() {
		return new AsyncSocketProcessor() {

			@Override
			public void onConnect(NetChannel channel) throws Exception {
				((PacketSocket)channel.asyncSocket).onConnect(channel);
			}

			@Override
			public void onReceive(NetChannel channel) throws Exception {
				PacketSocket that = ((PacketSocket)channel.asyncSocket);
				while(true) {
					if(channel.recvBuff == null) { //新数据包，读取头部
						int size = channel.socket.read(that.headBuff.getByteBuffer());
						if(size < 0) { //连接关闭请求
							that.closeChannel(channel);
							break;
						} else if(size == 0) {
							break; //本轮socket中的数据取完了
						}
						that.headBuff.getByteBuffer().flip();
						int bodySize = that.headBuff.readInt32() - LENGTH_SIZE;
						that.headBuff.getByteBuffer().clear();
						if(bodySize == 0) {
							continue; //数据包的实体为空
						}
						NetLog.logDebug("bodySize: "+bodySize);
						channel.recvBuff = ByteBufferPool.malloc(bodySize);
					} else {
						int size = channel.socket.read(channel.recvBuff);
						if(size < 0) { //连接关闭请求
							that.closeChannel(channel);
							break;
						}
						if(channel.recvBuff.position() == channel.recvBuff.limit()) { //取满一个数据包了
							//读取 recv buffer 到 ByteArray
							channel.recvBuff.rewind();
							ByteArray data = new ByteArray(channel.recvBuff);
							//取消息ID
							int msgID = data.readUInt16();
							//解析消息
							BaseMsg msg = that.msgClass.createMsg(msgID);
							if(msg == null) {
								NetLog.logDebug("undefined msg id: 0x"+Integer.toHexString(msgID));
								continue;
							}
							msg.readData(data);
							//释放 recv buffer
							ByteBufferPool.free(channel.recvBuff);
							channel.recvBuff = null;
							
							//触发接收事件
							that.onReceive(channel, msg);
						} else {
							break; //本轮socket中的数据取完了，但还未装满一个包，下轮接着取
						}
					}
				}
			}

			@Override
			public void onClose(NetChannel channel) throws Exception {
				((PacketSocket)channel.asyncSocket).onClose(channel);
			}
			
		};
	}
	
	public void send(NetChannel channel, ByteArray data) throws IOException {
//		int totalSize = data.size() + HEAD_SIZE;
//		ByteArray msg = new ByteArray(data.size() + HEAD_SIZE);
//		msg.writeInt32(totalSize);
//		msg.writeInt16(0xDF12);
//		msg.writeInt16(0xCF12);
//		msg.append(data);
//		msg.finishWrite();
		NetLog.logDebug("Send buff size: "+data.size());
		channel._send(data.getByteBuffer());
	}

	public boolean onAccept(ServerSocket socket) throws Exception {
		return true;
	}
	
	public void onConnect(NetChannel channel) throws Exception {
		;
	}
	public void onClose(NetChannel channel) throws Exception {
		;
	}
	
	public void onReceive(NetChannel channel, BaseMsg msg) throws Exception {
		;
	}
}
