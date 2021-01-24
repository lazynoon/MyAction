package net_io.core;

import net_io.core.ssl.SSLSocketServer;
import net_io.myaction.tool.HttpHeader;
import net_io.utils.EncodeUtils;
import net_io.utils.MixedUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

abstract public class WebSocketProtocol {
	private _WebSocketHandle handle = new _WebSocketHandle(this);
	private StreamSocket socket;
	private ConcurrentHashMap<String, WebSocketChannel> sidMap = new ConcurrentHashMap<String, WebSocketChannel>();
	private ConcurrentHashMap<NetChannel, WebSocketChannel> channelMap = new ConcurrentHashMap<NetChannel, WebSocketChannel>();
	private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(EncodeUtils.Charsets.ISO_8859_1);
	private static final int FLAG_TRANSPORT_MODE = 0x8;
	private static final int FLAG_TRANSPORT_START = 0x16;
	private static final int MAX_DATA_LENGTH = 8 * 1024 * 1024;
	// OP Codes
	public static final byte OPCODE_CONTINUATION = 0x00;
	public static final byte OPCODE_TEXT = 0x01;
	public static final byte OPCODE_BINARY = 0x02;
	public static final byte OPCODE_CLOSE = 0x08;
	public static final byte OPCODE_PING = 0x09;
	public static final byte OPCODE_PONG = 0x0A;

	public WebSocketProtocol() {
		socket = new StreamSocket(handle);
	}

	public WebSocketProtocol(boolean secure) {
		if(secure) {
			socket = new StreamSocket(handle);
		} else {
			socket = new SSLSocketServer(handle);
		}
	}

	public void bind(int port) throws IOException {
		socket.bind(port);
	}

	abstract protected boolean onAccept(InetSocketAddress address);

	abstract protected boolean onCreateInbound(WebSocketChannel channel, String head);

	abstract protected void onReceive(WebSocketChannel channel, ByteArray data) throws Exception ;

	abstract protected void onClose(WebSocketChannel channel) throws Exception;

	public void close(String sid) throws IOException {
		WebSocketChannel conn = sidMap.get(sid);
		if(conn == null) {
			throw new IOException("connect id not exists: "+sid);
		}
		conn.channel.close();
	}
	public WebSocketChannel getConn(String sid) {
		return sidMap.get(sid);
	}
	public NetChannel getNetChannel(String sid) {
		WebSocketChannel conn = sidMap.get(sid);
		if(conn == null) {
			return null;
		}
		return conn.channel;
	}

	protected void sendMsg(String sid, ByteBuffer buff) throws IOException {
		WebSocketChannel conn = sidMap.get(sid);
		if(conn == null) {
			throw new IOException("connect id not exists: "+sid);
		}
		conn.channel.send(buff);
	}

	private class _WebSocketHandle implements SocketEventHandle {
		private WebSocketProtocol that;
		private _WebSocketHandle(WebSocketProtocol that) {
			this.that = that;
		}

		public boolean onAccept(ServerSocket socket) throws Exception {
			//socket.getInetAddress()
			SocketAddress address = socket.getLocalSocketAddress();
			if(address instanceof InetSocketAddress) {
				return that.onAccept((InetSocketAddress)address);
			} else {
				//FIXME
				return true;
			}
		}

		@Override
		public boolean onAccept(InetSocketAddress address) throws Exception {
			return true;
		}

		@Override
		public void onConnect(NetChannel channel) throws Exception {
			String sid = EncodeUtils.createTimeRandId();
			WebSocketChannel conn = new WebSocketChannel(sid, channel);
			sidMap.put(sid, conn);
			channelMap.put(channel, conn);
		}

		@Override
		public void onClose(NetChannel channel) throws Exception {
			WebSocketChannel conn = channelMap.remove(channel);
			if(conn != null) {
				sidMap.remove(conn.sid);
				that.onClose(conn);
			}
		}

