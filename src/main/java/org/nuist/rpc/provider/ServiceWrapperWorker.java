package org.nuist.rpc.provider;

import java.util.List;

import org.nuist.rpc.provider.interceptor.ProviderProxyHandler;
import org.nuist.rpc.provider.model.ServiceWrapper;


/**
 *
 */
public interface ServiceWrapperWorker {
	
	ServiceWrapperWorker provider(Object serviceProvider);
	
	ServiceWrapperWorker provider(ProviderProxyHandler proxyHandler,Object serviceProvider);
	
	List<ServiceWrapper> create();

}
