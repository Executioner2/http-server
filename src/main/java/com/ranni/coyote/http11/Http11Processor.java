package com.ranni.coyote.http11;

import com.ranni.coyote.*;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 20:06
 */
public class Http11Processor extends AbstractProcessor {
    private final AbstractProtocol<?> protocol;

    private final Http11InputBuffer inputBuffer;
    private final Http11OutputBuffer outputBuffer;
    
    public Http11Processor(AbstractProtocol<?> protocol, Adapter adapter) {
        super(adapter);
        this.protocol = protocol;
    
    }
    


    @Override
    protected void prepareResponse() throws IOException {
        
    }

    @Override
    protected void finishResponse() throws IOException {

    }

    @Override
    protected void ack(ContinueResponseTiming continueResponseTiming) {

    }

    @Override
    protected void flush() throws IOException {

    }

    @Override
    protected int available(boolean doRead) {
        return 0;
    }

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


    /**
     * 处理请求的方法，会调用适配器然后由适配器交付给对应的容器
     *
     * @param socketWrapper 要处理的socket包装实例
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException {
        return null;
    }
}