		@Override
		public void onReceive(NetChannel channel, ByteArray data) throws Exception {
			WebSocketChannel conn = channelMap.get(channel);
			if((conn.flag & FLAG_TRANSPORT_MODE) == 0) { //head区域
				ByteArray buff = conn.buff;
				buff.append(data);
				int position = buff.position();
				if(position >= 4) {
					buff.position(position - 4);
					int b1 = buff.readByte();
					int b2 = buff.readByte();
					int b3 = buff.readByte();
					int b4 = buff.readByte();
					if(b1 == '\r' && b2 == '\n' && b3 == '\r' && b4 == '\n') { //找到head结束符
						conn.flag |= FLAG_TRANSPORT_MODE;
						byte[] bts = buff.readBytesBack();
						String head = new String(bts, 0, position - 4);
						HttpHeader header = HttpHeader.parse(head);
						boolean pass = true;
						String secKey = header.getHeader("Sec-WebSocket-Key");
						if(MixedUtils.isEmpty(secKey)) {
							pass = false;
						}
						if(pass && that.onCreateInbound(conn, head)) { //允许连接
							StringBuffer accept = new StringBuffer();
							accept.append("HTTP/1.1 101 Switching Protocols");
							accept.append("\r\n");
							accept.append("Connection: Upgrade");
							accept.append("\r\n");
							accept.append("Upgrade: WebSocket");
							accept.append("\r\n");
							accept.append("Sec-WebSocket-Accept: ");
							accept.append(EncodeUtils.base64Encode(EncodeUtils.digestSHA1(secKey.getBytes(EncodeUtils.Charsets.ISO_8859_1), WS_ACCEPT)));
							accept.append("\r\n");
							accept.append("\r\n");
							conn.channel.send(ByteBuffer.wrap(accept.toString().getBytes(EncodeUtils.Charsets.UTF_8)));
						} else {
							pass = false;
						}
						if(!pass) {
							channel.close(); //关闭连接
						}
					}
				}
			} else { //后续字节流
				//帧中的静荷数据(payload data)长度小于0x7E的为小帧, 静荷数据长度 >=0x7E又<=0x10000的为中帧, 大帧8字节
				if((conn.flag & FLAG_TRANSPORT_START) == 0) { //数据包的第1帧
					conn.flag |= FLAG_TRANSPORT_START; //数据包已开始
					data.getByteBuffer().order(ByteOrder.BIG_ENDIAN);
					byte b1 = data.readByte(); //FIN, RSV, RSV, RSV, opcode(4)
					byte b2 = data.readByte(); //MASK, Payload len(7)
					conn.dataLength = b2 & 0x7F;
					int opcode = b1 & 0xF;
					//关闭连接的操作码
					if(opcode == OPCODE_CLOSE) {
						channel.close(); //关闭连接
						return;
					}

					if(conn.dataLength == 126) {
						conn.dataLength = data.readUInt16();
					} else if(conn.dataLength == 127) {
						conn.dataLength = data.readUInt64();
					}
					if((b2 & 0x80) != 0) {
						conn.mask = new byte[4];
						data.readBytesTo(conn.mask);
					}
					if(conn.dataLength > MAX_DATA_LENGTH) {
						throw new ProtocolException("data length error. " + conn.dataLength);
					}
					int length = (int)conn.dataLength;
					conn.receiveBuff = new ByteArray(Math.max(length, 8192), length);
				}
				//读取数据包中的内容
				int maxRead = (int)conn.dataLength - conn.receiveBuff.position();
				int i;
				for(i=0; i<maxRead && data.hasRemaining(); i++) {
					byte b = data.readByte();
					if(conn.mask != null) {
						b ^= conn.mask[conn.receiveBuff.position() % 4];
					}
					conn.receiveBuff.writeByte(b);
				}
				if(i == maxRead) { //数据读完了
					that.onReceive(conn, conn.receiveBuff);
					if(data.hasRemaining()) {
						//TODO: 帧中未处理数据
					}
					conn.receiveBuff = null;
					conn.flag &= ~ FLAG_TRANSPORT_START; //数据包已结束，可重新开始
				}
			}
		}

	}
	
}
