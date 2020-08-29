package org.nuist.rpc.common.transport.body;

import java.util.List;

import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.metrics.ServiceMetrics;

public class MetricsCustomBody implements CommonCustomBody {
	
	private List<ServiceMetrics> serviceMetricses;

	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}

	public List<ServiceMetrics> getServiceMetricses() {
		return serviceMetricses;
	}

	public void setServiceMetricses(List<ServiceMetrics> serviceMetricses) {
		this.serviceMetricses = serviceMetricses;
	}
	
	
	

}
