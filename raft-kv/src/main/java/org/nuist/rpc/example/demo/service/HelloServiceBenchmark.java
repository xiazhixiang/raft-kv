package org.nuist.rpc.example.demo.service;


import org.nuist.rpc.client.annotation.RPCService;

public class HelloServiceBenchmark implements HelloService {

	@Override
	@RPCService(responsibilityName="xiaoy",
	serviceName="nuist.TEST.SAYHELLO",
	connCount = 4,
	isFlowController = false,
	degradeServiceDesc="默认返回hello")
	public String sayHello(String str) {
		return str;
	}
	

}
