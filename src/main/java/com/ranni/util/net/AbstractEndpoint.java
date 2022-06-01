package com.ranni.util.net;

import com.ranni.util.collections.SynchronizedStack;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Title: HttpServer
 * Description:
 * 抽象的接收端点
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/31 11:56
 * @Ref org.apache.tomcat.util.net.AbstractEndpoint
 */
public abstract class AbstractEndpoint<S, U> {

    // ==================================== 属性字段 ====================================
        

    /**
     * 是否使用内部执行器（executor为null时此字段为true）
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
     * 接收器
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
     * 保存相同套接字的连接
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
     * 多线程执行器
     */
    private Executor executor;

    /**
     * 线程池
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
     * keep-alive属性连接的超时时长，如果未设置则为soTimeout
     */
    private Integer keepAliveTimeout = null;

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
     * 线程池的名称
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

    protected abstract InetSocketAddress getLocalAddress() throws IOException;


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

    public boolean isRunning() {
        return running;
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

    public Handler<S> getHandler() {
        return handler;
    }
    
    public void setHandler(Handler<S> handler) {
        this.handler = handler;
    }


    // ==================================== 核心方法 ====================================

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
     * 连接数量加1
     * 
     * @return 返回当前连接数
     */
    protected long countDownConnection() {
        return 0;
    }


    /**
     * 处理特定状态的SocketWrapper
     * 
     * @param eSocketWrapperBase
     * @param socketStatus
     * @param dispatch
     * @return
     */
    public boolean processSocket(SocketWrapperBase<S> eSocketWrapperBase, SocketEvent socketStatus, boolean dispatch) {
        return false;
    }
}
