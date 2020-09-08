package org.nuist.rpc.example.demo.service;

import org.nuist.rpc.client.annotation.RPConsumer;

/**
 *
 */
public interface HelloService {

	@RPConsumer(serviceName="nuist.TEST.SAYHELLO")
	String sayHello(String str);

}
