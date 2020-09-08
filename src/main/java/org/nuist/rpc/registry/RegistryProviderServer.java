package org.nuist.rpc.registry;

import io.netty.channel.Channel;

import org.nuist.rpc.common.exception.remoting.RemotingSendRequestException;
import org.nuist.rpc.common.exception.remoting.RemotingTimeoutException;
import org.nuist.rpc.remoting.model.RemotingTransporter;

/**
 * @description 注册中心处理provider的服务接口
 */
public interface RegistryProviderServer {
	
	
	/**
	 * 处理provider发送过来的注册信息
	 * @param remotingTransporter 里面的CommonCustomBody 是#PublishServiceCustomBody
	 * @param channel
	 * @return
	 * @throws InterruptedException 
	 * @throws RemotingTimeoutException
	 * @throws RemotingSendRequestException
	 */
	RemotingTransporter handlerRegister(RemotingTransporter remotingTransporter, Channel channel) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException;
}
