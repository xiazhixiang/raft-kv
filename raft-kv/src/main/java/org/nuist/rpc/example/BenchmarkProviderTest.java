package org.nuist.rpc.example;


import org.nuist.rpc.common.exception.remoting.RemotingException;
import org.nuist.rpc.example.demo.service.HelloServiceBenchmark;
import org.nuist.rpc.provider.DefaultProvider;
import org.nuist.rpc.remoting.netty.NettyClientConfig;
import org.nuist.rpc.remoting.netty.NettyServerConfig;

/**
 * @description 性能测试的provider端
 */
public class BenchmarkProviderTest {

	public static void main(String[] args) throws InterruptedException, RemotingException {

		DefaultProvider defaultProvider = new DefaultProvider(new NettyClientConfig(), new NettyServerConfig());

		defaultProvider.registryAddress("127.0.0.1:18010") // 注册中心的地址
				.serviceListenPort(8899) // 暴露服务的地址
				.publishService(new HelloServiceBenchmark()) // 暴露的服务
				.start(); // 启动服务

	}

}
