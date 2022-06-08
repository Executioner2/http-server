package com.ranni.util.net;

import com.ranni.util.collections.SynchronizedStack;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Title: HttpServer
 * Description:
 * 抽象的接收端点
 * 
 * @param <S> 信道包装类型。NIO、NIO2 或者 APR。也有可能和U一样
 * @param <U> 底层信道类型。如AsynchronousSocketChannel。也有可能和S一样
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/31 11:56
 * @Ref org.apache.tomcat.util.net.AbstractEndpoint
 */
public abstract class AbstractEndpoint<S, U> {

    // ==================================== 属性字段 ====================================


    /**
     * 是否使用内部执行器（即是否使用此实例自己创建的线程池）
     */
    protected volatile boolean internalExecutor = true;
    
    /**
     * 接收端点是否正在运行 
     */
    protected volatile boolean running = false;

    /**
     * 接收端点是否暂停
     */
    protected volatile boolean paused = false;

    /**
     * 套接字处理器
     */
    private Handler<S> handler;

    /**
     * 请求流量控制
     */
   private volatile LimitLatch connectionLimitLatch = null;

    /**
     * 连接请求接收器
     */
    protected Acceptor<U> acceptor;

    /**
     * socket属性
     */
    protected final SocketProperties socketProperties = new SocketProperties();

    /**
     * socket处理器缓存
     */
    protected SynchronizedStack<SocketProcessorBase<S>> processorCache;

    /**
     * 对象名
     */
    private ObjectName objectName;

    /**
     * 保存相同信道的套接字
     */
    protected Map<U, SocketWrapperBase<S>> connections = new ConcurrentHashMap<>();

    /**
     * 能否发送文件
     */
    private boolean useSendfile = true;

    /**
     * 端点关闭时等待内部执行程序的时间。5S 
     */
    private long executorTerminationTimeoutMillis = 5000;

    /**
     * 接收线程的优先级
     */
    protected int acceptorThreadPriority = Thread.NORM_PRIORITY;

    /**
     * 最大连接数
     */
    private int maxConnections = 8 * 1024;

    /**
     * 多线程执行器（线程池）
     */
    private Executor executor;

    /**
     * 定时任务
     */
    private ScheduledExecutorService utilityExecutor = null;

    /**
     * server socket 端口号
     */
    private int port = -1;

    /**
     * why - 端口偏移
     */
    private int portOffset = 0;

    /**
     * server socket IP
     */
    private InetAddress address;

    /**
     * server socket 最大等待连接数
     */
    private int acceptCount = 100;

    /**
     * server socket 端口绑定时机<br>
     * 如果为<b>true</b>，则表示在{@link #init()}时绑定，在{@link #destroy()}解绑<br>
     * 否则，在{@link #start()}时绑定，在{@link #stop()}时解绑 
     */
    private boolean bindOnInit = true;

    /**
     * 绑定状态
     */
    private volatile BindState bindState = BindState.UNBOUND;

    /**
     * keep-alive属性连接的超时时长，如果未设置则为soTimeout
     */
    private Integer keepAliveTimeout = null;

    /**
     * 设置初始的核心线程数
     */
    private int minSpareThreads = 10;
    
    /**
     * 工作最大线程数
     */
    private int maxThreads = 200;

    /**
     * 工作线程优先级
     */
    protected int threadPriority = Thread.NORM_PRIORITY;
    
    /**
     * keep-alive属性连接的最大数量
     */
    private int maxKeepAliveRequests = 100;

    /**
     * 线程池的名称 xxx - 在线程池没有用到，在接收器线程倒是用到了
     */
    private String name = "TP";

    /**
     * 域名
     */
    private String domain;

    /**
     * 是否默认为守护线程
     */
    private boolean daemon = true;

    /**
     * 是否使用异步IO
     */
    private boolean useAsyncIO = true;

    /**
     * 接收端点属性
     */
    protected HashMap<String, Object> attributes = new HashMap<>();

    /**
     * 协议集合
     */
    protected final List<String> negotiableProtocols = new ArrayList<>();
    
    
    // ==================================== 内部类 ====================================

