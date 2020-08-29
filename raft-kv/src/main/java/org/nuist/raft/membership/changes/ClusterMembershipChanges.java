package org.nuist.raft.membership.changes;

import org.nuist.raft.common.Peer;

/**
 *
 * 集群配置变更接口.
 *
 *
 */
public interface ClusterMembershipChanges {

    /**
     * 添加节点.
     * @param newPeer
     * @return
     */
    Result addPeer(Peer newPeer);

    /**
     * 删除节点.
     * @param oldPeer
     * @return
     */
    Result removePeer(Peer oldPeer);
}

