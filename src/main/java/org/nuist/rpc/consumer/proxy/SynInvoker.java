package org.nuist.rpc.consumer.proxy;


import io.netty.channel.Channel;

import java.lang.reflect.Method;
import java.util.Map;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import org.nuist.rpc.client.annotation.RPConsumer;
import org.nuist.rpc.common.exception.remoting.RemotingSendRequestException;
import org.nuist.rpc.common.exception.remoting.RemotingTimeoutException;
import org.nuist.rpc.common.exception.rpc.NoServiceException;
import org.nuist.rpc.common.loadbalance.LoadBalanceStrategy;
import org.nuist.rpc.common.protocal.Protocol;
import org.nuist.rpc.common.transport.body.RequestCustomBody;
import org.nuist.rpc.common.transport.body.ResponseCustomBody;
import org.nuist.rpc.common.utils.ChannelGroup;
import org.nuist.rpc.common.utils.SystemClock;
import org.nuist.rpc.consumer.Consumer;
import org.nuist.rpc.remoting.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nuist.rpc.common.serialization.SerializerHolder.serializerImpl;

/**
 *
 *
 * @description 同步调用的类
 * @time 8月27日
 * @
 */
public class SynInvoker {

    private static final Logger logger = LoggerFactory.getLogger(SynInvoker.class);

    private Consumer consumer;
    private long timeoutMillis;
    private Map<String, Long> methodsSpecialTimeoutMillis;
    private LoadBalanceStrategy balanceStrategy;


    public SynInvoker(Consumer consumer, long timeoutMillis, Map<String, Long> methodsSpecialTimeoutMillis, LoadBalanceStrategy balanceStrategy) {
        this.consumer = consumer;
        this.timeoutMillis = timeoutMillis;
        this.methodsSpecialTimeoutMillis = methodsSpecialTimeoutMillis;
        this.balanceStrategy = balanceStrategy;
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args)  {
        logger.info("执行到这");
        RPConsumer rpcConsumer = method.getAnnotation(RPConsumer.class);

        String serviceName = rpcConsumer.serviceName();
        LoadBalanceStrategy _balanceStrategy = balanceStrategy;
        ChannelGroup channelGroup = consumer.loadBalance(serviceName,_balanceStrategy);

        if (channelGroup == null || channelGroup.size() == 0) {
            //如果有channelGroup但是channel中却没有active的Channel的有可能是用户通过直连的方式去调用，我们需要去根据远程的地址去初始化channel
            if(channelGroup != null && channelGroup.getAddress() != null){

                logger.warn("direct connect provider");
                Channel channel = null;
                try {
                    channel = consumer.directGetProviderByChannel(channelGroup.getAddress());
                    channelGroup.add(channel);

                } catch (InterruptedException e) {
                    logger.warn("direction get channel occor exception [{}]",e.getMessage());
                }
            }else{
                throw new NoServiceException("没有第三方提供该服务，请检查服务名");
            }
        }

        RequestCustomBody body = new RequestCustomBody();
        body.setArgs(args);                                   //调用参数
        body.setServiceName(serviceName);                     //调用的服务名
        body.setTimestamp(SystemClock.millisClock().now());   //调用的时间

        Long time = null;
        if(methodsSpecialTimeoutMillis != null){

            Long methodTime = methodsSpecialTimeoutMillis.get(serviceName);
            if(null != methodTime){
                time = methodTime;
            }
        }else{
            time = timeoutMillis == 0l ? 3000l :timeoutMillis;
        }


        RemotingTransporter request = RemotingTransporter.createRequestTransporter(Protocol.RPC_REQUEST, body);
        RemotingTransporter response;
        try {

            response = consumer.sendRpcRequestToProvider(channelGroup.next(),request,time);
            ResponseCustomBody customBody = serializerImpl().readObject(response.bytes(), ResponseCustomBody.class);
            return customBody.getResult();

        } catch (RemotingTimeoutException e) {
            logger.warn("call remoting timeout [{}]",e.getMessage());
            return null;
        } catch (RemotingSendRequestException e) {
            logger.warn("send request orror exception [{}]",e.getMessage());
            return null;
        } catch (InterruptedException e) {
            logger.error("interrupted exception [{}]",e.getMessage());
            return null;
        }
    }

}

