package org.nuist.raft.rpc;

import org.nuist.rpc.remoting.model.RemotingTransporter;

/**
 *
 */
public interface RpcServer {

    void start();

    void stop();

    RemotingTransporter handlerRequest(RemotingTransporter request) throws Exception;

}
