package org.nuist.rpc.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.nuist.rpc.consumer.Consumer;
import org.nuist.rpc.consumer.ConsumerClient;
import org.nuist.rpc.consumer.ConsumerConfig;
import org.nuist.rpc.consumer.proxy.ProxyFactory;
import org.nuist.rpc.example.demo.service.HelloService;
import org.nuist.rpc.remoting.netty.NettyClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 [main] WARN org.nuist.example.BenchmarkClient - count=12800000
 [main] WARN org.nuist.example.BenchmarkClient - Request count: 12800000, time: 378 second, qps: 33862
 */
public class BenchmarkClient {

	private static final Logger logger = LoggerFactory.getLogger(BenchmarkClient.class);

	public static void main(String[] args) throws Exception {

		int processors = Runtime.getRuntime().availableProcessors();

		NettyClientConfig registryNettyClientConfig = new NettyClientConfig();
		registryNettyClientConfig.setDefaultAddress("127.0.0.1:18010");

		NettyClientConfig provideClientConfig = new NettyClientConfig();

		ConsumerClient client = new ConsumerClient(registryNettyClientConfig, provideClientConfig, new ConsumerConfig());

		client.start();

		Consumer.SubscribeManager subscribeManager = client.subscribeService("nuist.TEST.SAYHELLO");

		if (!subscribeManager.waitForAvailable(3000l)) {
			throw new Exception("no service provider");
		}

		final HelloService helloService = ProxyFactory.factory(HelloService.class).consumer(client).timeoutMillis(3000l).newProxyInstance();

		for (int i = 0; i < 5000; i++) {
			String str = helloService.sayHello("Lyncc");
			System.out.println(str);
		}
		final int t = 50000;
		final int step = 5;
		long start = System.currentTimeMillis();
		final CountDownLatch latch = new CountDownLatch(processors << step);
		final AtomicLong count = new AtomicLong();
		for (int i = 0; i < (processors << step); i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < t; i++) {
						try {
							helloService.sayHello("Lyncc");

							if (count.getAndIncrement() % 10000 == 0) {
								logger.warn("count=" + count.get());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					latch.countDown();
				}
			}).start();
		}
		try {
			latch.await();
			logger.warn("count=" + count.get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long second = (System.currentTimeMillis() - start) / 1000;
		logger.warn("Request count: " + count.get() + ", time: " + second + " second, qps: " + count.get() / second);

	}

}
