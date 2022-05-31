package com.ranni.util.net;


import com.ranni.connector.ApplicationBufferHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // ==================================== 属性字段 ====================================

    /**
     * socket
     */
    private E socket;
    
    /**
     * 接收端点
     */
    private final AbstractEndpoint<E, ?> endpoint;

    /**
     * socket关闭标志位
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 读取超时
     */
    private volatile long readTimeout = -1;

    /**
     * 写入超时
     */
    private volatile long writeTimeout = -1;

    /**
     * 上一个I/O异常
     */
    protected volatile IOException previousIOException;

    /**
     * 保持连接
     */
    private volatile int keepAliveLeft = 100;

    /**
     * why - 升级？
     */
    private volatile boolean upgraded;

    /**
     * 安全标志位
     */
    private boolean secure;

    /**
     * 协商协议
     */
    private String negotiatedProtocol;

    /**
     * 接收请求的服务器IP
     */
    protected String localAddr;

    /**
     * 接收请求的服务器名
     */
    protected String localName = null;

    /**
     * 接收请求的服务器端口号
     */
    protected int localPort = -1;

    /**
     * 发起请求的客户端IP
     */
    protected String remoteAddr = null;

    /**
     * 发起请求的主机名
     */
    protected String remoteHost = null;

    /**
     * 发起请求的端口号
     */
    protected int remotePort = -1;

    // ==================================== 构造方法 ====================================
    
    public SocketWrapperBase(E socket, AbstractEndpoint<E,?> endpoint) {
        this.socket = socket;
        this.endpoint = endpoint;
    }
    

    // ==================================== 抽象方法 ====================================
    
    public abstract boolean isReadyForRead() throws IOException;
    public abstract void setAppReadBufHandler(ApplicationBufferHandler handler);
    public abstract int read(boolean block, byte[] b, int off, int len) throws IOException;
    public abstract int read(boolean block, ByteBuffer byteBuffer) throws IOException;


    // ==================================== 核心方法 ====================================
    
    
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


    /**
     * @return 如果返回<b>true</b>，表示还有数据可读
     */
    public boolean hasDataToRead() {
        return true;
    }
}
