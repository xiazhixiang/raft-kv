package org.nuist.raft.rpc.test;

import org.nuist.raft.impl.DefaultNode;
import org.nuist.raft.rpc.myRpc.DefaultRpcServer;

public class ServerTest {

    public static void main(String[] args) {
        DefaultRpcServer server =  new DefaultRpcServer(18001, new DefaultNode());
        server.start();
    }
}