    /**
     * 套接字处理器
     */
    public interface Handler<S> {
        /**
         * 套接字处理状态
         */
        enum SocketState {
            OPEN, CLOSED, LONG, ASYNC_END, SENDFILE, UPGRADING, UPGRADED, SUSPENDED
        }

        
        /**
         * 套接字处理
         * 
         * @param socket 套接字包装
         * @param status 处理事件
         * @return 返回处理状态
         */
        SocketState process(SocketWrapperBase<S> socket, SocketEvent status);


        /**
         * @return 返回全局请求处理器
         */
        Object getGlobal();


        /**
         * 释放传入的套接字的所有资源
         * 
         * @param socketWrapper 要释放资源的套接字
         */
        void release(SocketWrapperBase<S> socketWrapper);


        /**
         * 通知端点停止接收新的连接
         */
        void pause();


        /**
         * 回收与此处理器相关的资源
         */
        void recycle();
    }


    /**
     * 绑定状态
     */
    protected enum BindState {
        UNBOUND(false, false),
        BOUND_ON_INIT(true, true),
        BOUND_ON_START(true, true),
        SOCKET_CLOSED_ON_STOP(false, true);

        private final boolean bound;
        private final boolean wasBound;

        BindState(boolean bound, boolean wasBound) {
            this.bound = bound;
            this.wasBound = wasBound;
        }

        public boolean isBound() {
            return bound;
        }

        public boolean wasBound() {
            return wasBound;
        }
    }
    

    // ==================================== 基本方法 ====================================

    /**
     * 调用超时
     * 
     * @param timeout 超时时间
     * @return 返回超时时间
     */
    public static long toTimeout(long timeout) {
        return (timeout > 0) ? timeout : Long.MAX_VALUE;
    }
    
    public Set<SocketWrapperBase<S>> getConnections() {
        return new HashSet<>(connections.values());
    }
    
    public SocketProperties getSocketProperties() {
        return socketProperties;
    }
        
    public void setUseAsyncIO(boolean useAsyncIO) {
        this.useAsyncIO = useAsyncIO;
    }
    
    public boolean getUseAsyncIO() {
        return useAsyncIO;
    }

    public Executor getExecutor() {
        return executor;
    }
    
    public void setExecutor(Executor executor) {
        this.executor = executor;
        this.internalExecutor = (executor == null);
    }
    
    public boolean getUseSendfile() {
        return useSendfile;
    }
    
    public void setUseSendfile(boolean useSendfile) {
        this.useSendfile = useSendfile;
    }
    
    public long getExecutorTerminationTimeoutMillis() {
        return executorTerminationTimeoutMillis;
    }
    
    public void setExecutorTerminationTimeoutMillis(long executorTerminationTimeoutMillis) {
        this.executorTerminationTimeoutMillis = executorTerminationTimeoutMillis;
    }
    
    public int getAcceptorThreadPriority() {
        return acceptorThreadPriority;
    }
    
    public void setAcceptorThreadPriority(int acceptorThreadPriority) {
        this.acceptorThreadPriority = acceptorThreadPriority;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        LimitLatch latch = this.connectionLimitLatch;
        if (latch != null) {
            if (maxConnections == -1) {
                releaseConnectionLatch();
            } else {
                latch.setLimit(maxConnections);
            }
        } else if (maxConnections > 0) {
            initializeConnectionLatch();
        }
    }
    
    public long getMaxConnections() {
        return maxConnections;
    }
    

    /**
     * @return 返回连接数量
     */
    public long getConnectionCount() {
        LimitLatch latch = this.connectionLimitLatch;
        if (latch != null) {
            return latch.getCount();
        }
        return -1;
    }
    
    public void setUtilityExecutor(ScheduledExecutorService utilityExecutor) {
        this.utilityExecutor = utilityExecutor;
    }
    
    public ScheduledExecutorService getUtilityExecutor() {
        if (utilityExecutor == null) {
            utilityExecutor = new ScheduledThreadPoolExecutor(1);
        }
        return utilityExecutor;
    }

    
    /* 端口 */
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getPortOffset() { 
        return portOffset; 
    } 
    
    public void setPortOffset(int portOffset) {
        if (portOffset < 0) {
            throw new IllegalArgumentException("接收端点端口偏移值错误！  " + portOffset);
        }
        this.portOffset = portOffset;
    }

