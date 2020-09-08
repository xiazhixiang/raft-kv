package org.nuist.raft.rpc.myRpc;

import org.nuist.raft.exception.RaftRemotingException;
import org.nuist.raft.rpc.RpcClient;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.nuist.rpc.remoting.netty.NettyClientConfig;
import org.nuist.rpc.remoting.netty.NettyRemotingClient;
import org.nuist.rpc.example.netty.NettyServerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nuist.rpc.common.serialization.SerializerHolder.serializerImpl;

/**
 * 自己实现的RPC
 */
public class DefaultRpcClient implements RpcClient {

//    public static void main(String[] args) throws Exception {
//
//        //int processors = Runtime.getRuntime().availableProcessors();
//
//        NettyClientConfig nettyClientConfig = new NettyClientConfig();
//        NettyRemotingClient client = new NettyRemotingClient(nettyClientConfig);
//        client.start();
//        //Channel channel = client.directConnect("127.0.0.1:8899");
//        MyRequest request = MyRequest.newBuilder()
//                .cmd(MyRequest.A_ENTRIES).obj(new Object()).url("127.0.0.1").build();
//        System.out.println(request.toString());
//
//        RemotingTransporter remotingTransporter = RemotingTransporter.createRequestTransporter(TEST, request);
//        RemotingTransporter response = client.invokeSync("127.0.0.1:18001", remotingTransporter, 3000);
//        MyResponse myResponse = serializerImpl().readObject(response.bytes(), MyResponse.class);
//        System.out.println(myResponse.getResult());
//
//    }

    public static Logger logger = LoggerFactory.getLogger(DefaultRpcClient.class.getName());

    private static final NettyRemotingClient client;

    static {
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        client = new NettyRemotingClient(nettyClientConfig);
        client.start();
    }



    public Response send(Request request) {
        return send(request, 200000);
    }

    public Response send(Request request, int timeout) {
        Response result = null;
        try {
            RemotingTransporter remotingTransporter = RemotingTransporter.createRequestTransporter(NettyServerTest.TEST, request);
            RemotingTransporter response = client.invokeSync(request.url, remotingTransporter, 3000);
            result = serializerImpl().readObject(response.bytes(), Response.class);
        } catch (Exception e) {
            //e.printStackTrace();
            logger.info("rpc RaftRemotingException ");
            throw new RaftRemotingException();
        }
//        catch (InterruptedException e) {
////            e.printStackTrace();
////        }
        return (result);
    }


}
