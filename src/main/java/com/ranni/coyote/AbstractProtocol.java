package com.ranni.coyote;

import com.ranni.util.collections.SynchronizedStack;
import com.ranni.util.net.AbstractEndpoint;
import com.ranni.util.net.SocketEvent;
import com.ranni.util.net.SocketWrapperBase;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/11 15:56
 * @Ref org.apache.coyote.AbstractProtocol
 */
public abstract class AbstractProtocol<S> implements ProtocolHandler {

    // ==================================== 字段属性 ====================================
    
    private int nameIndex = 0;
    
    private final AbstractEndpoint<S, ?> endpoint;
    
    private AbstractEndpoint.Handler<S> handler;

    /**
     * 处于等待状态中的请求处理器
     */
    private final Set<Processor> waitingProcessor = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private ScheduledFuture<?> timeoutFuture = null;
    private ScheduledFuture<?> monitorFuture;

    private int maxHeaderCount = 100;
    protected int processorCache = 200;
    protected Adapter adapter; // 适配器，用于ProtocolHandler和连接器之间的连接
    
    // ==================================== 构造方法 ====================================
    
    public AbstractProtocol(AbstractEndpoint<S,?> endpoint) {
        this.endpoint = endpoint;
        setConnectionLinger(Constants.DEFAULT_CONNECTION_LINGER);
        setTcpNoDelay(Constants.DEFAULT_TCP_NO_DELAY);
    }


    // ==================================== 基本方法 ====================================
    
    @Override
    public void setAdapter(Adapter adapter) { this.adapter = adapter; }
    @Override
    public Adapter getAdapter() { return adapter; }
    
    public int getMaxHeaderCount() {
        return maxHeaderCount;
    }
    public void setMaxHeaderCount(int maxHeaderCount) {
        this.maxHeaderCount = maxHeaderCount;
    }

    public int getProcessorCache() { return this.processorCache; }
    public void setProcessorCache(int processorCache) {
        this.processorCache = processorCache;
    }

    public int getMaxThreads() { return endpoint.getMaxThreads(); }
    public void setMaxThreads(int maxThreads) {
        endpoint.setMaxThreads(maxThreads);
    }

    public int getMaxConnections() { return endpoint.getMaxConnections(); }
    public void setMaxConnections(int maxConnections) {
        endpoint.setMaxConnections(maxConnections);
    }


    public int getMinSpareThreads() { return endpoint.getMinSpareThreads(); }
    public void setMinSpareThreads(int minSpareThreads) {
        endpoint.setMinSpareThreads(minSpareThreads);
    }


    public int getThreadPriority() { return endpoint.getThreadPriority(); }
    public void setThreadPriority(int threadPriority) {
        endpoint.setThreadPriority(threadPriority);
    }


    public int getAcceptCount() { return endpoint.getAcceptCount(); }
    public void setAcceptCount(int acceptCount) { endpoint.setAcceptCount(acceptCount); }


    public boolean getTcpNoDelay() { return endpoint.getTcpNoDelay(); }
    public void setTcpNoDelay(boolean tcpNoDelay) {
        endpoint.setTcpNoDelay(tcpNoDelay);
    }


    public int getConnectionLinger() { return endpoint.getConnectionLinger(); }
    public void setConnectionLinger(int connectionLinger) {
        endpoint.setConnectionLinger(connectionLinger);
    }

    
    public int getKeepAliveTimeout() { return endpoint.getKeepAliveTimeout(); }
    public void setKeepAliveTimeout(int keepAliveTimeout) {
        endpoint.setKeepAliveTimeout(keepAliveTimeout);
    }

    public InetAddress getAddress() { return endpoint.getAddress(); }
    public void setAddress(InetAddress ia) {
        endpoint.setAddress(ia);
    }


    public int getPort() { return endpoint.getPort(); }
    public void setPort(int port) {
        endpoint.setPort(port);
    }


    public int getPortOffset() { return endpoint.getPortOffset(); }
    public void setPortOffset(int portOffset) {
        endpoint.setPortOffset(portOffset);
    }


    public int getPortWithOffset() { return endpoint.getPortWithOffset(); }


    public int getLocalPort() { return endpoint.getLocalPort(); }


    public int getConnectionTimeout() {
        return endpoint.getConnectionTimeout();
    }
    public void setConnectionTimeout(int timeout) {
        endpoint.setConnectionTimeout(timeout);
    }

