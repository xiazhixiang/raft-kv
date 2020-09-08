package org.nuist.rpc.remoting;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuist.rpc.common.exception.remoting.RemotingSendRequestException;
import org.nuist.rpc.common.exception.remoting.RemotingTimeoutException;
import org.nuist.rpc.common.protocal.Protocol;
import org.nuist.rpc.common.utils.Pair;
import org.nuist.rpc.remoting.model.NettyChannelInactiveProcessor;
import org.nuist.rpc.remoting.model.NettyRequestProcessor;
import org.nuist.rpc.remoting.model.RemotingResponse;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 *
 * @description netty C/S 端的客户端提取，子类去完全netty的一些创建的事情，该抽象类则取完成使用子类创建好的channel去与远程端交互
 *  
 * 10:57:27
 * @
 */
public abstract class NettyRemotingBase {
	
	private static final Logger logger = LoggerFactory.getLogger(NettyRemotingBase.class);
	
	/******key为请求的opaque value是远程返回的结果封装类******/
	protected final ConcurrentHashMap<Long, RemotingResponse> responseTable = new ConcurrentHashMap<Long, RemotingResponse>(256);
	
	//如果使用者没有对创建的Netty网络段注入某个特定请求的处理器的时候，默认使用该默认的处理器
	protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;
	
	//netty网络段channelInactive事件发生的处理器
	protected Pair<NettyChannelInactiveProcessor, ExecutorService> defaultChannelInactiveProcessor;
	
