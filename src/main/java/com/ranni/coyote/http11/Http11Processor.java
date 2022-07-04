package com.ranni.coyote.http11;

import com.ranni.coyote.*;
import com.ranni.coyote.Constants;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.http.parse.HttpParser;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 适用于Http1.1的处理器，此实例在对请求做过部分处理后会把请求交付给容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 20:06
 * @Ref org.apache.coyote.http11.Http11Processor
 */
public class Http11Processor extends AbstractProcessor {

    // ==================================== 基本属性字段 ====================================

    /**
     * http1.1协议实例
     */
    private final AbstractHttp11Protocol<?> protocol;

    /**
     * 适用于http1.1的输入缓冲处理
     */
    private final Http11InputBuffer inputBuffer;

    /**
     * 适用于http1.1的输出缓冲处理
     */
    private final Http11OutputBuffer outputBuffer;

    /**
     * Http解析器
     */
    private final HttpParser httpParser;
    
    
    // ==================================== 构造方法 ====================================
    
    public Http11Processor(AbstractHttp11Protocol<?> protocol, Adapter adapter) {
        super(adapter);
        this.protocol = protocol;
    
        httpParser = new HttpParser(protocol.getRelaxedPathChars(), protocol.getRelaxedQueryChars());
        
        inputBuffer = new Http11InputBuffer(request, protocol.getMaxHttpRequestHeaderSize(), protocol.getRejectIllegalHeader(), httpParser);
        request.setInputBuffer(inputBuffer);
        
        outputBuffer = new Http11OutputBuffer(response, protocol.getMaxHttpResponseHeaderSize());
        response.setOutputBuffer(outputBuffer);
        
        // XXX - input/output 缓冲区过滤阀
    }


    // ==================================== 核心方法 ====================================


    /**
     * 是否是需要断开连接的状态码
     * 
     * @param status 状态码
     * @return 如果返回<b>true</b>，则表示此状态码应该断开与客户端的会话连接
     */
    private static boolean statusDropsConnection(int status) {
        return status == 400 /* SC_BAD_REQUEST */ ||
               status == 408 /* SC_REQUEST_TIMEOUT */ ||
               status == 411 /* SC_LENGTH_REQUIRED */ ||
               status == 413 /* SC_REQUEST_ENTITY_TOO_LARGE */ ||
               status == 414 /* SC_REQUEST_URI_TOO_LONG */ ||
               status == 500 /* SC_INTERNAL_SERVER_ERROR */ ||
               status == 503 /* SC_SERVICE_UNAVAILABLE */ ||
               status == 501 /* SC_NOT_IMPLEMENTED */;
    }


    /**
     * 处理请求的方法（究极核心的方法），会调用适配器然后由
     * 适配器交付给对应的容器。
     * 
     * @param socketWrapper 要处理的socket包装实例
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException {
        RequestInfo ri = request.getRequestProcessor();
        ri.setStage(Constants.STAGE_PARSE); // 切换到请求解析状态
        
        setSocketWrapper(socketWrapper);
        
        return null;
    }

    private void setSocketWrapper(SocketWrapperBase<?> socketWrapper) {
    }


    /**
     * 响应前准备
     * 
     * @throws IOException
     */
    @Override
    protected void prepareResponse() throws IOException {
        
    }


    /**
     * 完成响应
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected void finishResponse() throws IOException {
        outputBuffer.end();
    }


    /**
     * 对于100-continue的处理态度
     * 
     * @param continueResponseTiming 100-continue的处理态度
     */
    @Override
    protected void ack(ContinueResponseTiming continueResponseTiming) {
        if (continueResponseTiming == ContinueResponseTiming.ALWAYS
            || continueResponseTiming == protocol.getContinueResponseTimingInternal()) {
            
            if (!response.isCommitted() && request.hasExpectation()) {
                inputBuffer.setSwallowInput(true);
                try {
                    outputBuffer.sendAck();
                } catch (IOException e) {
                    setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
                }
            }
        }
    }

    /**
     * 刷新输出缓冲区
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected void flush() throws IOException {
        outputBuffer.flush();
    }


    /**
     * 输入缓冲区可读取数据量
     * 
     * @param doRead 如果为<b>true</b>，则表示允许在没有数据的情况下非阻塞读取数据到输入缓冲区
     * @return 返回输入缓冲区可读数据量
     */
    @Override
    protected int available(boolean doRead) {
        return inputBuffer.available(doRead);
    }


    /**
     * TODO - 设置请求体数据到请求实例的输入缓冲区中
     * 
     * @param body 请求体数据
     */
    @Override
    protected void setRequestBody(ByteChunk body) {

    }

    @Override
    protected void setSwallowResponse() {

    }

    @Override
    protected void disableSwallowRequest() {

    }

    @Override
    protected boolean isRequestBodyFullyRead() {
        return false;
    }

    @Override
    protected void registerReadInterest() {

    }

    @Override
    protected boolean isReadyForWrite() {
        return false;
    }

    
}
