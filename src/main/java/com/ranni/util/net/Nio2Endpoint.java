package com.ranni.util.net;

import com.ranni.util.collections.SynchronizedStack;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

/**
 * Title: HttpServer
 * Description:
 * NIO2。有些地方也称其为AIO
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/4 12:18
 * @Ref org.apache.tomcat.util.net.Nio2Endpoint
 */
public class Nio2Endpoint extends AbstractJsseEndpoint<Nio2Channel, AsynchronousSocketChannel> {

    
    // ==================================== 属性字段 ====================================
    
    /**
     * NIO2的server socket
     */
    private volatile AsynchronousServerSocketChannel serverSocket = null;

    /**
     * 检测程序是否内联（直到容器内都是在一个线程内完成处理）
     */
    private static ThreadLocal<Boolean> inlineCompletion = new ThreadLocal<>();
    
    /**
     * 与server socket相关的线程组<br>
     * 相当于选择器，选择感兴趣的事件注册到此线程组（选择器）中，然后会自动帮我们做数据IO
     */
    private AsynchronousChannelGroup threadGroup = null;

    /**
     * XXX - 所有线程是否都已经关闭了
     */
    private volatile boolean allClosed;

    /**
     * Nio2Channel缓存
     */
    private SynchronizedStack<Nio2Channel> nioChannels;

    /**
     * 上一个发起请求的客户端地址
     */
    private SocketAddress previousAcceptedSocketRemoteAddress;

    /**
     * 上一个请求发来的时间
     */
    private long previousAcceptedSocketNanoTime = 0;


    // ==================================== 内部类 ====================================

    /**
     * Nio2接收器。实现了CompletionHandler，用于处理连接
     */
    protected class Nio2Acceptor extends Acceptor<AsynchronousSocketChannel>
            implements CompletionHandler<AsynchronousSocketChannel, Void> {

        /**
         * 错误延迟
         */
        protected int errorDelay = 0;


        // ==================================== 构造方法 ====================================
        
        /**
         * 构造Nio2请求接收器实例
         */
        public Nio2Acceptor(AbstractEndpoint<?, AsynchronousSocketChannel> endpoint) {
            super(endpoint);
        }

        
        // ==================================== 核心方法 ====================================
        
        /**
         * 一个接收请求的线程
         */
        @Override
        public void run() {
            if (!isPaused()) {
                try {
                    countUpOrAwaitConnection();
                } catch (InterruptedException e) {
                    ;
                }
                
                if (!isPaused()) {
                    serverSocket.accept(null, this);
                } else {
                    state = AcceptorState.PAUSED;
                }
            } else {
                state = AcceptorState.PAUSED;
            }
        }


        /**
         * 停止接收器
         * 
         * @param waitSeconds 此参数在NIO2中忽略
         */
        @Override
        public void stop(int waitSeconds) {
            acceptor.state = AcceptorState.ENDED;
        }
        

        /**
         * 三次握手连接成功，得到此与此请求通信的AsynchronousSocketChannel
         * 
         * @param socket 与此请求通信的套接字
         * @param attachment 附加参数
         */
        @Override
        public void completed(AsynchronousSocketChannel socket, Void attachment) {
            errorDelay = 0;
            
            if (isRunning() && !isPaused()) {
                if (getMaxConnections() == -1) {
                    // 同意连接，扔进AsynchronousChannelGroup中异步处理
                    serverSocket.accept(null, this);
                } else if (getConnectionCount() < getMaxConnections()) {
                    try {
                        // 连接数+1
                        countUpOrAwaitConnection();
                    } catch (InterruptedException e) {
                        ;
                    }
                    // 同意连接，扔进AsynchronousChannelGroup中异步处理
                    serverSocket.accept(null, this);
                } else {                    
                    if (isRunning()) {
                        state = AcceptorState.PAUSED;
                    }
                    // 不处理接收到的这个请求，销毁它
                    destroySocket(socket);
                }
            }
        }


        /**
         * 三次握手失败！
         * 
         * @param exc 出现的异常
         * @param attachment 附加参数
         */
        @Override
        public void failed(Throwable exc, Void attachment) {
            if (isRunning()) {
                if (!isPaused()) {
                    // 尝试返回这个错误
                    if (getMaxConnections() == -1) {
                        serverSocket.accept(null, this);
                    } else {
                        getExecutor().execute(this);
                    }
                } else {
                    state = AcceptorState.PAUSED;
                }

                countDownConnection();
                System.err.println("端点接收请求失败  " + exc);
            } else {
                countDownConnection();
            }
        }
        
        
    }
    
    
    
    // ==================================== 核心方法 ====================================
    
    @Override
    protected InetSocketAddress getLocalAddress() throws IOException {
        return null;
    }

    @Override
    protected SocketProcessorBase<Nio2Channel> createSocketProcessor(SocketWrapperBase<Nio2Channel> socketWrapper, SocketEvent event) {
        return null;
    }


    /**
     * 绑定server socket<br>
     * 进行的操作：<br>
     * <ol>
     *     <li>创建工作的线程池</li>
     *     <li>生成异步通道组</li>
     *     <li>打开并设置一个server socket</li>
     * <ol/>
     * 
     * @throws Exception 可能抛出异常 
     */
    @Override
    public void bind() throws Exception {
        // 创建工作的线程池
        if (getExecutor() == null) {
            createExecutor();
        }
        
        // 生成异步通道组，用于注册感兴趣的socket事件
        if (getExecutor() instanceof ExecutorService) {
            threadGroup = AsynchronousChannelGroup.withThreadPool((ExecutorService) getExecutor());
        }
        
        serverSocket = AsynchronousServerSocketChannel.open(threadGroup);
        socketProperties.setProperties(serverSocket);
        InetSocketAddress addr = new InetSocketAddress(getAddress(), getPort());
        serverSocket.bind(addr, getAcceptCount());
    }

    @Override
    public void unbind() throws Exception {

    }


    /**
     * 启动NIO2 端点并创建接收器（acceptor）<br>
     * 会创建处理器缓存，信道（NioChannel）缓存
     * 
     * @throws Exception 可能抛出异常
     */
    @Override
    public void startInternal() throws Exception {
        if (!running) {
            allClosed = false;
            running = true;
            paused = false;
            
            if (socketProperties.getProcessorCache() != 0) {
                processorCache = new SynchronizedStack<>(SynchronizedStack.DEFAULT_SIZE,
                        socketProperties.getProcessorCache());
            }
            if (socketProperties.getBufferPool() != 0) {
                nioChannels = new SynchronizedStack<>(SynchronizedStack.DEFAULT_SIZE,
                        socketProperties.getBufferPool());
            }
            
            if (getExecutor() == null) {
                createExecutor();
            }
            
            initializeConnectionLatch();
            startAcceptorThread();
        }
    }


    /**
     * 启动Nio2的接收器线程
     */
    @Override
    protected void startAcceptorThread() {
        if (acceptor == null) {
            acceptor = new Nio2Acceptor(this);
        }
    }

    @Override
    public void stopInternal() throws Exception {

    }

    @Override
    protected void doCloseServerSocket() throws IOException {

    }

    @Override
    protected AsynchronousSocketChannel serverSocketAccept() throws Exception {
        return null;
    }

    @Override
    protected boolean setSocketOptions(AsynchronousSocketChannel socket) {
        return false;
    }

    @Override
    protected void destroySocket(AsynchronousSocketChannel socket) {

    }
}
