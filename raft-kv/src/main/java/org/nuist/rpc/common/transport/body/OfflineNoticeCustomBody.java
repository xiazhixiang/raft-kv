package org.nuist.rpc.common.transport.body;

import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;

/**
 * @description 下线通知的时候发送给consumer的主体
 */
public class OfflineNoticeCustomBody implements CommonCustomBody {
	
	private int port;
	
	private String host;

	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
	

}
