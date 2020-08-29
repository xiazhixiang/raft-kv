package org.nuist.rpc.example.netty;

import io.netty.channel.ChannelHandlerContext;
import org.nuist.rpc.common.protocal.Protocol;
import org.nuist.rpc.remoting.model.NettyRequestProcessor;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.nuist.rpc.remoting.netty.NettyRemotingServer;
import org.nuist.rpc.remoting.netty.NettyServerConfig;

import java.util.concurrent.Executors;

import static org.nuist.rpc.common.serialization.SerializerHolder.serializerImpl;


public class NettyServerTest {
	
	public static final byte TEST = -1;
	
	public static void main(String[] args) {
		
		NettyServerConfig config = new NettyServerConfig();
		config.setListenPort(18001);
		NettyRemotingServer server = new NettyRemotingServer(config);
		server.registerProecessor(TEST, new NettyRequestProcessor() {
			
			@Override
			public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter transporter) throws Exception {
				transporter.setCustomHeader(serializerImpl().readObject(transporter.bytes(), TestCommonCustomBody.class));
				System.out.println(transporter);
				transporter.setTransporterType(Protocol.RESPONSE_REMOTING);
				return transporter;
			}
		}, Executors.newCachedThreadPool());
		server.start();
	}

}
