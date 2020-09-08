package org.nuist.rpc.common.transport.body;

import java.util.List;

import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.rpc.MetricsReporter;

/**
 * @description 管理员发送给监控中心的信息
 */
public class ProviderMetricsCustomBody implements CommonCustomBody {
	
	
	private List<MetricsReporter> metricsReporter;

	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}

	public List<MetricsReporter> getMetricsReporter() {
		return metricsReporter;
	}

	public void setMetricsReporter(List<MetricsReporter> metricsReporter) {
		this.metricsReporter = metricsReporter;
	}
	

}
