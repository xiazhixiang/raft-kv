package org.nuist.rpc.remoting.netty.idle;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

import org.nuist.rpc.common.utils.SystemClock;

/**
 * 基于{@link HashedWheelTimer}的空闲链路监测.
 *
 * 相比较Netty4.x的默认链路监测方式:
 *
 * Netty4.x默认的链路检测使用的是eventLoop的delayQueue, delayQueue是一个优先级队列, 复杂度为O(log n),
 * 每个worker处理自己的链路监测, 可能有助于减少上下文切换, 但是网络IO操作与idle会相互影响.
 *
 * 这个实现使用{@link HashedWheelTimer}的复杂度为O(1), 而且网络IO操作与idle不会相互影响, 但是有上下文切换.
 *
 * 如果连接数小, 比如几万以内, 可以直接用Netty4.x默认的链路检测{@link io.netty.handler.timeout.IdleStateHandler},
 * 如果连接数较大, 建议使用这个实现.
 *
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
public class IdleStateChecker extends ChannelDuplexHandler {

    private static final long MIN_TIMEOUT_MILLIS = 1;

    // do not create a new ChannelFutureListener per write operation to reduce GC pressure.
    private final ChannelFutureListener writeListener = new ChannelFutureListener() {

        public void operationComplete(ChannelFuture future) throws Exception {
            firstWriterIdleEvent = firstAllIdleEvent = true;
            lastWriteTime = SystemClock.millisClock().now(); // make hb for firstWriterIdleEvent and firstAllIdleEvent
        }
    };

    private final HashedWheelTimer timer;

    private final long readerIdleTimeMillis;
    private final long writerIdleTimeMillis;
    private final long allIdleTimeMillis;

    private volatile int state; // 0 - none, 1 - initialized, 2 - destroyed
    private volatile boolean reading;

    private volatile Timeout readerIdleTimeout;
    private volatile long lastReadTime;
    private boolean firstReaderIdleEvent = true;

    private volatile Timeout writerIdleTimeout;
    private volatile long lastWriteTime;
    private boolean firstWriterIdleEvent = true;

    private volatile Timeout allIdleTimeout;
    private boolean firstAllIdleEvent = true;

    public IdleStateChecker(
            HashedWheelTimer timer,
            int readerIdleTimeSeconds,
            int writerIdleTimeSeconds,
            int allIdleTimeSeconds) {

        this(timer, readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, SECONDS);
    }

    public IdleStateChecker(
            HashedWheelTimer timer,
            long readerIdleTime,
            long writerIdleTime,
            long allIdleTime,
            TimeUnit unit) {

        if (unit == null) {
            throw new NullPointerException("unit");
        }

        this.timer = timer;

        if (readerIdleTime <= 0) {
            readerIdleTimeMillis = 0;
        } else {
            readerIdleTimeMillis = Math.max(unit.toMillis(readerIdleTime), MIN_TIMEOUT_MILLIS);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeMillis = 0;
        } else {
            writerIdleTimeMillis = Math.max(unit.toMillis(writerIdleTime), MIN_TIMEOUT_MILLIS);
        }
        if (allIdleTime <= 0) {
            allIdleTimeMillis = 0;
        } else {
            allIdleTimeMillis = Math.max(unit.toMillis(allIdleTime), MIN_TIMEOUT_MILLIS);
        }
    }

    /**
     * Return the readerIdleTime that was given when instance this class in milliseconds.
     */
    public long getReaderIdleTimeInMillis() {
        return readerIdleTimeMillis;
    }

    /**
     * Return the writerIdleTime that was given when instance this class in milliseconds.
     */
    public long getWriterIdleTimeInMillis() {
        return writerIdleTimeMillis;
    }

    /**
     * Return the allIdleTime that was given when instance this class in milliseconds.
     */
    public long getAllIdleTimeInMillis() {
        return allIdleTimeMillis;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (readerIdleTimeMillis > 0 || allIdleTimeMillis > 0) {
            firstReaderIdleEvent = firstAllIdleEvent = true;
            reading = true; // make hb for firstReaderIdleEvent and firstAllIdleEvent
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (readerIdleTimeMillis > 0 || allIdleTimeMillis > 0) {
            lastReadTime = SystemClock.millisClock().now(); // make hb for firstReaderIdleEvent and firstAllIdleEvent
            reading = false;
        }
        ctx.fireChannelReadComplete();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (writerIdleTimeMillis > 0 || allIdleTimeMillis > 0) {
            if (promise.isVoid()) {
                firstWriterIdleEvent = firstAllIdleEvent = true;
                lastWriteTime = SystemClock.millisClock().now(); // make hb for firstWriterIdleEvent and firstAllIdleEvent
            } else {
                promise.addListener(writeListener);
            }
        }
        ctx.write(msg, promise);
    }

    private void initialize(ChannelHandlerContext ctx) {
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        switch (state) {
            case 1:
            case 2:
                return;
        }

        state = 1;

        lastReadTime = lastWriteTime = SystemClock.millisClock().now();
        if (readerIdleTimeMillis > 0) {
            readerIdleTimeout = timer.newTimeout(
                    new ReaderIdleTimeoutTask(ctx),
                    readerIdleTimeMillis, MILLISECONDS);
        }
        if (writerIdleTimeMillis > 0) {
            writerIdleTimeout = timer.newTimeout(
                    new WriterIdleTimeoutTask(ctx),
                    writerIdleTimeMillis, MILLISECONDS);
        }
        if (allIdleTimeMillis > 0) {
            allIdleTimeout = timer.newTimeout(
                    new AllIdleTimeoutTask(ctx),
                    allIdleTimeMillis, MILLISECONDS);
        }
    }

    private void destroy() {
        state = 2;

        if (readerIdleTimeout != null) {
            readerIdleTimeout.cancel();
            readerIdleTimeout = null;
        }
        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel();
            writerIdleTimeout = null;
        }
        if (allIdleTimeout != null) {
            allIdleTimeout.cancel();
            allIdleTimeout = null;
        }
    }

    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    private final class ReaderIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.channel().isOpen()) {
                return;
            }

            long lastReadTime = IdleStateChecker.this.lastReadTime;
            long nextDelay = readerIdleTimeMillis;
            if (!reading) {
                nextDelay -= SystemClock.millisClock().now() - lastReadTime;
            }
            if (nextDelay <= 0) {
                // Reader is idle - set a new timeout and notify the callback.
                readerIdleTimeout = timer.newTimeout(this, readerIdleTimeMillis, MILLISECONDS);
                try {
                    IdleStateEvent event;
                    if (firstReaderIdleEvent) {
                        firstReaderIdleEvent = false;
                        event = IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT;
                    } else {
                        event = IdleStateEvent.READER_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                readerIdleTimeout = timer.newTimeout(this, nextDelay, MILLISECONDS);
            }
        }
    }

    private final class WriterIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.channel().isOpen()) {
                return;
            }

            long lastWriteTime = IdleStateChecker.this.lastWriteTime;
            long nextDelay = writerIdleTimeMillis - (SystemClock.millisClock().now() - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = timer.newTimeout(this, writerIdleTimeMillis, MILLISECONDS);
                try {
                    IdleStateEvent event;
                    if (firstWriterIdleEvent) {
                        firstWriterIdleEvent = false;
                        event = IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT;
                    } else {
                        event = IdleStateEvent.WRITER_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = timer.newTimeout(this, nextDelay, MILLISECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        AllIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.channel().isOpen()) {
                return;
            }

            long nextDelay = allIdleTimeMillis;
            if (!reading) {
                long lastIoTime = Math.max(lastReadTime, lastWriteTime);
                nextDelay -= SystemClock.millisClock().now() - lastIoTime;
            }
            if (nextDelay <= 0) {
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeout = timer.newTimeout(this, allIdleTimeMillis, MILLISECONDS);
                try {
                    IdleStateEvent event;
                    if (firstAllIdleEvent) {
                        firstAllIdleEvent = false;
                        event = IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT;
                    } else {
                        event = IdleStateEvent.ALL_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Either read or write occurred before the timeout - set a new
                // timeout with shorter delay.
                allIdleTimeout = timer.newTimeout(this, nextDelay, MILLISECONDS);
            }
        }
    }
}
