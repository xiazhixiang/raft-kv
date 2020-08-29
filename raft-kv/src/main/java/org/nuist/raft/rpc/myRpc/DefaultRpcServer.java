package org.nuist.raft.rpc.myRpc;

import io.netty.channel.ChannelHandlerContext;
import org.nuist.rpc.common.protocal.Protocol;
import org.nuist.raft.client.ClientKVReq;
import org.nuist.raft.common.Peer;
import org.nuist.raft.entity.AentryParam;
import org.nuist.raft.entity.RvoteParam;
import org.nuist.raft.impl.DefaultNode;
import org.nuist.raft.membership.changes.ClusterMembershipChanges;
import org.nuist.raft.rpc.RpcServer;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.nuist.rpc.remoting.netty.NettyRemotingServer;
import org.nuist.rpc.remoting.netty.NettyServerConfig;
import org.nuist.rpc.example.netty.NettyServerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import static org.nuist.rpc.common.serialization.SerializerHolder.serializerImpl;

public class DefaultRpcServer implements RpcServer {

//    public static void main(String[] args) {
//
//        NettyServerConfig config = new NettyServerConfig();
//        config.setListenPort(18001);
//        NettyRemotingServer server = new NettyRemotingServer(config);
//        server.registerProecessor(TEST, new MyProcessor() {
//            @Override
//            public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter transporter) throws Exception {
//                return handlerRequest(transporter);
//            }
//        }, Executors.newCachedThreadPool());
//        server.start();
//    }

    private static Logger logger = LoggerFactory.getLogger(DefaultRpcServer.class);
    private volatile boolean flag;

    private DefaultNode node;

    private NettyRemotingServer server;

    public DefaultRpcServer(int port, DefaultNode node) {

        if (flag) {
            return;
        }
        synchronized (this) {
            if (flag) {
                return;
            }

            NettyServerConfig config = new NettyServerConfig();
            config.setListenPort(port);
            server = new NettyRemotingServer(config);

            server.registerProecessor(NettyServerTest.TEST, new RaftUserProcessor() {
                @Override
                public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter transporter) throws Exception {
                    return handlerRequest(transporter);
                }
            }, Executors.newCachedThreadPool());
            this.node = node;
            flag = true;
        }

    }


    public void start() {
        server.start();
    }

    public void stop() {
        server.shutdown();
    }



    public RemotingTransporter handlerRequest(RemotingTransporter transporter) throws Exception{

        Request request = (serializerImpl().readObject(transporter.bytes(), Request.class));
        System.out.println(request);

        transporter.setTransporterType(Protocol.RESPONSE_REMOTING);

        if (request.getCmd() == Request.R_VOTE) {
            transporter.setCustomHeader(new Response(node.handlerRequestVote((RvoteParam) request.getObj())));
            return transporter;
        } else if (request.getCmd() == Request.A_ENTRIES) {
            transporter.setCustomHeader(new Response(node.handlerAppendEntries((AentryParam) request.getObj())));
            return transporter;
        } else if (request.getCmd() == Request.CLIENT_REQ) {
            transporter.setCustomHeader(new Response(node.handlerClientRequest((ClientKVReq) request.getObj())));
            return transporter;
        } else if (request.getCmd() == Request.CHANGE_CONFIG_REMOVE) {
            transporter.setCustomHeader(new Response(((ClusterMembershipChanges) node).removePeer((Peer) request.getObj())));
            return transporter;
        } else if (request.getCmd() == Request.CHANGE_CONFIG_ADD) {
            transporter.setCustomHeader(new Response(((ClusterMembershipChanges) node).addPeer((Peer) request.getObj())));
            return transporter;
        }
        return null;
    }

//    @Override
//    public Response handlerRequest(Request request) {
//        if (request.getCmd() == Request.R_VOTE) {
//            return new Response(node.handlerRequestVote((RvoteParam) request.getObj()));
//        } else if (request.getCmd() == Request.A_ENTRIES) {
//            return new Response(node.handlerAppendEntries((AentryParam) request.getObj()));
//        } else if (request.getCmd() == Request.CLIENT_REQ) {
//            return new Response(node.handlerClientRequest((ClientKVReq) request.getObj()));
//        } else if (request.getCmd() == Request.CHANGE_CONFIG_REMOVE) {
//            return new Response(((ClusterMembershipChanges) node).removePeer((Peer) request.getObj()));
//        } else if (request.getCmd() == Request.CHANGE_CONFIG_ADD) {
//            return new Response(((ClusterMembershipChanges) node).addPeer((Peer) request.getObj()));
//        }
//        return null;
//    }

}
