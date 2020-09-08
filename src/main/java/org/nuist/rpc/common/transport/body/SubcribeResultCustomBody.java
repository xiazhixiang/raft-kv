package org.nuist.rpc.common.transport.body;

import java.util.ArrayList;
import java.util.List;

import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.loadbalance.LoadBalanceStrategy;
import org.nuist.rpc.common.rpc.RegisterMeta;

/**

 * @description 注册中心向consumer反馈的服务信息
 * 修改成List 这样方便订阅的时候，一起将所有的服务信息发送出去
 */
public class SubcribeResultCustomBody implements CommonCustomBody {
	
	private String serviceName;
	
	private LoadBalanceStrategy loadBalanceStrategy;
	
	private List<RegisterMeta> registerMeta = new ArrayList<RegisterMeta>();
	
	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}

	public List<RegisterMeta> getRegisterMeta() {
		return registerMeta;
	}

	public void setRegisterMeta(List<RegisterMeta> registerMeta) {
		this.registerMeta = registerMeta;
	}

	public LoadBalanceStrategy getLoadBalanceStrategy() {
		return loadBalanceStrategy;
	}

	public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
		this.loadBalanceStrategy = loadBalanceStrategy;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
