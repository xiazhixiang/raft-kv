package org.nuist.rpc.remoting.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.nuist.rpc.common.protocal.Protocol;

/**
 * 
 *
 * @description
 * @time
 * @
 */
@SuppressWarnings("deprecation")
public class Heartbeats {

    private static final ByteBuf HEARTBEAT_BUF;
    
    static {
        ByteBuf buf = Unpooled.buffer(Protocol.HEAD_LENGTH);
        buf.writeShort(Protocol.MAGIC);
        buf.writeByte(Protocol.HEARTBEAT);
        buf.writeByte(0);
        buf.writeLong(0);
        buf.writeInt(0);
        HEARTBEAT_BUF = Unpooled.unmodifiableBuffer(Unpooled.unreleasableBuffer(buf));
    }

    /**
     * Returns the shared heartbeat content.
     */
    public static ByteBuf heartbeatContent() {
        return HEARTBEAT_BUF.duplicate();
    }
}
