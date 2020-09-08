//package org.nuist.raft.rpc;
//
//import com.alipay.remoting.exception.RemotingException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import org.nuist.raft.exception.RaftRemotingException;
//
///**
// *
// *
// */
//public class DefaultRpcClient implements RpcClient {
//
//    public static Logger logger = LoggerFactory.getLogger(DefaultRpcClient.class.getName());
//
//    private final static com.alipay.remoting.rpc.RpcClient CLIENT = new com.alipay.remoting.rpc.RpcClient();
//    static {
//        CLIENT.init();
//    }
//
//
//    @Override
//    public Response send(Request request) {
//        return send(request, 200000);
//    }
//
//    @Override
//    public Response send(Request request, int timeout) {
//        Response result = null;
//        try {
//            result = (Response) CLIENT.invokeSync(request.getUrl(), request, timeout);
//        } catch (RemotingException e) {
//            //e.printStackTrace();
//            logger.info("rpc RaftRemotingException ");
//            throw new RaftRemotingException();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return (result);
//    }
//}
