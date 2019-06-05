package net_io.msg;

import net_io.core.ByteArray;

public abstract class MsgReadWrite {
	abstract public void readData(ByteArray data);
	abstract public void writeData(ByteArray data);

}
