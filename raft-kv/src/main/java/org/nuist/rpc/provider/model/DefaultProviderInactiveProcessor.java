package org.nuist.rpc.provider.model;

import io.netty.channel.ChannelHandlerContext;

import org.nuist.rpc.provider.DefaultProvider;
import org.nuist.rpc.remoting.model.NettyChannelInactiveProcessor;

/**
 * @description provider的netty inactive触发的事件
 */
public class DefaultProviderInactiveProcessor implements NettyChannelInactiveProcessor {

	private DefaultProvider defaultProvider;

	public DefaultProviderInactiveProcessor(DefaultProvider defaultProvider) {
		this.defaultProvider = defaultProvider;
	}

	@Override
	public void processChannelInactive(ChannelHandlerContext ctx) {
		defaultProvider.setProviderStateIsHealthy(false);
	}

}
