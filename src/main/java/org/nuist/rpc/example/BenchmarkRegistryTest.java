package org.nuist.rpc.example;

import org.nuist.rpc.common.rpc.ServiceReviewState;
import org.nuist.rpc.registry.DefaultRegistryServer;
import org.nuist.rpc.registry.RegistryServerConfig;
import org.nuist.rpc.remoting.netty.NettyServerConfig;

/**
 * @description 性能测试的注册中心端
 */
public class BenchmarkRegistryTest {
	
	
	public static void main(String[] args) {
        
		NettyServerConfig config = new NettyServerConfig();
		RegistryServerConfig registryServerConfig = new RegistryServerConfig();
		registryServerConfig.setDefaultReviewState(ServiceReviewState.PASS_REVIEW);
		//注册中心的端口号
		config.setListenPort(18010);
		new DefaultRegistryServer(config,registryServerConfig).start();
		
	}

}
