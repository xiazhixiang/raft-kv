package org.nuist.rpc.common.loadbalance;

/**
 * @description 负载均衡的访问策略
 */
public enum LoadBalanceStrategy {
	
	RANDOM, //随机
	WEIGHTINGRANDOM, //加权随机
	ROUNDROBIN, //轮询

}
