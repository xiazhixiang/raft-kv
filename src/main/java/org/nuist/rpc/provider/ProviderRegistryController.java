package org.nuist.rpc.provider;

import java.util.List;

import org.nuist.rpc.client.metrics.ServiceMeterManager;
import org.nuist.rpc.provider.flow.control.ServiceFlowControllerManager;
import org.nuist.rpc.provider.model.ServiceWrapper;
import org.nuist.rpc.common.utils.Pair;


/**
 * @description provider端的控制器
 */
public class ProviderRegistryController {
	
	private DefaultProvider defaultProvider;
	
	//provider与注册中心的所有逻辑控制器
	private RegistryController registryController;
	//provider与monitor端通信的控制器
	private ProviderMonitorController providerMonitorController;
	//本地服务编织服务管理
	private LocalServerWrapperManager localServerWrapperManager;
	private final ServiceProviderContainer providerContainer;
	private ServiceFlowControllerManager serviceFlowControllerManager = new ServiceFlowControllerManager();
	
	public ProviderRegistryController(DefaultProvider defaultProvider) {
		this.defaultProvider = defaultProvider;
		providerContainer = new DefaultServiceProviderContainer();
		localServerWrapperManager = new LocalServerWrapperManager(this);
		registryController = new RegistryController(defaultProvider);
		providerMonitorController = new ProviderMonitorController(defaultProvider);
	}
	
	/**
	 * 检查符合自动降级的服务
	 */
	public void checkAutoDegrade() {

		//获取到所有需要降级的服务名
		List<Pair<String, DefaultServiceProviderContainer.CurrentServiceState>> needDegradeServices = providerContainer.getNeedAutoDegradeService();

		//如果当前实例需要降级的服务列表不为空的情况下，循环每个列表	
		if (!needDegradeServices.isEmpty()) {
			
			for (Pair<String, DefaultServiceProviderContainer.CurrentServiceState> pair : needDegradeServices) {

				//服务名
				String serviceName = pair.getKey();
				//最低成功率
				Integer minSuccessRate = pair.getValue().getMinSuccecssRate();
				//调用的实际成功率
				Integer realSuccessRate = ServiceMeterManager.calcServiceSuccessRate(serviceName);
				
				if (minSuccessRate > realSuccessRate) {
					
					final Pair<DefaultServiceProviderContainer.CurrentServiceState, ServiceWrapper> _pair = this.defaultProvider.getProviderController().getProviderContainer()
							.lookupService(serviceName);
					DefaultServiceProviderContainer.CurrentServiceState currentServiceState = _pair.getKey();
					if (!currentServiceState.getHasDegrade().get()) {
						currentServiceState.getHasDegrade().set(true);
					}
				}
			}
		}
	}

	public DefaultProvider getDefaultProvider() {
		return defaultProvider;
	}

	public void setDefaultProvider(DefaultProvider defaultProvider) {
		this.defaultProvider = defaultProvider;
	}

	public LocalServerWrapperManager getLocalServerWrapperManager() {
		return localServerWrapperManager;
	}

	public void setLocalServerWrapperManager(LocalServerWrapperManager localServerWrapperManager) {
		this.localServerWrapperManager = localServerWrapperManager;
	}

	public RegistryController getRegistryController() {
		return registryController;
	}

	public void setRegistryController(RegistryController registryController) {
		this.registryController = registryController;
	}

	public ProviderMonitorController getProviderMonitorController() {
		return providerMonitorController;
	}

	public void setProviderMonitorController(ProviderMonitorController providerMonitorController) {
		this.providerMonitorController = providerMonitorController;
	}

	public ServiceProviderContainer getProviderContainer() {
		return providerContainer;
	}

	public ServiceFlowControllerManager getServiceFlowControllerManager() {
		return serviceFlowControllerManager;
	}

	public void setServiceFlowControllerManager(ServiceFlowControllerManager serviceFlowControllerManager) {
		this.serviceFlowControllerManager = serviceFlowControllerManager;
	}
	

}