    /**
     * @return 如果port &gt; 0，则返回 port + portOffset。否则仅返回port
     */
    public int getPortWithOffset() {
        int port = getPort();
        if (port > 0) {
            return port + getPortOffset();
        }
        return port;
    }


    /**
     * @return 返回处理请求的server socket端口（如果有的话）。没有就返回-1
     */
    public final int getLocalPort() {
        try {
            InetSocketAddress localAddress = getLocalAddress();
            if (localAddress == null) {
                return -1;
            }
            return localAddress.getPort();
        } catch (IOException e) {
            return -1;
        }
    }


    protected abstract InetSocketAddress getLocalAddress() throws IOException;
    
    
    /* ip */
    
    public InetAddress getAddress() {
        return address;
    }
    
    public void setAddress(InetAddress address) {
        this.address = address;
    }
    
    
    /* 最大等待连接数 */
    
    public int getAcceptCount() {
        return acceptCount;
    }
    
    public void setAcceptCount(int acceptCount) {
        if (acceptCount > 0) {
            this.acceptCount = acceptCount;
        }
    }

    
    /* 端口绑定时机 */

    /**
     * server socket 端口绑定时机
     * 
     * @return 如果为<b>true</b>，则表示在{@link #init()}时绑定，在{@link #destroy()}解绑<br>
     *         否则，在{@link #start()}时绑定，在{@link #stop()}时解绑 
     */
    public boolean getBindOnInit() {
        return bindOnInit;
    }


    /**
     * 设置server socket 端口绑定时机
     * 
     * @param bindOnInit 如果为<b>true</b>，则表示在{@link #init()}时绑定，在{@link #destroy()}解绑<br>
     *                   否则，在{@link #start()}时绑定，在{@link #stop()}时解绑
     */
    public void setBindOnInit(boolean bindOnInit) {
        this.bindOnInit = bindOnInit;
    }
    
    
    /* 绑定状态 */
    
    public BindState getBindState() {
        return bindState;
    }
    
    
    /* keep-alive的超时时间 */
    
