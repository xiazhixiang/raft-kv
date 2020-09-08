package org.nuist.raft.rpc;

import org.nuist.raft.rpc.myRpc.Request;
import org.nuist.raft.rpc.myRpc.Response;

/**
 *
 *
 */
public interface RpcClient {

    Response send(Request request);

    Response send(Request request, int timeout);
}
