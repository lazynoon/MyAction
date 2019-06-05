package net_io.myaction.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net_io.core.ByteArray;
import net_io.core.NetChannel;
import net_io.core.SocketEventHandle;
import net_io.core.StatNIO;
import net_io.core.StreamSocket;
import net_io.core.ssl.SSLSocketServer;
import net_io.myaction.CheckException;
import net_io.myaction.tool.HttpHeader;
import net_io.utils.EncodeUtils;
import net_io.utils.MixedUtils;
import net_io.utils.thread.MyThreadPool;

public class WebSocket  {
	private _WebSocketHandle handle = new _WebSocketHandle(this);
	private StreamSocket socket;
	private ConcurrentHashMap<String, WebSocketConnection> sidMap = new ConcurrentHashMap<String, WebSocketConnection>();
	private ConcurrentHashMap<NetChannel, WebSocketConnection> channelMap = new ConcurrentHashMap<NetChannel, WebSocketConnection>();
	private MyThreadPool producerPool;
	private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(StandardCharsets.ISO_8859_1);
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
	
	public WebSocket(MyThreadPool producerPool) {
		// 构造一个线程池  
		this.producerPool = producerPool;
		socket = new StreamSocket(handle);
	}

	public WebSocket(MyThreadPool producerPool, boolean secure) {
		// 构造一个线程池  
		this.producerPool = producerPool;
		if(secure) {
			socket = new StreamSocket(handle);
		} else {
			socket = new SSLSocketServer(handle);
		}
	}

	public void bind(int port) throws IOException {
		socket.bind(port);
	}
	
	protected boolean onAccept(InetSocketAddress address) {
		return true;
	}
	
	protected boolean onCreateInbound(String sid, String head) {
		return true;
	}
	
	public void close(String sid) throws IOException {
		WebSocketConnection conn = sidMap.get(sid);
		if(conn == null) {
			throw new IOException("connect id not exists: "+sid);
		}
		conn.channel.close();
	}
	public WebSocketConnection getConn(String sid) {
		return sidMap.get(sid);
	}
	public NetChannel getNetChannel(String sid) {
		WebSocketConnection conn = sidMap.get(sid);
		if(conn == null) {
			return null;
		}
		return conn.channel;
	}
	
	protected void sendMsg(String sid, String data) throws IOException {
		byte[] bts = data.getBytes(StandardCharsets.UTF_8);
		sendMsg(sid, bts);
	}
	protected void sendMsg(String sid, byte[] bts) throws IOException {
		WebSocketConnection conn = sidMap.get(sid);
		if(conn == null) {
			throw new IOException("connect id not exists: "+sid);
		}
		
		ByteArray buff = new ByteArray(bts.length+10);
		buff.getByteBuffer().order(ByteOrder.BIG_ENDIAN);
		int flag1 = 0x80 | OPCODE_TEXT;
		buff.writeByte(flag1);
		if(bts.length > 0xFFFF) {
			buff.writeByte(127);
			buff.writeUInt64(bts.length);
		} else if(bts.length > 125) {
			buff.writeByte(126);
			buff.writeUInt16(bts.length);
		} else {
			buff.writeByte(bts.length);
		}
		buff.writeBytesDirect(bts);
		buff.finishWrite();
		conn.channel.send(buff.getByteBuffer());
	}
	
	protected void onReceive(String sid, ByteArray data) throws Exception {
		String buff = new String(data.readBytesBack());
		System.out.println("buff: "+buff);
		producerPool.execute(new SocketActionProcessor(this, sid, buff));

		//sendMsg(sid, "Welcome!!! "+DateUtils.getDateTime() + " - " + sid + " - " + new String(data.readBytesBack()));
	}
	
	protected void onClose(String sid) {
		Map<String, Long> stat = StatNIO.bossClass.getStat();
		//System.out.println("\r\n------------------------------------\r\n");
		System.out.println("<strong>socket stat</strong>");
		for(String key : StatNIO.bossClass.getFields()) {
			String name = StatNIO.bossClass.getChineseName(key);
			System.out.println(name+": "+stat.get(key));
		}

		stat = StatNIO.packetStat.getStat();
		System.out.println("\r\n----------------  pactket socket stat  --------------------\r\n");
		System.out.println("<strong>packet socket stat</strong>");
		for(String key : StatNIO.packetStat.getFields()) {
			String name = StatNIO.packetStat.getChineseName(key);
			System.out.println(name+": "+stat.get(key));
		}

		stat = StatNIO.streamStat.getStat();
		System.out.println("\r\n----------------  stream socket stat  --------------------\r\n");
		System.out.println("<strong>stream socket stat</strong>");
		for(String key : StatNIO.streamStat.getFields()) {
			String name = StatNIO.streamStat.getChineseName(key);
			System.out.println(name+": "+stat.get(key));
		}
		
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
			String sid = EncodeUtils.createTimeRandId();
			WebSocketConnection conn = new WebSocketConnection(sid, channel);
			sidMap.put(sid, conn);
			channelMap.put(channel, conn);
		}
		public void onClose(NetChannel channel) throws Exception {
			WebSocketConnection conn = channelMap.remove(channel);
			if(conn != null) {
				sidMap.remove(conn.sid);
			}
			that.onClose(conn.sid);
		}
		
		public void onReceive(NetChannel channel, ByteArray data) throws Exception {
			WebSocketConnection conn = channelMap.get(channel);
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
						if(pass && that.onCreateInbound(conn.sid, head)) { //允许连接
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
							conn.channel.send(ByteBuffer.wrap(accept.toString().getBytes(StandardCharsets.UTF_8)));
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
					@SuppressWarnings("unused")
					byte b1 = data.readByte(); //FIN, RSV, RSV, RSV, opcode(4)
					byte b2 = data.readByte(); //MASK, Payload len(7)
					conn.dataLength = b2 & 0x7F;
					
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
						throw new CheckException(121, "data length error. " + conn.dataLength);
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
					that.onReceive(conn.sid, conn.receiveBuff);
					if(data.hasRemaining()) {
						//TODO: 帧中未处理数据
					}
					conn.receiveBuff = null;
					conn.flag &= ~ FLAG_TRANSPORT_START; //数据包已结束，可重新开始
				}
			}
		}
		
	}
	
	private static class WebSocketConnection {
		String sid;
		NetChannel channel;
		int flag = 0;
		ByteArray buff = new ByteArray(4096); //head 最大4K
		long dataLength = 0;
		byte[] mask = null; //null表示没有MASK
		ByteArray receiveBuff = null;
		
		private WebSocketConnection(String sid, NetChannel channel) {
			this.sid = sid;
			this.channel = channel;
		}
	}
}