	protected final ExecutorService publicExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);


        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
        }
    });
	
	//注入的某个requestCode对应的处理器放入到HashMap中，键值对一一匹配
	protected final HashMap<Byte/* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
            new HashMap<Byte, Pair<NettyRequestProcessor, ExecutorService>>(64);
	
	//远程端的调用具体实现
	public RemotingTransporter invokeSyncImpl(final Channel channel,final RemotingTransporter request,final long timeoutMillis) throws RemotingTimeoutException, RemotingSendRequestException, InterruptedException{
		
		try {
			//构造一个请求的封装体，请求Id和请求结果一一对应
			final RemotingResponse remotingResponse = new RemotingResponse(request.getOpaque(), timeoutMillis, null);
			//将请求放入一个"篮子"中，等远程端填充该篮子中嗷嗷待哺的每一个结果集
			this.responseTable.put(request.getOpaque(), remotingResponse);
			//发送请求
			channel.writeAndFlush(request).addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()){
						//如果发送对象成功，则设置成功
						remotingResponse.setSendRequestOK(true);
						return;
					}else{
						remotingResponse.setSendRequestOK(false);
					}
					//如果请求发送直接失败，则默认将其从responseTable这个篮子中移除
					responseTable.remove(request.getOpaque());
					//失败的异常信息
					remotingResponse.setCause(future.cause());
					//设置当前请求的返回主体返回体是null(请求失败的情况下，返回的结果肯定是null)
					remotingResponse.putResponse(null);
					logger.warn("use channel [{}] send msg [{}] failed and failed reason is [{}]",channel,request,future.cause().getMessage());
				}
			});
			
			RemotingTransporter remotingTransporter = remotingResponse.waitResponse();
			if(null == remotingTransporter){
				//如果发送是成功的，则说明远程端，处理超时了
				if (remotingResponse.isSendRequestOK()) {
					throw new RemotingTimeoutException(ConnectionUtils.parseChannelRemoteAddr(channel),
	                        timeoutMillis, remotingResponse.getCause());
				}else{
					throw new RemotingSendRequestException(ConnectionUtils.parseChannelRemoteAddr(channel),
							remotingResponse.getCause());
				}
			}
			return remotingTransporter;
		}finally {
			//最后不管怎么样，都需要将其从篮子中移除出来，否则篮子会撑爆的
			this.responseTable.remove(request.getOpaque());
		}
	}
	
	//ChannelRead0方法对应的具体实现
	protected void processMessageReceived(ChannelHandlerContext ctx, RemotingTransporter msg) {
		
		if(logger.isDebugEnabled()){
			logger.debug("channel [] received RemotingTransporter is [{}]",ctx.channel(),msg);
		}
		
		final RemotingTransporter remotingTransporter = msg;
		
        if (remotingTransporter != null) {
            switch (remotingTransporter.getTransporterType()) {
            //作为server端 client端的请求的对应的处理
            case Protocol.REQUEST_REMOTING:
                processRemotingRequest(ctx, remotingTransporter);
                break;
            //作为客户端，来自server端的响应的处理
            case Protocol.RESPONSE_REMOTING:
                processRemotingResponse(ctx, remotingTransporter);
                break;
            default:
                break;
            }
        }
	}


	protected void processChannelInactive(final ChannelHandlerContext ctx) {
		final Pair<NettyChannelInactiveProcessor, ExecutorService> pair = this.defaultChannelInactiveProcessor;
		if(pair != null){
			
			 Runnable run = new Runnable(){

				@Override
				public void run() {
					try {
						pair.getKey().processChannelInactive(ctx);
					} catch (RemotingSendRequestException | RemotingTimeoutException | InterruptedException e) {
						logger.error("server occor exception [{}]",e.getMessage());
					}
				}
			 };
			 try {
				 pair.getValue().submit(run);
			} catch (Exception e) {
				logger.error("server is busy,[{}]",e.getMessage());
			}
				 
		}
	}
	
	protected void processRemotingRequest(final ChannelHandlerContext ctx, final RemotingTransporter remotingTransporter) {
		
		final Pair<NettyRequestProcessor, ExecutorService> matchedPair = this.processorTable.get(remotingTransporter.getCode());
		final Pair<NettyRequestProcessor, ExecutorService> pair =
                null == matchedPair ? this.defaultRequestProcessor : matchedPair;
		 if (pair != null) {
			 
			 Runnable run = new Runnable() {

				@Override
				public void run() {
					try {
						RPCHook rpcHook = NettyRemotingBase.this.getRPCHook();
                        if (rpcHook != null) {
                            rpcHook.doBeforeRequest(ConnectionUtils.parseChannelRemoteAddr(ctx.channel()), remotingTransporter);
                        }
                        final RemotingTransporter response = pair.getKey().processRequest(ctx, remotingTransporter);
                        if (rpcHook != null) {
                            rpcHook.doAfterResponse(ConnectionUtils.parseChannelRemoteAddr(ctx.channel()),
                            		remotingTransporter, response);
                        }
                        if(null != response){
                        	ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {

    							@Override
    							public void operationComplete(ChannelFuture future) throws Exception {
    								if(!future.isSuccess()){
    									logger.error("fail send response ,exception is [{}]",future.cause().getMessage());
    								}
    							}
                             });
                        }
					} catch (Exception e) {
						logger.error("processor occur exception [{}]",e.getMessage());
						final RemotingTransporter response = RemotingTransporter.newInstance(remotingTransporter.getOpaque(), Protocol.RESPONSE_REMOTING, Protocol.HANDLER_ERROR, null);
                        ctx.writeAndFlush(response);
					}
				}
			 };
			 try {
				 pair.getValue().submit(run);
			} catch (Exception e) {
				logger.error("server is busy,[{}]",e.getMessage());
				final RemotingTransporter response = RemotingTransporter.newInstance(remotingTransporter.getOpaque(), Protocol.RESPONSE_REMOTING, Protocol.HANDLER_BUSY, null);
                ctx.writeAndFlush(response);
			}
		 }
	}
	
	protected abstract RPCHook getRPCHook();

	/**
	 * client处理server端返回的消息的处理
	 * @param ctx
	 * @param remotingTransporter
	 */
	protected void processRemotingResponse(ChannelHandlerContext ctx, RemotingTransporter remotingTransporter) {
		//从缓存篮子里拿出对应请求的对应响应的载体RemotingResponse
		final RemotingResponse remotingResponse = responseTable.get(remotingTransporter.getOpaque());
		//不超时的情况下
		if(null != remotingResponse){
			//首先先设值，这样会在countdownlatch wait之前把值赋上
			remotingResponse.setRemotingTransporter(remotingTransporter);
			//可以直接countdown
			remotingResponse.putResponse(remotingTransporter);
			//从篮子中移除
			responseTable.remove(remotingTransporter.getOpaque());
		}else {
            logger.warn("received response but matched Id is removed from responseTable maybe timeout");
            logger.warn(remotingTransporter.toString());
        }
	}
	
	
}
