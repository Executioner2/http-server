package com.ranni.connector.coyote;

import com.ranni.util.net.AbstractEndpoint;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.SocketEvent;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/11 16:39
 * @Ref org.apache.coyote.Processor
 */
public interface Processor {
    /**
     * Process a connection. This is called whenever an event occurs (e.g. more
     * data arrives) that allows processing to continue for a connection that is
     * not currently being processed.
     *
     * @param socketWrapper The connection to process
     * @param status The status of the connection that triggered this additional
     *               processing
     *
     * @return The state the caller should put the socket in when this method
     *         returns
     *
     * @throws IOException If an I/O error occurs during the processing of the
     *         request
     */
    AbstractEndpoint.Handler.SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent status) throws IOException;


    /**
     * @return {@code true} if the Processor is currently processing an upgrade
     *         request, otherwise {@code false}
     */
    boolean isUpgrade();
    boolean isAsync();

    /**
     * Check this processor to see if the timeout has expired and process a
     * timeout if that is that case.
     * <p>
     * Note: The name of this method originated with the Servlet 3.0
     * asynchronous processing but evolved over time to represent a timeout that
     * is triggered independently of the socket read/write timeouts.
     *
     * @param now The time (as returned by {@link System#currentTimeMillis()} to
     *            use as the current time to determine whether the timeout has
     *            expired. If negative, the timeout will always be treated as ifq
     *            it has expired.
     */
    void timeoutAsync(long now);

    /**
     * @return The request associated with this processor.
     */
    Request getRequest();

    /**
     * Recycle the processor, ready for the next request which may be on the
     * same connection or a different connection.
     */
    void recycle();
    

    /**
     * Allows retrieving additional input during the upgrade process.
     *
     * @return leftover bytes
     *
     * @throws IllegalStateException if this is called on a Processor that does
     *         not support upgrading
     */
    ByteBuffer getLeftoverInput();

    /**
     * Informs the processor that the underlying I/O layer has stopped accepting
     * new connections. This is primarily intended to enable processors that
     * use multiplexed connections to prevent further 'streams' being added to
     * an existing multiplexed connection.
     */
    void pause();

    /**
     * Check to see if the async generation (each cycle of async increments the
     * generation of the AsyncStateMachine) is the same as the generation when
     * the most recent async timeout was triggered. This is intended to be used
     * to avoid unnecessary processing.
     *
     * @return {@code true} If the async generation has not changed since the
     *         async timeout was triggered
     */
    boolean checkAsyncTimeoutGeneration();


    /**
     * 调度分派，处理非标准HTTP通信流程。<br>
     * 如Servlet 3.0的异步容器处理
     * 
     * @param event 调度事件
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    SocketState dispatch(SocketEvent event) throws IOException;


    /**
     * 处理请求的方法，会调用适配器然后由适配器交付给对应的容器
     *
     * @param socketWrapper 要处理的socket包装实例
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException;

    
    /**
     * 异步处理
     * 
     * @return 返回异步处理后的socket状态
     */
    SocketState asyncPostProcess();
}
