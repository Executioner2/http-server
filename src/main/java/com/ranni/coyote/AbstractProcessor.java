package com.ranni.coyote;

import com.ranni.util.buf.ByteChunk;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.DispatchType;
import com.ranni.util.net.SocketEvent;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 20:07
 * @Ref org.apache.coyote.AbstractProcessor
 */
public abstract class AbstractProcessor implements Processor, ActionHook {

    // ==================================== 属性字段 ====================================
    
    protected final Adapter adapter;
    protected final Request request;
    protected final Response response;
    protected volatile SocketWrapperBase<?> socketWrapper = null;


    // ==================================== 构造方法 ====================================

    public AbstractProcessor(Adapter adapter) {
        this(adapter, new Request(), new Response());
    }
    
    public AbstractProcessor(Adapter adapter, Request request, Response response) {
        this.adapter = adapter;
        this.request = request;
        this.response = response;
        this.request.setHook(this);
        this.response.setHook(this);
        this.request.setResponse(response);
    }


    // ==================================== 响应的抽象方法 ====================================
    
    protected abstract void prepareResponse() throws IOException;
    protected abstract void finishResponse() throws IOException;
    protected abstract void ack(ContinueResponseTiming continueResponseTiming);
    protected abstract void flush() throws IOException;
    protected abstract int available(boolean doRead);
    protected abstract void setRequestBody(ByteChunk body);
    protected abstract void setSwallowResponse();
    protected abstract void disableSwallowRequest();

    
    // ==================================== 抽象方法 ====================================
    
    protected abstract boolean isRequestBodyFullyRead();
    protected abstract void registerReadInterest();
    protected abstract boolean isReadyForWrite();
    

    // ==================================== 核心方法 ====================================
    
    /**
     * 处理socket事件
     * 
     * @param socketWrapper 要处理的socket包装实例
     * @param event 事件
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    @Deprecated // XXX - 实现并不完整，加上@Deprecated是为了提醒调用者
    public SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent event) throws IOException {
        SocketState state = SocketState.CLOSED;
        Iterator<DispatchType> dispatches = null; // 需要进行的调度
        
        do {
            if (dispatches != null) {
                // XXX - 有指定的分派，暂不支持
                DispatchType next = dispatches.next();
                state = dispatch(next.getSocketStatus());
                
                if (!dispatches.hasNext()) {
                    state = checkForPipelinedData(state, socketWrapper);
                }
            } else if (event == SocketEvent.DISCONNECT) {
                // 什么都不做，使其回收
            } else if (isAsync() || isUpgrade() || state == SocketState.ASYNC_END) {
                // 异步或服务升级就需要另外分派此请求
                state = dispatch(event);
                state = checkForPipelinedData(state, socketWrapper);
            } else if (event == SocketEvent.OPEN_READ) {
                // 直接服务这个请求
                service(socketWrapper);
            } else if (event == SocketEvent.CONNECT_FAIL) {
                // XXX - 连接失败，响应400
            } else {
                // 不符合上面的条件，默认关闭此请求
                state = SocketState.CLOSED;
            }
            
            if (isAsync()) {
                state = asyncPostProcess();
            }
            
            if (dispatches == null || !dispatches.hasNext()) {
                // XXX - 暂不支持指定分派
            }
            
        } while (state == SocketState.ASYNC_END || 
                dispatches != null && state != SocketState.CLOSED);
        
        return state;
    }

    @Override
    public boolean isUpgrade() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void timeoutAsync(long now) {

    }

    public final SocketWrapperBase<?> getSocketWrapper() {
        return socketWrapper;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void recycle() {

    }

    @Override
    public ByteBuffer getLeftoverInput() {
        return null;
    }

    @Override
    public void pause() {

    }

    @Override
    public boolean checkAsyncTimeoutGeneration() {
        return false;
    }


    private SocketState checkForPipelinedData(SocketState inState, SocketWrapperBase<?> socketWrapper)
            throws IOException {
        if (inState == SocketState.OPEN) {
            // There may be pipe-lined data to read. If the data isn't
            // processed now, execution will exit this loop and call
            // release() which will recycle the processor (and input
            // buffer) deleting any pipe-lined data. To avoid this,
            // process it now.
            return service(socketWrapper);
        } else {
            return inState;
        }
    }
    

    /**
     * 调度分派，处理非标准HTTP通信流程。<br>
     * 如Servlet 3.0的异步容器处理
     * 
     * @param event 调度事件
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public SocketState dispatch(SocketEvent event) throws IOException {
        return null;
    }


    @Override
    public SocketState asyncPostProcess() {
        return null;
    }


    /**
     * 钩子方法
     * 
     * @param actionCode 动作代码
     * @param param 携带参数
     */
    @Override
    public void action(ActionCode actionCode, Object param) {
        
    }
}
