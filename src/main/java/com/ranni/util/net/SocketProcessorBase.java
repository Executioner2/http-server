package com.ranni.util.net;

/**
 * Title: HttpServer
 * Description:
 * socket处理器抽象类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:12
 * @Ref org.apache.tomcat.util.net.SocketProcessorBase
 */
public abstract class SocketProcessorBase<S> implements Runnable {
    public void reset(SocketWrapperBase<S> socketWrapper, SocketEvent event) {
        
    }
}
