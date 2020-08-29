package org.nuist.rpc.provider;

import static org.nuist.rpc.common.protocal.Protocol.AUTO_DEGRADE_SERVICE;
import static org.nuist.rpc.common.protocal.Protocol.DEGRADE_SERVICE;
import io.netty.channel.ChannelHandlerContext;

import org.nuist.rpc.remoting.ConnectionUtils;
import org.nuist.rpc.remoting.model.NettyRequestProcessor;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description provider端注册的处理器
 */
public class DefaultProviderRegistryProcessor implements NettyRequestProcessor {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultProviderRegistryProcessor.class);
	
	private DefaultProvider defaultProvider;

	public DefaultProviderRegistryProcessor(DefaultProvider defaultProvider) {
		this.defaultProvider = defaultProvider;
	}

	@Override
	public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter request) throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("receive request, {} {} {}",//
                request.getCode(), //
                ConnectionUtils.parseChannelRemoteAddr(ctx.channel()), //
                request);
        }
		
		switch (request.getCode()) {
		   case DEGRADE_SERVICE:
			    return this.defaultProvider.handlerDegradeServiceRequest(request,ctx.channel(),DEGRADE_SERVICE);
		   case AUTO_DEGRADE_SERVICE:
			    return this.defaultProvider.handlerDegradeServiceRequest(request,ctx.channel(),AUTO_DEGRADE_SERVICE);
		}
		return null;
	}

}
