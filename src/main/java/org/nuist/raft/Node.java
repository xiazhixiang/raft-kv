package org.nuist.raft;

import org.nuist.raft.common.NodeConfig;
import org.nuist.raft.entity.AentryParam;
import org.nuist.raft.entity.AentryResult;
import org.nuist.raft.entity.RvoteParam;
import org.nuist.raft.entity.RvoteResult;
import org.nuist.raft.client.ClientKVAck;
import org.nuist.raft.client.ClientKVReq;

/**
 *
 *
 */
public interface Node<T> extends LifeCycle{

    /**
     * 设置配置文件.
     *
     * @param config
     */
    void setConfig(NodeConfig config);

    /**
     * 处理请求投票 RPC.
     *
     * @param param
     * @return
     */
    RvoteResult handlerRequestVote(RvoteParam param);

    /**
     * 处理附加日志请求.
     *
     * @param param
     * @return
     */
    AentryResult handlerAppendEntries(AentryParam param) throws Exception;

    /**
     * 处理客户端请求.
     *
     * @param request
     * @return
     */
    ClientKVAck handlerClientRequest(ClientKVReq request) throws Exception;

    /**
     * 转发给 leader 节点.
     * @param request
     * @return
     */
    ClientKVAck redirect(ClientKVReq request);

}
