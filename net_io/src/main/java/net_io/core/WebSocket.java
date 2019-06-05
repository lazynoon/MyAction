package net_io.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ssl.SSLSocketServer;
import net_io.myaction.CheckException;
import net_io.myaction.socket.SocketActionProcessor;
import net_io.myaction.tool.HttpHeader;
import net_io.utils.EncodeUtils;
import net_io.utils.MixedUtils;
import net_io.utils.thread.MyThreadPool;

public class WebSocket  {
	private static final String TRANSPORT_MODE_KEY = "net_io.core.WebSocket:TRANSPORT_MODE";
	private static final String HEADER_BUFFER_KEY = "net_io.core.WebSocket:HEADER_BUFFER";
	private static final int MAX_HEAD_LENGTH = 8192;
	private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(StandardCharsets.ISO_8859_1);
	private SocketEventHandle handle;
	private StreamSocket socket;
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
	
	public WebSocket(SocketEventHandle handle) {
		init(handle, false);
	}
	public WebSocket(SocketEventHandle handle, boolean secure) {
		init(handle, secure);
	}
	private void init(SocketEventHandle handle, boolean secure) {
		this.handle = handle;
		if(secure) {
			socket = new SSLSocketServer(new _WebSocketHandle(this));
		} else {
			socket = new StreamSocket(new _WebSocketHandle(this));
		}
	}

	public void bind(int port) throws IOException {
		socket.bind(port);
	}
	
	private boolean processHeader(NetChannel channel, ByteArray data) throws IOException {
		ByteArray buff;
		if(channel.containsChannelStorage(HEADER_BUFFER_KEY)) {
			buff = (ByteArray) channel.getChannelStorage(HEADER_BUFFER_KEY);
			if(buff.size() + data.size() > MAX_HEAD_LENGTH) {
				throw new IOException("header length error: "+(buff.size() + data.size()));
			}
			buff.append(data);
		} else {
			if(data.size() > MAX_HEAD_LENGTH) {
				throw new IOException("header length error: "+(data.size()));
			}
			buff = data;
		}
		//head区域
		boolean pass = false;
		int position = buff.size();
		if(position >= 4) {
			buff.position(position - 4);
			int b1 = buff.readByte();
			int b2 = buff.readByte();
			int b3 = buff.readByte();
			int b4 = buff.readByte();
			if(b1 == '\r' && b2 == '\n' && b3 == '\r' && b4 == '\n') { //找到head结束符
				byte[] bts = buff.readBytesBack();
				String head = new String(bts, 0, position - 4);
				HttpHeader header = HttpHeader.parse(head);
				String secKey = header.getHeader("Sec-WebSocket-Key");
				if(!MixedUtils.isEmpty(secKey)) {
					pass = true;
				}
				if(pass) { //允许连接
					StringBuffer accept = new StringBuffer();
					accept.append("HTTP/1.1 101 Switching Protocols");
					accept.append("\r\n");
					accept.append("Connection: Upgrade");
					accept.append("\r\n");
					accept.append("Upgrade: WebSocket");
					accept.append("\r\n");
					accept.append("Sec-WebSocket-Accept: ");
					accept.append(EncodeUtils.base64Encode(EncodeUtils.digestSHA1(secKey.getBytes(StandardCharsets.ISO_8859_1), WS_ACCEPT)));
					accept.append("\r\n");
					accept.append("\r\n");
					channel.send(ByteBuffer.wrap(accept.toString().getBytes(StandardCharsets.UTF_8)));
				} else {
					channel.close(); //关闭连接
				}
				channel.removeChannelStorage(HEADER_BUFFER_KEY);
			} else {
				channel.setChannelStorage(HEADER_BUFFER_KEY, buff);
			}
		}
		return pass;
	}
	
	private class _WebSocketHandle implements SocketEventHandle {
		private WebSocket that;
		private _WebSocketHandle(WebSocket that) {
			this.that = that;
		}
		
//		public boolean onAccept(ServerSocket socket) throws Exception {
//			SocketAddress address = socket.getLocalSocketAddress();
//			if(address instanceof InetSocketAddress) {
//				return that.onAccept((InetSocketAddress)address);
//			} else {
//				//FIXME
//				return true;
//			}
//		}
//		
		public void onConnect(NetChannel channel) throws Exception {
			that.handle.onConnect(channel);
		}
		public void onClose(NetChannel channel) throws Exception {
			that.handle.onClose(channel);
		}
		
		public void onReceive(NetChannel channel, ByteArray data) throws Exception {
			if(channel.containsChannelStorage(TRANSPORT_MODE_KEY)) {
				that.handle.onReceive(channel, data);
			} else {
				if(that.processHeader(channel, data)) {
					channel.setChannelStorage(TRANSPORT_MODE_KEY, new Boolean(true));
				}
//				//帧中的静荷数据(payload data)长度小于0x7E的为小帧, 静荷数据长度 >=0x7E又<=0x10000的为中帧, 大帧8字节
//				if((conn.flag & FLAG_TRANSPORT_START) == 0) { //数据包的第1帧
//					conn.flag |= FLAG_TRANSPORT_START; //数据包已开始
//					data.getByteBuffer().order(ByteOrder.BIG_ENDIAN);
//					@SuppressWarnings("unused")
//					byte b1 = data.readByte(); //FIN, RSV, RSV, RSV, opcode(4)
//					byte b2 = data.readByte(); //MASK, Payload len(7)
//					conn.dataLength = b2 & 0x7F;
//					
//					if(conn.dataLength == 126) {
//						conn.dataLength = data.readUInt16();
//					} else if(conn.dataLength == 127) {
//						conn.dataLength = data.readUInt64();
//					}
//					if((b2 & 0x80) != 0) {
//						conn.mask = new byte[4];
//						data.readBytesTo(conn.mask);
//					}
//					if(conn.dataLength > MAX_DATA_LENGTH) {
//						throw new CheckException(121, "data length error. " + conn.dataLength);
//					}
//					int length = (int)conn.dataLength;
//					conn.receiveBuff = new ByteArray(Math.max(length, 8192), length);
//				}
//				//读取数据包中的内容
//				int maxRead = (int)conn.dataLength - conn.receiveBuff.position();
//				int i;
//				for(i=0; i<maxRead && data.hasRemaining(); i++) {
//					byte b = data.readByte();
//					if(conn.mask != null) {
//						b ^= conn.mask[conn.receiveBuff.position() % 4];
//					}
//					conn.receiveBuff.writeByte(b);
//				}
//				if(i == maxRead) { //数据读完了
//					that.onReceive(conn.sid, conn.receiveBuff);
//					if(data.hasRemaining()) {
//						//TODO: 帧中未处理数据
//					}
//					conn.receiveBuff = null;
//					conn.flag &= ~ FLAG_TRANSPORT_START; //数据包已结束，可重新开始
//				}
			}
		}
		
	}
	
//	private static class WebSocketConnection {
//		String sid;
//		NetChannel channel;
//		int flag = 0;
//		ByteArray buff = new ByteArray(4096); //head 最大4K
//		long dataLength = 0;
//		byte[] mask = null; //null表示没有MASK
//		ByteArray receiveBuff = null;
//		
//		private WebSocketConnection(String sid, NetChannel channel) {
//			this.sid = sid;
//			this.channel = channel;
//		}
//	}
}