    public long getConnectionCount() {
        return endpoint.getConnectionCount();
    }


    @Override
    public Executor getExecutor() {
        return endpoint.getExecutor();
    }

    @Override
    public void setExecutor(Executor executor) {
        endpoint.setExecutor(executor);
    }

    @Override
    public boolean isAprRequired() {
        return false;
    }


    @Override
    public boolean isSendfileSupported() {
        return endpoint.getUseSendfile();
    }

    @Override
    public String getId() {
        return endpoint.getId();
    }

    public void setAcceptorThreadPriority(int threadPriority) {
        endpoint.setAcceptorThreadPriority(threadPriority);
    }
    public int getAcceptorThreadPriority() {
        return endpoint.getAcceptorThreadPriority();
    }


    // ==================================== 核心方法 ====================================

    protected AbstractEndpoint<S,?> getEndpoint() {
        return endpoint;
    }
    
    protected AbstractEndpoint.Handler<S> getHandler() {
        return handler;
    }

    protected void setHandler(AbstractEndpoint.Handler<S> handler) {
        this.handler = handler;
    }

    
    /**
     * @return 返回协议前缀，如（Http，Ajp）
     */
    protected abstract String getProtocolName();


    /**
     * @return 返回创建并配置的处理器实例
     */
    protected abstract Processor createProcessor();


    // ==================================== 生命周期方法 ====================================
    
    @Override
    public void init() throws Exception {
        // XXX - 省略的JMX相关配置
        endpoint.init();
    }


    @Override
    public void start() throws Exception {
        endpoint.start();
    }


    @Override
    public void pause() throws Exception {
        endpoint.pause();
    }
    
    
    public boolean isPaused() {
        return endpoint.isPaused();
    }


    @Override
    public void resume() throws Exception {
        endpoint.resume();
    }


    @Override
    public void stop() throws Exception {
        for (Processor processor : waitingProcessor) {
            processor.timeoutAsync(-1);
        }
        
        endpoint.stop();
    }


    @Override
    public void destroy() throws Exception {
        endpoint.destroy();
    }


    @Override
    public void closeServerSocketGraceful() {
        endpoint.closeServerSocketGraceful();
    }


    @Override
    public long awaitConnectionsClose(long waitMillis) {
        return endpoint.awaitConnectionsClose(waitMillis);
    }
    
    
    public void removeWaitingProcessor(Processor processor) {
        waitingProcessor.remove(processor);
    }

    
    public void addWaitingProcessor(Processor processor) {
        waitingProcessor.add(processor);
    }

    // ==================================== 内部类 ====================================

    /**
     * 连接处理器类
     */
    protected static class ConnectionHandler<S> implements AbstractEndpoint.Handler<S> {
        
        private final AbstractProtocol<S> protocol;
        private final AtomicLong registerCount = new AtomicLong(0);
        private final RecycledProcessors recycledProcessors = new RecycledProcessors(this);

        public ConnectionHandler(AbstractProtocol<S> protocol) {
            this.protocol = protocol;
        }


        protected AbstractProtocol<S> getProtocol() {
            return protocol;
        }


