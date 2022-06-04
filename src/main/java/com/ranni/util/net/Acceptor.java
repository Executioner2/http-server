package com.ranni.util.net;

/**
 * Title: HttpServer
 * Description:
 * server socket请求接收器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:10
 * @Ref org.apache.tomcat.util.net.Acceptor
 */
public class Acceptor<U> implements Runnable {

    
    // ==================================== 属性字段 ====================================
    
    /**
     * 接收器的状态
     */
    protected volatile AcceptorState state = AcceptorState.NEW;

    /**
     * 接收器是否处于停止状态
     */
    private volatile boolean stopCalled = false;

    /**
     * 端点
     */
    private final AbstractEndpoint<?, U> endpoint;

    /**
     * 接收器线程名
     */
    private String threadName;


    // ==================================== 构造方法 ====================================
    
    public Acceptor(AbstractEndpoint<?, U> endpoint) {
        this.endpoint = endpoint;
    }


    // ==================================== 内部类 ====================================

    public enum AcceptorState {
        NEW, RUNNING, PAUSED, ENDED
    }


    // ==================================== 核心方法 ====================================
    
    @Override
    public void run() {
        
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void stop(int waitSeconds) {
        
    }
}
