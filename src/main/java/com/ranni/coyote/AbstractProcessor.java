package com.ranni.coyote;

import com.ranni.container.Host;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.DispatchType;
import com.ranni.util.net.SocketEvent;
import com.ranni.util.net.SocketWrapperBase;

import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Title: HttpServer
 * Description:
 * 
 * TODO:
 * XXX - 钩子方法的判断不易于升级。可以考虑利用Map+反射机制
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

    private char[] hostNameC = new char[0];

    /**
     * 异步超时时间
     */
    private volatile long asyncTimeout = -1;

    private volatile long asyncTimeoutGeneration = 0;

    /**
     * 处理请求/响应的错误状态
     */
    private ErrorState errorState = ErrorState.NONE;
    

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
    protected abstract void populatePort();
    

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
                state = service(socketWrapper);
            } else if (event == SocketEvent.CONNECT_FAIL) {
                // XXX - SSL连接建立失败，响应400
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

    
    /**
     * Host标头解析
     *
     * @param hostMB host字节消息块
     */
    protected void parseHost(MessageBytes hostMB) {
        if (hostMB == null || hostMB.isNull()) {
            // XXX - 没有这部分的数据
            return;
        } else if (hostMB.getLength() == 0) {
            request.serverName().setString("");
            populatePort();
            return;
        }
        
        ByteChunk hostBC = hostMB.getByteChunk();
        byte[] hostB = hostBC.getBytes();
        int len = hostBC.getLength();
        int start = hostBC.getStart();
        if (hostNameC.length < len) {
            hostNameC = new char[len];
        }
        
        try {
            int colonPos = Host.parse(hostMB);
            
            if (colonPos != -1) {
                // 指明了端口号的，解析端口号
                int port = 0;
                for (int i = colonPos + 1; i < len; i++) {
                    byte b = hostB[i + start];
                    if (b < 48 || b > 57) {
                        response.setStatus(400);
                        setErrorState(ErrorState.CLOSE_CLEAN, null);
                        return;
                    }
                    
                    port = port * 10 + b - 48;
                }
                request.setServerPort(port);
                len = colonPos;
            }
            
            // 解析主机名
            for (int i = 0; i < len; i++) {
                hostNameC[i] = (char) (hostB[start + i]);
            }
            request.serverName().setChars(hostNameC, 0, len);
            
        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            setErrorState(ErrorState.CLOSE_CLEAN, e);
        }
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
    
    protected void setSocketWrapper(SocketWrapperBase<?> socketWrapper) {
        this.socketWrapper = socketWrapper;
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
        errorState = ErrorState.NONE;
//        asyncStateMachine.recycle();
    }

    @Override
    public ByteBuffer getLeftoverInput() {
        return null;
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
    public final SocketState dispatch(SocketEvent event) throws IOException {
        // TODO - 待实现
        return null;
    }


    @Override
    public SocketState asyncPostProcess() {
        return null;
    }


    /**
     * 如果新的错误比当前的错误更严重，则更新当前错误为新的错误。
     * 
     * @param errorState 错误状态码
     * @param t 异常
     */
    protected void setErrorState(ErrorState errorState, Throwable t) {
        boolean error = response.setError();
        boolean blockIo = this.errorState.isIoAllowed() && !errorState.isIoAllowed();
        this.errorState = this.errorState.getMostSevere(errorState);
        
        if (response.getStatus() < 400 && !(t instanceof IOException)) {
            response.setStatus(500);
        }
        if (t != null) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
        }
        if (blockIo && isAsync() && error) {
            // TODO 异步处理此事件
        }
    }


    /**
     * @return 返回处理当前请求/响应的错误状态
     */
    protected ErrorState getErrorState() {
        return errorState;
    }
    

    /**
     * IO异常处理
     */
    private void handleIOException (IOException ioe) {
        if (ioe instanceof CloseNowException) {
            // 关闭信道，但是保持连接
            setErrorState(ErrorState.CLOSE_NOW, ioe);
        } else {
            // 关闭连接以及连接内的所有信道
            setErrorState(ErrorState.CLOSE_CONNECTION_NOW, ioe);
        }
    }


    /**
     * 是否允许从套接字获取属性的方法。默认是允许的，子类如果需要其
     * 它的判断方式，需要重写此方法
     * 
     * @return 如果返回<b>true</b>，则表示允许从套接字获取属性
     */
    protected boolean getPopulateRequestAttributesFromSocket() {
        return true;
    }
    

    /**
     * 钩子方法
     * 
     * @param actionCode 动作代码
     * @param param 携带参数
     */
    @Override
    public final void action(ActionCode actionCode, Object param) {
        switch (actionCode) {
            // 发送响应头
            case COMMIT: {
                if (!response.isCommitted()) {
                    try {
                        prepareResponse();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
                break;
            }

            // 完成发送，关闭此条通信信道
            case CLOSE: {
                action(ActionCode.COMMIT, null);
                try {
                    finishResponse();
                } catch (IOException e) {
                    handleIOException(e);
                }
                break;
            }
                
            // 对于100-continue的处理态度
            case ACK: {
                ack((ContinueResponseTiming) param);
                break;
            }
                
            // 刷新输出缓冲区
            case CLIENT_FLUSH: {
                action(ActionCode.COMMIT, null);
                try {
                    flush();
                } catch (IOException e) {
                    handleIOException(e);
                }
                break;
            }
            
            // 设置请求实例的输入缓冲区中可读数据量。
            // param表示是否允许在没有数据可读时非
            // 阻塞式从socket缓冲区中读数据到请求
            // 实例的输入缓冲区中。
            case AVAILABLE: {
                request.setAvailable(available(Boolean.TRUE.equals(param)));
                break;
            }
            
            // 设置请求体数据
            case REQ_SET_BODY_REPLAY: {
                setRequestBody((ByteChunk) param);
                break;
            }
            
            // 设置当前是否存在错误
            case IS_ERROR: {
                ((AtomicBoolean) param).set(getErrorState().isError());
                break;
            }
                
            // 设置当前错误是否可以进行缓冲区I/O
            case IS_IO_ALLOWED: {
                ((AtomicBoolean) param).set(getErrorState().isIoAllowed());
                break;
            }
                
            // 立马关闭连接
            case CLOSE_NOW: {
                // 告知缓冲区完成响应
                setSwallowResponse();
                if (param instanceof Throwable) {
                    setErrorState(ErrorState.CLOSE_NOW, (Throwable) param);
                } else {
                    setErrorState(ErrorState.CLOSE_NOW, null);
                }
                break;
            }
                
            // 禁用请求正文
            case DISABLE_SWALLOW_INPUT: {
                disableSwallowRequest();
                setErrorState(ErrorState.CLOSE_CLEAN, null);
                break;
            }

            // 填充远程主机端口号
            case REQ_REMOTEPORT_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.setRemotePort(socketWrapper.getRemotePort());
                }
                break;
            }
                
            // 填充远程主机地址
            case REQ_HOST_ADDR_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.remoteAddr().setString(socketWrapper.getRemoteAddr());
                }
                break;
            }
                
            // 填充远程主机地址
            case REQ_PEER_ADDR_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.peerAddr().setString(socketWrapper.getRemoteAddr());
                }
                break;
            }
                
            // 填充远程主机
            case REQ_HOST_ATTRIBUTE: {
                populateRequestAttributeRemoteHost();
                break;
            }
               
            // 填充服务器本地端口
            case REQ_LOCALPORT_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.setLocalPort(socketWrapper.getLocalPort());
                }
                break;
            }
                
            // 填充服务器地址
            case REQ_LOCAL_ADDR_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.localAddr().setString(socketWrapper.getLocalAddr());
                }
                break;
            }
            
            // 填充服务器名
            case REQ_LOCAL_NAME_ATTRIBUTE: {
                if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
                    request.localName().setString(socketWrapper.getLocalName());
                }
                break;
            }
            
            // SSL支持
            case REQ_SSL_ATTRIBUTE: {
                populateSslRequestAttributes();
                break;
            }
            
            // 取得SSL证书
            case REQ_SSL_CERTIFICATE: {
                try {
                    sslReHandShake();
                } catch (IOException ioe) {
                    setErrorState(ErrorState.CLOSE_CONNECTION_NOW, ioe);
                }
                break;
            }
            
            // Servlet 3.0 的异步支持
            case ASYNC_START: {
                
                break;
            }
            
            case ASYNC_COMPLETE: {
                
                break;
            }
            
            case ASYNC_DISPATCH: {
                
                break;
            }
            
            case ASYNC_DISPATCHED: {
                
                break;
            }
            
            case ASYNC_ERROR: {
                
                break;
            }
            
            case ASYNC_IS_ASYNC: {
                
                break;
            }
            
            case ASYNC_IS_COMPLETING: {
                
                break;
            }
            
            case ASYNC_IS_DISPATCHING: {
                
                break;
            }
            
            case ASYNC_IS_ERROR: {
                
                break;
            }
            
            case ASYNC_IS_STARTED: {
                
                break;
            }
            
            case ASYNC_IS_TIMINGOUT: {
                
                break;
            }
            
            case ASYNC_RUN: {
                
                break;
            }
            
            case ASYNC_SETTIMEOUT: {
                
                break;
            }
            
            case ASYNC_TIMEOUT: {
                
                break;
            }
            
            case ASYNC_POST_PROCESS: {
                
                break;
            }
            
            case REQUEST_BODY_FULLY_READ: {
                
                break;
            }
            
            case NB_READ_INTEREST: {
                
                break;
            }
            
            case NB_WRITE_INTEREST: {
                
                break;
            }
            
            case DISPATCH_READ: {
                
                break;
            }
            
            case DISPATCH_WRITE: {
                
                break;
            }

            case DISPATCH_EXECUTE: {

                break;
            }

            case UPGRADE: {

                break;
            }

            case IS_PUSH_SUPPORTED: {

                break;
            }


            case PUSH_REQUEST: {

                break;
            }

            case IS_TRAILER_FIELDS_READY: {

                break;
            }
            
            case IS_TRAILER_FIELDS_SUPPORTED: {

                break;
            }
            
            case CONNECTION_ID: {

                break;
            }
            
            case STREAM_ID: {

                break;
            }
         }
    }


    /**
     * 取得SSL证书
     */
    protected void sslReHandShake() throws IOException {
        
    }


    /**
     * SSL支持
     */
    protected void populateSslRequestAttributes() {
        
    }


    /**
     * 填充远程主机地址
     */
    protected void populateRequestAttributeRemoteHost() {
        if (getPopulateRequestAttributesFromSocket() && socketWrapper != null) {
            request.remoteHost().setString(socketWrapper.getRemoteHost());
        }
    }


}
