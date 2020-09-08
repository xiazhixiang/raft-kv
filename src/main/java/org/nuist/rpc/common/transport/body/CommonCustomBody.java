package org.nuist.rpc.common.transport.body;

import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;



/**
 * @description 网络传输对象的主体对象
 */
public interface CommonCustomBody {
	
    void checkFields() throws RemotingCommmonCustomException;
}
