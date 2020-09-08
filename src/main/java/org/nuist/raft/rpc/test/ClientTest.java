package org.nuist.raft.rpc.test;

import org.nuist.raft.rpc.myRpc.DefaultRpcClient;
import org.nuist.raft.rpc.myRpc.Request;

public class ClientTest {

    public static void main(String[] args) {
        DefaultRpcClient client = new DefaultRpcClient();
        Request request = Request.newBuilder()
                .cmd(Request.A_ENTRIES).obj(new Object()).url("127.0.0.1:18001").build();
        System.out.println(client.send(request));
    }
}