    public int getKeepAliveTimeout() {
        if (keepAliveTimeout == null) {
            return getConnectionTimeout();
        } else {
            return keepAliveTimeout.intValue();
        }
    }
    
    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = Integer.valueOf(keepAliveTimeout);
    }
    
    
    /* 设置连接超时 */
    
    public int getConnectionTimeout() {
        return socketProperties.getSoTimeout();
    }
    
    public void setConnectionTimeout(int timeout) {
        socketProperties.setSoTimeout(timeout);
    }


    /* 设置线程池核心线程数 */
    
    /**
     * 设置线程池的核心线程数
     * 
     * @param minSpareThreads 核心线程数
     */
    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
        Executor executor = this.executor;
        
        if (internalExecutor && executor instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) executor).setCorePoolSize(minSpareThreads);
        }
    }
    
    public int getMinSpareThreads() {
        return Math.min(getMinSpareThreadsInternal(), getMaxThreads());
    }

    private int getMinSpareThreadsInternal() {
        if (internalExecutor) {
            return minSpareThreads;
        } else {
            return -1;
        }
    }

    
    /* 设置线程池最大线程数 */

    /**
     * 设置线程池的最大线程数
     * 
     * @param maxThreads 最大线程数
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        Executor executor = this.executor;
        if (internalExecutor && executor instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) executor).setMaximumPoolSize(maxThreads);
        }
    }
    
    public int getMaxThreads() {
        if (internalExecutor) {
            return maxThreads;
        } else {
            return -1;
        }
    }
    
    
    /* 设置线程优先级 */
    
    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }
    
    public int getThreadPriority() {
        if (internalExecutor) {
            return threadPriority;
        } else {
            return -1;
        }
    }
    
    
    /**
     * 设置套接字延迟
     * 
     * @param tcpNoDelay 套接字延迟标志位
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        socketProperties.setTcpNoDelay(tcpNoDelay);
    }

    
    /**
     * @return 返回套接字延迟标志
     */
    public boolean getTcpNoDelay() {
        return socketProperties.getTcpNoDelay();
    }


    /**
     * @return 返回连接延迟时间
     */
    public int getConnectionLinger() { return socketProperties.getSoLingerTime(); }

    /**
     * 设置连接延迟时间
     * 
     * @param connectionLinger 连接延迟时间
     */
    public void setConnectionLinger(int connectionLinger) {
        socketProperties.setSoLingerTime(connectionLinger);
        socketProperties.setSoLingerOn(connectionLinger >= 0);
    }


    /**
     * @return 返回允许keep-alive的最大请求数
     */
    public int getMaxKeepAliveRequests() {
        if (bindState.isBound()) {
            return maxKeepAliveRequests;
        } else {
            return -1;
        }
    }
    
    public void setMaxKeepAliveRequests(int maxKeepAliveRequests) {
        this.maxKeepAliveRequests = maxKeepAliveRequests;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }
    
    public boolean getDaemon() {
        return daemon;
    }

    public String getId() {
        return null;
    }    

    public void addNegotiatedProtocol(String negotiableProtocol) {
        negotiableProtocols.add(negotiableProtocol);
    }
    
    public boolean hasNegotiableProtocols() {
        return (negotiableProtocols.size() > 0);
    }

    public Handler<S> getHandler() {
        return handler;
    }

    public void setHandler(Handler<S> handler) {
        this.handler = handler;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    
    // ==================================== 核心方法 ====================================

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }
    
    
    /**
     * @return 返回线程池当前管理的线程数
     */
    public int getCurrentThreadCount() {
        Executor executor = this.executor;
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getPoolSize();
            } else {
                return -1;
            }
        } else {
            return -2;
        }
    }


    /**
     * @return 返回线程池正在使用的线程
     */
    public int getCurrentThreadBusy() {
        Executor executor = this.executor;
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getActiveCount();
            } else {
                return -1;
            }
        } else {
            return -2;
        }
    }
    

    /**
     * 创建线程池
     */
    public void createExecutor() {
        internalExecutor = true;
        executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), 60, 
                    TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory());
    }


    /**
     * 关闭线程池
     */
    public void shutdownExecutor() {
        Executor executor = this.executor;
        if (executor != null && internalExecutor) {
            this.executor = null;
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
                tpe.shutdownNow();
                long timeout = getExecutorTerminationTimeoutMillis();
                if (timeout > 0) {
                    // 等待关闭
                    try {
                        tpe.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        ;
                    }
                    if (tpe.isTerminating()) {
                        System.out.println("线程池已关闭！"); // XXX - sout
                    }
                }
            }
        }
    }


    /**
     * 此方法的作用是建立一个连接，使准备暂停（暂停标志位设为了true）的
     * Acceptor能够及时进入到暂停状态。否则，准备暂停将等到下一个请求
     * 到来才会使Acceptor进入到暂停状态。
     */
    protected void unlockAccept() {
        if (acceptor == null || acceptor.getState() != Acceptor.AcceptorState.RUNNING) {
            return;
        }
        
        InetSocketAddress unlockAddress = null;
        InetSocketAddress localAddress = null;

        try {
            localAddress = getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (localAddress == null) {
            return;
        }
        
        try {
            unlockAddress = getUnlockAddress(localAddress);
            
            try (Socket s = new Socket()) {
                int stmo = 2 * 1000;
                int utmo = 2 * 1000;
                
                if (getSocketProperties().getSoTimeout() > stmo) {
                    stmo = getSocketProperties().getSoTimeout();
                }
                if (getSocketProperties().getUnlockTimeout() > utmo) {
                    utmo = getSocketProperties().getUnlockTimeout();
                }
                
                s.setSoTimeout(stmo);
                s.setSoLinger(getSocketProperties().getSoLingerOn(), utmo); // 设置是否超时发送RST包关闭连接
                s.connect(unlockAddress, utmo);
            }
            
            // 等待连接退出。等待1000ms。每1毫秒检查一次
            long startTime = System.nanoTime(); // 返回纳秒
            while (startTime + 1000000000 > System.nanoTime()) {
                if (startTime + 1000000 < System.nanoTime()) {
                    Thread.sleep(1);
                }
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    /**
     * 获取真实的连接地址
     * 
     * @param localAddress 连接地址
     * @return 返回真实的连接地址
     * @throws SocketException 可能抛出套接字异常
     */
    private static InetSocketAddress getUnlockAddress(InetSocketAddress localAddress) throws SocketException {
        if (localAddress.getAddress().isAnyLocalAddress()) {
            // 没有重写的类，不会进入到此方法中
            
//            // Need a local address of the same type (IPv4 or IPV6) as the
//            // configured bind address since the connector may be configured
//            // to not map between types.
//            InetAddress loopbackUnlockAddress = null;
//            InetAddress linkLocalUnlockAddress = null;
//
//            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//            while (networkInterfaces.hasMoreElements()) {
//                NetworkInterface networkInterface = networkInterfaces.nextElement();
//                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
//                while (inetAddresses.hasMoreElements()) {
//                    InetAddress inetAddress = inetAddresses.nextElement();
//                    if (localAddress.getAddress().getClass().isAssignableFrom(inetAddress.getClass())) {
//                        if (inetAddress.isLoopbackAddress()) {
//                            if (loopbackUnlockAddress == null) {
//                                loopbackUnlockAddress = inetAddress;
//                            }
//                        } else if (inetAddress.isLinkLocalAddress()) {
//                            if (linkLocalUnlockAddress == null) {
//                                linkLocalUnlockAddress = inetAddress;
//                            }
//                        } else {
//                            // Use a non-link local, non-loop back address by default
//                            return new InetSocketAddress(inetAddress, localAddress.getPort());
//                        }
//                    }
//                }
//            }
//            // Prefer loop back over link local since on some platforms (e.g.
//            // OSX) some link local addresses are not included when listening on
//            // all local addresses.
//            if (loopbackUnlockAddress != null) {
//                return new InetSocketAddress(loopbackUnlockAddress, localAddress.getPort());
//            }
//            if (linkLocalUnlockAddress != null) {
//                return new InetSocketAddress(linkLocalUnlockAddress, localAddress.getPort());
//            }
//            // Fallback
//            return new InetSocketAddress("localhost", localAddress.getPort());
        } else {
//            return localAddress;
        }

        return localAddress;
    }
    

    /**
     * 执行SocketWrapper的特定事件
     *
     * @param socketWrapper 要处理的socket包装类
     * @param event socket 要处理的socket事件
     * @param dispatch 是否应该再新的容器线程上处理（是否交给线程池执行）
     *                 
     * @return 如果返回<b>ture</b>，则表示成功处理
     */
    public boolean processSocket(SocketWrapperBase<S> socketWrapper, SocketEvent event, boolean dispatch) {
        try {
            if (socketWrapper == null) {
                return false;
            }
            
            SocketProcessorBase<S> sc = null;
            if (processorCache != null) {
                // 复用socket处理器
                sc = processorCache.pop();
            }
            if (sc == null) {
                sc = createSocketProcessor(socketWrapper, event);
            } else {
                sc.reset(socketWrapper, event);
            }

            Executor executor = getExecutor();
            if (dispatch && executor != null) {
                // 使用线程池执行
                executor.execute(sc);
            } else {
                // 和接收器一个线程执行
                sc.run();
            }            
        } catch (RejectedExecutionException ree) {
            ree.printStackTrace();
            return false;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
            
        return true;
    }

    protected abstract SocketProcessorBase<S> createSocketProcessor(SocketWrapperBase<S> socketWrapper, SocketEvent event);


    // ==================================== 生命周期管理 ====================================
    
    public abstract void bind() throws Exception;
    public abstract void unbind() throws Exception;
    public abstract void startInternal() throws Exception;
    public abstract void stopInternal() throws Exception;


    /**
     * 尝试绑定。如果触发异常，则清除绑定
     * 
     * @throws Exception 可能抛出异常
     */
    private void bindWithCleanup() throws Exception {
        try {
            bind();
        } catch (Throwable t) {
            unbind();
            throw t;
        }
    }


    /**
     * 初始化
     * 
     * @throws Exception 可能抛出异常
     */
    public  final void init() throws Exception {
        if (bindOnInit) {
            bindWithCleanup();
            bindState = BindState.BOUND_ON_INIT;
        }
        // XXX - 域名注册
    }
    
    
    public final void start() throws Exception {
        if (bindState == BindState.UNBOUND) {
            bindWithCleanup();
            bindState = BindState.BOUND_ON_START;
        }
        startInternal();
    }


    /**
     * 启动接收器线程。接收HTTP请求
     */
    protected void startAcceptorThread() {
        acceptor = new Acceptor<>(this);
        String threadName = getName() + "-Acceptor";
        acceptor.setThreadName(threadName);
        Thread thread = new Thread(acceptor, threadName);
        thread.setPriority(getAcceptorThreadPriority());
        thread.setDaemon(getDaemon());
        thread.start();
    }


    /**
     * 暂停此端点，释放连接限制阀门的资源。
     */
    public void pause() {
        if (running && !paused) {
            paused = true;
            releaseConnectionLatch();
            unlockAccept(); // 使acceptor能够及时进入到停止状态
            getHandler().pause();
        }
    }


    /**
     * 恢复暂停端点
     */
    public void resume() {
        if (running) {
            paused = false;
        }
    }


    /**
     * 停止端点
     * 
     * @throws Exception 可能抛出异常
     */
    public final void stop() throws Exception {
        stopInternal();
        if (bindState == BindState.BOUND_ON_START || bindState == BindState.SOCKET_CLOSED_ON_STOP) {
            unbind();
            bindState = BindState.UNBOUND;
        }
    }


    /**
     * 销毁端点
     * 
     * @throws Exception 可能抛出异常
     */
    public final void destroy() throws Exception {
        if (bindState == BindState.BOUND_ON_INIT) {
            unbind();
            bindState = BindState.UNBOUND;
        }
    }


    /**
     * 初始化连接限制阀门
     * 
     * @return 返回连接限制阀门
     */
    protected LimitLatch initializeConnectionLatch() {
        if (maxConnections == -1) {
            return null;
        }
        
        if (connectionLimitLatch == null) {
            connectionLimitLatch = new LimitLatch(getMaxConnections());
        }
        return connectionLimitLatch;
    }


    /**
     * 释放连接限制阀门的资源
     */
    private void releaseConnectionLatch() {
        LimitLatch latch = this.connectionLimitLatch;
        if (latch != null) {
            latch.releaseAll();
        }
        connectionLimitLatch = null;
    }


    // ==================================== 连接统计 ====================================

    /**
     * LimitLatch中的连接数+1
     * 
     * @throws InterruptedException 如果等待的线程被中断则抛出此异常
     */
    protected void countUpOrAwaitConnection() throws InterruptedException {
        if (maxConnections == -1) {
            return;
        }

        LimitLatch latch = this.connectionLimitLatch;
        if (latch != null) {
            latch.countUpOrAwait();
        }
    }


    /**
     * LimitLatch中的连接数-1
     *
     * @return 返回当前连接数，如果返回-1则表示没有设置最大连接数或连接限制阀为null
     */
    protected long countDownConnection() {
        if (maxConnections == -1) {
            return -1;
        }

        LimitLatch latch = this.connectionLimitLatch;
        if (latch != null) {
            return latch.countDown();
        }
        
        return -1;
    }


    /**
     * 关闭服务socket
     */
    public final void closeServerSocketGraceful() {
        if (bindState == BindState.BOUND_ON_START) {
            acceptor.stop(-1);
            releaseConnectionLatch();
            
            getHandler().pause();
            
            bindState = BindState.SOCKET_CLOSED_ON_STOP;
            
            try {
                doCloseServerSocket();
            } catch (IOException ioe) {
                System.err.println(ioe + "\n" + "server socket关闭失败！");
            }
        }
    }


    /**
     * 等待关闭客户端连接
     * 
     * @param waitMillis 等待时长
     * @return 返回正真等待了的时长
     */
    public final long awaitConnectionsClose(long waitMillis) {
        while (waitMillis > 0 && !connections.isEmpty()) {
            try {
                Thread.sleep(50);
                waitMillis -= 50;
            } catch (InterruptedException e) {
                Thread.interrupted();
                waitMillis = 0;
            }
        }
        
        return waitMillis;
    }


    protected abstract void doCloseServerSocket() throws IOException;
    protected abstract U serverSocketAccept() throws Exception;
    protected abstract boolean setSocketOptions(U socket);
    protected abstract void destroySocket(U socket);
    

    /**
     * 关闭套接字
     * 
     * @param socket 要关闭的套接字
     */
    protected void closeSocket(U socket) {
        SocketWrapperBase<S> socketWrapper = connections.get(socket);
        if (socketWrapper != null) {
            socketWrapper.close();
        }
    }
    
}
