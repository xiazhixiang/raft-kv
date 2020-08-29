package org.nuist.rpc.remoting.model;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRpcRequestProcessor {
	
	void processRPCRequest(ChannelHandlerContext ctx, RemotingTransporter request);

}
