package com.ranni.util.net;

import java.util.Objects;

/**
 * Title: HttpServer
 * Description:
 * socket处理器抽象类
 * 
 * @param <S> 信道类型
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:12
 * @Ref org.apache.tomcat.util.net.SocketProcessorBase
 */
public abstract class SocketProcessorBase<S> implements Runnable {
    
    protected SocketWrapperBase<S> socketWrapper;
    protected SocketEvent event;

    public SocketProcessorBase(SocketWrapperBase<S> socketWrapper, SocketEvent event) {
        reset(socketWrapper, event);
    }

    public void reset(SocketWrapperBase<S> socketWrapper, SocketEvent event) {
        Objects.requireNonNull(event);   
        this.socketWrapper = socketWrapper;
        this.event = event;
    }


    /**
     * socket处理器线程，交付给容器
     */
    @Override
    public void run() {
        synchronized (socketWrapper) {
            if (socketWrapper.isClosed()) {
                return;
            }
            
            doRun();
        }
    }

    /**
     * socket处理器线程中的方法，正儿八经开始处理
     * 请求，再处理器中处理下就会交付给容器
     */
    protected abstract void doRun();
}
