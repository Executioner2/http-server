package com.ranni.coyote;

import com.ranni.util.net.AbstractEndpoint;
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
 * @Date 2022/6/12 20:07
 * @Ref org.apache.coyote.AbstractProcessor
 */
public class AbstractProcessor implements Processor  {
    @Override
    public AbstractEndpoint.Handler.SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent status) throws IOException {
        return null;
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

    @Override
    public Request getRequest() {
        return null;
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
}
