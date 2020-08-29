package org.nuist.raft.rpc.myRpc;

import io.netty.channel.ChannelHandlerContext;
import org.nuist.rpc.remoting.model.NettyRequestProcessor;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RaftUserProcessor implements NettyRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RaftUserProcessor.class);

    @Override
    public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter transporter) throws Exception {

        throw new Exception();
    }

//    public RemotingTransporter handlerRequest(RemotingTransporter transporter) {
//
//        MyRequest request = (serializerImpl().readObject(transporter.bytes(), MyRequest.class));
//        System.out.println(request);
//
//        transporter.setTransporterType(Protocol.RESPONSE_REMOTING);
//
//        if (request.getCmd() == Request.R_VOTE) {
//            logger.info("R_VOTE");
//            transporter.setCustomHeader(new MyResponse("R_VOTE"));
//            return transporter;
//        } else if (request.getCmd() == Request.A_ENTRIES) {
//            transporter.setCustomHeader(new MyResponse("A_ENTRIES"));
//            return transporter;
//        } else if (request.getCmd() == Request.CLIENT_REQ) {
//            transporter.setCustomHeader(new MyResponse("CLIENT_REQ"));
//            return transporter;
//        } else if (request.getCmd() == Request.CHANGE_CONFIG_REMOVE) {
//            transporter.setCustomHeader(new MyResponse("CHANGE_CONFIG_REMOVE"));
//            return transporter;
//        } else if (request.getCmd() == Request.CHANGE_CONFIG_ADD) {
//            transporter.setCustomHeader(new MyResponse("CHANGE_CONFIG_ADD"));
//            return transporter;
//        }
//        return null;
//    }
}
