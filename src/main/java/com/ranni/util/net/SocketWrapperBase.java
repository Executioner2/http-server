package com.ranni.util.net;

import com.ranni.connector.ApplicationBufferHandler;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/30 15:08
 * @Ref org.apache.tomcat.util.net.SocketWrapperBase
 */
public abstract class SocketWrapperBase<E> {

    /**
     * 读取超时
     */
    private long readTimeout;

    public abstract boolean isReadyForRead() throws IOException;
    public abstract void setAppReadBufHandler(ApplicationBufferHandler handler);
    public abstract int read(boolean block, byte[] b, int off, int len) throws IOException;
    public abstract int read(boolean block, ByteBuffer byteBuffer) throws IOException;

    public SocketBufferHandler getSocketBufferHandler() {
        return null;
    }


    /**
     * 设置读取超时，-1表示无限制。
     * 
     * @param readTimeout 小于等于 0时设置为-1
     */
    public void setReadTimeout(long readTimeout) {
        if (readTimeout > 0) { 
            this.readTimeout = readTimeout;
        } else {
            this.readTimeout = -1;
        }
    }
}
