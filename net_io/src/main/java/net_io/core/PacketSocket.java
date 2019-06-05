/**
 * 第一个数据包，必须包含整个头部
 */
package net_io.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import net_io.msg.BaseMsg;
import net_io.msg.MsgClass;
import net_io.utils.NetLog;


public class PacketSocket extends AsyncBaseSocket {
	final public static int LENGTH_SIZE = 4;
	private ByteArray headBuff = new ByteArray(LENGTH_SIZE);
	protected MsgClass msgClass = new MsgClass();

	public PacketSocket() {
		super(initProcessor());
		StatNIO.packetStat.create_packet_socket.incrementAndGet(); //StatNIO
	}
	
	private static AsyncSocketProcessor initProcessor() {
		return new AsyncSocketProcessor() {

			@Override
			public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
				return ((PacketSocket)that).acceptPrecheck(that, socket);
			}

			@Override
			public void onConnect(NetChannel channel) throws Exception {
				StatNIO.packetStat.call_on_connect.incrementAndGet(); //StatNIO
				((PacketSocket)channel.asyncSocket).onConnect(channel);
			}

			@Override
			public void onReceive(NetChannel channel) throws Exception {
				StatNIO.packetStat.call_on_receive.incrementAndGet(); //StatNIO
				PacketSocket that = ((PacketSocket)channel.asyncSocket);
				//that.headBuff = new ByteArray(LENGTH_SIZE); //FIXME
				while(true) {
					if(channel.recvBuff == null) { //新数据包，读取头部
						StatNIO.packetStat.read_times.incrementAndGet(); //StatNIO
						int size = channel.socket.read(that.headBuff.getByteBuffer());
						if(size < 0) { //连接关闭请求
							StatNIO.packetStat.read_close.incrementAndGet(); //StatNIO
							that.closeChannel(channel);
							break;
						} else if(size == 0) {
							StatNIO.packetStat.read_zero.incrementAndGet(); //StatNIO
							break; //本轮socket中的数据取完了
						}
						StatNIO.packetStat.receive_size.addAndGet(size); //StatNIO
						if(that.headBuff.position() < LENGTH_SIZE) { //数据包长度4个字节未取完
							//若是第1个数据包，则为异常，直接关闭连接
							if(channel.socketReadCount == 1) {
								StatNIO.packetStat.error_format_packet.incrementAndGet();
								that.closeChannel(channel);
							}
							break;
						}
						that.headBuff.getByteBuffer().flip();
						long bodySize = that.headBuff.readUInt32() - LENGTH_SIZE;
						that.headBuff.getByteBuffer().clear();
						if(bodySize == 0) {
							StatNIO.packetStat.msg_empty_package.incrementAndGet(); //StatNIO
							continue; //数据包的实体为空
						}
						if(bodySize > BaseMsg.MAX_MSG_LENGTH) {
							NetLog.logWarn("body size("+bodySize+") error.");
							StatNIO.packetStat.error_format_packet.incrementAndGet();
							that.closeChannel(channel);
							break;
						}
						channel.recvBuff = ByteBuffer.allocate((int)bodySize);
					} else {
						StatNIO.packetStat.read_times.incrementAndGet(); //StatNIO
						int size = channel.socket.read(channel.recvBuff);
						if(size < 0) { //连接关闭请求
							StatNIO.packetStat.read_close.incrementAndGet(); //StatNIO
							that.closeChannel(channel);
							break;
						} else if(size == 0) {
							StatNIO.packetStat.read_zero.incrementAndGet(); //StatNIO
						} else if(channel.recvBuff.position() == channel.recvBuff.limit()) { //取满一个数据包了
							StatNIO.packetStat.receive_size.addAndGet(size); //StatNIO
							StatNIO.packetStat.msg_create_invoke.incrementAndGet(); //StatNIO
							//读取 recv buffer 到 ByteArray
							channel.recvBuff.rewind();
							ByteArray data = new ByteArray(channel.recvBuff);
							//取消息ID
							int msgID = data.readUInt16();
							//解析消息
							BaseMsg msg = that.msgClass.createMsg(msgID);
							if(msg == null) {
								StatNIO.packetStat.msg_undefined.incrementAndGet(); //StatNIO
								NetLog.logInfo("undefined msg id: 0x"+Integer.toHexString(msgID));
								continue;
							}
							msg.readData(data);
							//释放 recv buffer
							channel.recvBuff = null;
							
							//触发接收事件
							StatNIO.packetStat.msg_process.incrementAndGet(); //StatNIO
							//System.out.println(DateUtils.getDateTime() +" read cost time: "+(System.currentTimeMillis()-channel.getCreateTime()));
							that.onReceive(channel, msg);
						} else {
							StatNIO.packetStat.msg_wait_read.incrementAndGet(); //StatNIO
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
		StatNIO.packetStat.send_invoke.incrementAndGet(); //StatNIO
		StatNIO.packetStat.send_size.addAndGet(data.size()); //StatNIO
		channel._send(data.getByteBuffer());
	}

	public boolean acceptPrecheck(AsyncBaseSocket that, ServerSocket socket) throws Exception {
		return true;
	}

	public boolean onAccept(ServerSocket socket) throws Exception {
		StatNIO.packetStat.default_on_accept.incrementAndGet(); //StatNIO
		return true;
	}
	
	public void onConnect(NetChannel channel) throws Exception {
		StatNIO.packetStat.default_on_connect.incrementAndGet(); //StatNIO
	}
	public void onClose(NetChannel channel) throws Exception {
		StatNIO.packetStat.default_on_close.incrementAndGet(); //StatNIO
	}
	
	public void onReceive(NetChannel channel, BaseMsg msg) throws Exception {
		StatNIO.packetStat.default_on_receive.incrementAndGet(); //StatNIO
	}
}
