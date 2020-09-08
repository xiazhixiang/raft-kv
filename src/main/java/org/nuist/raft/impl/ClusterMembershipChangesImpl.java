package org.nuist.raft.impl;

import org.nuist.raft.rpc.myRpc.Request;
import org.nuist.raft.rpc.myRpc.Response;
import org.nuist.raft.common.NodeStatus;
import org.nuist.raft.common.Peer;
import org.nuist.raft.entity.LogEntry;
import org.nuist.raft.membership.changes.ClusterMembershipChanges;
import org.nuist.raft.membership.changes.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * 集群配置变更接口默认实现.
 *
 *
 */
public class ClusterMembershipChangesImpl implements ClusterMembershipChanges {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMembershipChangesImpl.class);


    private final DefaultNode node;

    public ClusterMembershipChangesImpl(DefaultNode node) {
        this.node = node;
    }

    /** 必须是同步的,一次只能添加一个节点
     * @param newPeer*/
    @Override
    public synchronized Result addPeer(Peer newPeer) {
        // 已经存在
        if (node.peerSet.getPeersWithOutSelf().contains(newPeer)) {
            return new Result();
        }

        node.peerSet.getPeersWithOutSelf().add(newPeer);

        if (node.status == NodeStatus.LEADER) {
            node.nextIndexs.put(newPeer, 0L);
            node.matchIndexs.put(newPeer, 0L);

            for (long i = 0; i < node.logModule.getLastIndex(); i++) {
                LogEntry e = node.logModule.read(i);
                if (e != null) {
                    node.replication(newPeer, e);
                }
            }

            for (Peer item : node.peerSet.getPeersWithOutSelf()) {
                // TODO 同步到其他节点.
                Request request = Request.newBuilder()
                    .cmd(Request.CHANGE_CONFIG_ADD)
                    .url(newPeer.getAddr())
                    .obj(newPeer)
                    .build();

                Response response = node.rpcClient.send(request);
                Result result = (Result) response.getResult();
                if (result != null && result.getStatus() == Result.Status.SUCCESS.getCode()) {
                    LOGGER.info("replication config success, peer : {}, newServer : {}", newPeer, newPeer);
                } else {
                    LOGGER.warn("replication config fail, peer : {}, newServer : {}", newPeer, newPeer);
                }
            }

        }

        return new Result();
    }


    /** 必须是同步的,一次只能删除一个节点
     * 不需要发送？？？？？
     * @param oldPeer*/
    @Override
    public synchronized Result removePeer(Peer oldPeer) {
        node.peerSet.getPeersWithOutSelf().remove(oldPeer);
        node.nextIndexs.remove(oldPeer);
        node.matchIndexs.remove(oldPeer);

        return new Result();
    }
}
