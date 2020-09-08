package org.nuist.rpc.remoting.watcher;

import io.netty.channel.ChannelHandler;

/**
 * 
 *
 *
 * @time
 */
public interface ChannelHandlerHolder {

	ChannelHandler[] handlers();
	
}