        /**
         * 处理socket的各种事件
         * 
         * @param wrapper 套接字包装
         * @param status 处理事件
         * @return 返回socket状态
         */
        @Override
        public SocketState process(SocketWrapperBase<S> wrapper, SocketEvent status) {
            if (wrapper == null) {
                return SocketState.CLOSED;
            }

            // 拿走socket包装类中的处理器，保证不会有多个线程去修改socket状态
            Processor processor = (Processor) wrapper.takeCurrentProcessor();
            
            if (SocketEvent.TIMEOUT == status &&
                (processor == null 
                 || !processor.isAsync() && !processor.isUpgrade()
                 || processor.isAsync() && !processor.checkAsyncTimeoutGeneration())) {
                
                return SocketState.OPEN;
            }

            if (processor != null) {
                getProtocol().removeWaitingProcessor(processor);
            } else if (status == SocketEvent.DISCONNECT || status == SocketEvent.ERROR) {
                return SocketState.CLOSED;
            }
            
            try {
                if (processor == null) {
                    processor = recycledProcessors.pop();                
                }

                if (processor == null) {
                    processor = getProtocol().createProcessor();
                }                
                
                SocketState state;
                do {
                    state = processor.process(wrapper, status);
                    
                    if (state == SocketState.UPGRADING) {
                        // XXX - 暂不支持协议升级，退出
                        break;
                    }
                    
                } while (state == SocketState.UPGRADING);
                
                if (state == SocketState.LONG) {
                    // 长连接
                    longPoll(wrapper, processor);
                    if (processor.isAsync()) {
                        // 处理器异步的，那么加入待处理集合中
                        getProtocol().addWaitingProcessor(processor);
                    }
                } else if (state == SocketState.OPEN) {
                    // keep-alive但是还处于请求状态。可以回收
                    release(processor);
                    processor = null;
                    wrapper.registerReadInterest(); // why - 为什么需要注册读取兴趣
                } else if (state == SocketState.SENDFILE) {
                    // XXX - 文件发送
                } else if (state == SocketState.UPGRADED) {
                    // XXX - 协议升级过
                } else if (state == SocketState.SUSPENDED) {
                    // XXX - 暂停
                } else {
                    // 请求处理完成，连接已经关闭，可以释放资源
                    if (processor != null && processor.isUpgrade()) {
                        // XXX - 对于进行过协议升级的请求暂无处理
                    }
                    
                    release(processor);
                    processor = null;
                }

                if (processor != null) {
                    // 到这儿表示请求没有处理完，归还回去
                    wrapper.setCurrentProcessor(processor);
                }
                
                return state;
            } catch (Throwable e) {
                e.printStackTrace();
            }

            // 到这儿说明出现了异常，释放处理器并关闭socket连接
            release(processor);
            return SocketState.CLOSED;
        }


        /**
         * 请求需要进行长轮询。<br>
         * 因为请求是个升级的连接或者请求行请求头不完整
         * 
         * @param socket socket
         * @param processor 协议处理器
         */
        protected void longPoll(SocketWrapperBase<?> socket, Processor processor) {
            if (!processor.isAsync()) {                
                socket.registerReadInterest();
            }
        }
        

        @Override
        public Object getGlobal() {
            return null;
        }

        
        @Override
        public void release(SocketWrapperBase<S> socketWrapper) {
            Processor processor = (Processor) socketWrapper.takeCurrentProcessor();
            release(processor);
        }

        private void release(Processor processor) {
            if (processor != null) {
                processor.recycle();
                if (processor.isUpgrade()) {
                    // 升级协议，还需要再次执行
                    getProtocol().removeWaitingProcessor(processor);
                } else {
                    // 此次请求处理完成，放入到回收栈中
                    recycledProcessors.push(processor);
                }
            }
        }


        /**
         * 暂停接收端点中所有连接的请求处理器
         */
        @Override
        public final void pause() {
            for (SocketWrapperBase<S> wrapperBase : protocol.getEndpoint().getConnections()) {
                Processor processor = (Processor) wrapperBase.getCurrentProcessor();
                if (processor != null) {
                    processor.pause();
                }
            }
        }


        /**
         * 复用，重置属性值。<br>
         * 此方法仅仅清空了复用请求处理器栈
         */
        @Override
        public void recycle() {
            recycledProcessors.clear();
        }
    }


    /**
     * 回收的处理器
     */
    protected static class RecycledProcessors extends SynchronizedStack<Processor> {
        private final transient ConnectionHandler<?> handler;
        protected final AtomicInteger size = new AtomicInteger(0);

        public RecycledProcessors(ConnectionHandler<?> handler) {
            this.handler = handler;
        }

        @Override
        public boolean push(Processor obj) {
            int cacheSize = handler.getProtocol().getProcessorCache();
            boolean offer = cacheSize == -1 ? true : size.get() < cacheSize;
            boolean res = false;
            
            if (offer) {
                res = super.push(obj);
                if (res) {
                    size.incrementAndGet();
                }
            }
            
            return res;
        }

        @Override
        public Processor pop() {
            Processor res = super.pop();
            if (res != null) {
                size.decrementAndGet();
            }
            return res;
        }

        @Override
        public void clear() {
//            Processor next = null;
//            
//            // 弹出去，保证gc回收掉，clear导致 
//            do {
//                next = pop();
//            } while (next != null);
            
            super.clear();
            size.set(0);
        }
    }
    
    
}
