package org.nuist.rpc.test.utils;

import org.nuist.rpc.remoting.netty.NettyRemotingServer;
import org.nuist.rpc.remoting.netty.NettyServerConfig;

public class NettyRemotingServerTest {

    public static void main(String[] args) {
        NettyServerConfig conf = new NettyServerConfig();
        conf.setListenPort(18010);
        new NettyRemotingServer(conf).start();
    }
}
