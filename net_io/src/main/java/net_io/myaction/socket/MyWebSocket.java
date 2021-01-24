package net_io.myaction.socket;

import net_io.core.ByteArray;
import net_io.core.WebSocketChannel;
import net_io.core.WebSocketProtocol;
import net_io.utils.EncodeUtils;
import net_io.utils.thread.MyThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

public class MyWebSocket extends WebSocketProtocol {
	private MyThreadPool producerPool;

	public MyWebSocket(MyThreadPool producerPool) {
		// 构造一个线程池
		this.producerPool = producerPool;
	}

	public MyWebSocket(MyThreadPool producerPool, boolean secure) {
		super(secure);
		// 构造一个线程池
		this.producerPool = producerPool;
	}

	protected void sendMsg(String sid, String data) throws IOException {
		byte[] bts = data.getBytes(EncodeUtils.Charsets.UTF_8);
		sendMsg(sid, bts);
	}

	protected void sendMsg(String sid, byte[] bts) throws IOException {
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
		super.sendMsg(sid, buff.getByteBuffer());
	}

	@Override
	protected boolean onAccept(InetSocketAddress address) {
		return true;
	}

	@Override
	protected boolean onCreateInbound(WebSocketChannel channel, String head) {
		return true;
	}

	@Override
	protected void onReceive(WebSocketChannel channel, ByteArray data) throws Exception {
		String buff = new String(data.readBytesBack());
		System.out.println("buff: "+buff);
		producerPool.execute(new SocketActionProcessor(this, channel, buff));

		//sendMsg(sid, "Welcome!!! "+DateUtils.getDateTime() + " - " + sid + " - " + new String(data.readBytesBack()));
	}

	@Override
	protected void onClose(WebSocketChannel channel) {

	}
}
