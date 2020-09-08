package org.nuist.rpc.remoting;

import org.nuist.rpc.remoting.model.RemotingResponse;


/**
 * 
 *
 * @description 远程调用之后的回调函数
 * 11:06:40
 * @
 */
public interface InvokeCallback {
	
    void operationComplete(final RemotingResponse remotingResponse);
    
}
