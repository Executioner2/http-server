package com.ranni.util.net;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:10
 * @Ref org.apache.tomcat.util.net.Acceptor
 */
public class Acceptor<U> implements Runnable {
    
    private final AbstractEndpoint<?, U> endpoint;
    private String threadName;

    public Acceptor(AbstractEndpoint<?, U> endpoint) {
        this.endpoint = endpoint;
    }
    
    @Override
    public void run() {
        
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void stop(int waitSeconds) {
        
    }
}
