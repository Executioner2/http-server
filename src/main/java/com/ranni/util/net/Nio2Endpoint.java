package com.ranni.util.net;

import com.ranni.connector.ApplicationBufferHandler;
import com.ranni.util.collections.SynchronizedStack;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.*;

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
     * 是否正在排队处理
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
                    // 继续同意连接，扔进AsynchronousChannelGroup中异步处理
                    serverSocket.accept(null, this);
                } else if (getConnectionCount() < getMaxConnections()) {
                    try {
                        // 连接数+1
                        countUpOrAwaitConnection();
                    } catch (InterruptedException e) {
                        ;
                    }
                    // 继续同意连接，扔进AsynchronousChannelGroup中异步处理
                    serverSocket.accept(null, this);
                } else {
                    // 在一个新线程上重新接受请求
                    getExecutor().execute(this);
                }
                
                // 处理请求，如果返回false则表示处理失败，需要关闭socket
                if (!setSocketOptions(socket)) {
                    closeSocket(socket);
                }
            } else {
                if (isRunning()) {
                    state = AcceptorState.PAUSED;
                }
                // 不处理接收到的这个请求，销毁它
                destroySocket(socket);
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


    /**
     * NIO2 socket包装类
     * TODO:
     * XXX - 文件操作待实现
     */
    public static class Nio2SocketWrapper extends SocketWrapperBase<Nio2Channel> {

        // ==================================== 属性字段 ====================================
        
        private final SynchronizedStack<Nio2Channel> nioChannels;
        
        /**
         * 异步读处理器
         */
        private final CompletionHandler<Integer, ByteBuffer> readCompletionHandler;
        
        /**
         * 是否对读取感兴趣
         */
        private boolean readInterest = false;
        
        /**
         * 读取通知
         */
        private boolean readNotify = false;

        /**
         * 异步写处理器
         */
        private final CompletionHandler<Integer, ByteBuffer> writeCompletionHandler;

        /**
         * 完成处理器 
         */
        private final CompletionHandler<Long, ByteBuffer[]> gatheringWriteCompletionHandler;

        /**
         * 是否对写入感兴趣
         */
        private boolean writeInterest = false;

        /**
         * 写通知
         */
        private boolean writeNotify = false;


        // ==================================== 构造方法 ====================================
        
        public Nio2SocketWrapper(Nio2Channel channel, final Nio2Endpoint endpoint) {
            super(channel, endpoint);
            nioChannels = endpoint.getNioChannels();
            socketBufferHandler = channel.getBufferHandler();
            
            // 创建读取处理器
            this.readCompletionHandler = new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer nBytes, ByteBuffer attachment) {
                    readNotify = false;
                    synchronized (readCompletionHandler) {
                        if (nBytes.intValue() < 0) {
                            failed(new EOFException(), attachment);
                        } else {
                            if (readInterest && !Nio2Endpoint.isInline()) {
                                readNotify = true;
                            } else {
                                // 到这里表示当前要么没有表示过读感兴趣
                                // 要么就是处于内联读取触发此方法的所以
                                // 要释放信号量
                                readPending.release();  
                            }
                            readInterest = false;
                        }
                    }
                    
                    if (readNotify) {
                        // 处理socket读取事件
                        getEndpoint().processSocket(Nio2SocketWrapper.this, SocketEvent.OPEN_READ, false);
                    }
                }
                

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    IOException ioe;
                    
                    if (exc instanceof IOException) {
                        ioe = (IOException) exc;
                    } else {
                        ioe = new IOException(exc);
                    }
                    setError(ioe);
                    if (exc instanceof AsynchronousCloseException) {
                        readPending.release();
                        getEndpoint().processSocket(Nio2SocketWrapper.this, SocketEvent.STOP, false);
                    } else if (!getEndpoint().processSocket(Nio2SocketWrapper.this, SocketEvent.ERROR, true)) {
                        close();
                    }
                }
            }; // this.readCompletionHandler赋值end
            
            // 创建写入处理器
            this.writeCompletionHandler = new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer nBytes, ByteBuffer attachment) {
                    writeNotify = false;
                    boolean notify = false;
                    
                    if (nBytes.intValue() < 0) {
                        failed(new EOFException("写入失败"), attachment);
                    } else if (!nonBlockingWriteBuffer.isEmpty()) {
                        ByteBuffer[] array = nonBlockingWriteBuffer.toArray(attachment);
                        getSocket().write(array, 0, array.length,
                                toTimeout(getWriteTimeout()), TimeUnit.MILLISECONDS,
                                array, gatheringWriteCompletionHandler);
                    } else if (attachment.hasRemaining()) {
                        getSocket().write(attachment, toTimeout(getWriteTimeout()),
                                TimeUnit.MILLISECONDS, attachment, writeCompletionHandler);
                    } else {
                        if (writeInterest && !Nio2Endpoint.isInline()) {
                            writeNotify = true;
                            notify = true;
                        } else {
                            writePending.release();
                        }
                        writeInterest = false;
                    }
                    
                    if (notify) {
                        if (!endpoint.processSocket(Nio2SocketWrapper.this, SocketEvent.OPEN_WRITE, true)) {
                            close();   
                        }
                    }
                }

                    
                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    IOException ioe;
                    if (exc instanceof IOException) {
                        ioe = (IOException) exc;
                    } else {
                        ioe = new IOException(exc);
                    }
                    
                    setError(ioe);
                    writePending.release();
                    if (!endpoint.processSocket(Nio2SocketWrapper.this, SocketEvent.ERROR, true)) {
                        close();
                    }
                }
            }; // this.writeCompletionHandler赋值end
            
            // 创建写入处理器
            this.gatheringWriteCompletionHandler = new CompletionHandler<Long, ByteBuffer[]>() {
                @Override
                public void completed(Long nBytes, ByteBuffer[] attachment) {
                    writeNotify = false;
                    boolean notify = false;
                    
                    synchronized (writeCompletionHandler) {
                        if (nBytes.longValue() < 0) {
                            failed(new EOFException("写入失败"), attachment);
                        } else if (!nonBlockingWriteBuffer.isEmpty() || buffersArrayHasRemaining(attachment, 0, attachment.length)) {
                            ByteBuffer[] array = nonBlockingWriteBuffer.toArray(attachment);
                            getSocket().write(array, 0, array.length, 
                                    toTimeout(getWriteTimeout()), TimeUnit.MILLISECONDS, 
                                    array, gatheringWriteCompletionHandler);
                        } else {
                            if (writeInterest && !Nio2Endpoint.isInline()) {
                                writeNotify = true;
                                notify = true;
                            } else {
                                writePending.release();
                            }
                            writeInterest = false;
                        }
                    }
                    
                    if (notify) {
                        if (!endpoint.processSocket(Nio2SocketWrapper.this, SocketEvent.OPEN_WRITE, true)) {
                            close();
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer[] attachment) {
                    IOException ioe;
                    if (exc instanceof IOException) {
                        ioe = (IOException) exc;
                    } else {
                        ioe = new IOException(exc);
                    }
                    
                    setError(ioe);
                    writePending.release();
                    if (!endpoint.processSocket(Nio2SocketWrapper.this, SocketEvent.ERROR, true)) {
                        close();
                    }
                }
            }; // this.gatheringWriteCompletionHandler赋值end
            
        }


        // ==================================== 核心方法 ====================================

        /**
         * 注册写入兴趣，表示想要写入数据<br>
         * 如果没有写入挂起，那直接触发写入事件
         */
        @Override
        public void registerWriteInterest() {
            synchronized (writeCompletionHandler) {
                // 已经写入了数据并发送了通知
                if (writeNotify) {
                    return;
                }
                writeInterest = true;
                if (writePending.availablePermits() == 1) {
                    // 没有写入挂起，当前可以直接写入
                    if (!getEndpoint().processSocket(this, SocketEvent.OPEN_WRITE, true)) {
                        close();
                    }
                }
            }
        }


        /**
         * 注册读取兴趣，表示想要读取数据
         */
        @Override
        public void registerReadInterest() {
            synchronized (readCompletionHandler) {
                // 已经读取了数据并发送了通知
                if (readNotify) {
                    return;
                }
                
                readInterest = true;
                if (readPending.tryAcquire()) {
                    // 可以当场读取
                    try {
                        if (fillReadBuffer(false) > 0) {
                            if (!getEndpoint().processSocket(this, SocketEvent.OPEN_READ, true)) {
                                close();
                            }
                        }
                    } catch (IOException e) {
                        setError(e);
                    }
                }
            }
        }


        /**
         * 是否准备好了读取数据。
         * 
         * @return 如果返回<b>true</b>，则表示可以读取数据了
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        public boolean isReadyForRead() throws IOException {
            synchronized (readCompletionHandler) {
                // 已经准备好读取数据了
                if (readNotify) {
                    return true;
                }
                
                // 已经表示了对读感兴趣，但是读操作处于挂起状态
                if (!readPending.tryAcquire()) {
                    readInterest = true;
                    return false;
                }
                
                // 可以直接从缓冲区中读取
                if (!socketBufferHandler.isReadBufferEmpty()) {
                    readPending.release();
                    return true;
                }
                
                // 尝试读一些数据
                boolean isReady = fillReadBuffer(false) > 0;
                if (!isReady) {
                    // 没有读到数据，告诉线程对读感兴趣
                    readInterest = true;
                }
                return isReady;
            }
        }
        

        @Override
        public void setAppReadBufHandler(ApplicationBufferHandler handler) {
            getSocket().setAppReadBufHandler(handler);
        }

        @Override
        public int read(boolean block, byte[] b, int off, int len) throws IOException {
            checkError();

            if (socketBufferHandler == null) {
                throw new IOException("socket.closed");
            }
            
            if (!readNotify) {
                if (block) {
                    try {
                        // 阻塞式取得读取信号量
                        readPending.acquire();
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                } else {
                    if (!readPending.tryAcquire()) {
                        return 0;
                    }
                }
            }
            
            int nRead = populateReadBuffer(b, off, len);
            if (nRead > 0) {
                // 数据已经从socket缓冲区处理器的读缓冲区中读取出来了
                // 因此不需要再通知取数据
                readNotify = false;
                readPending.release(); // 释放信号量
                return nRead;
            }

            // 走到这儿说明socket缓冲区处理器的读缓冲区中没有数据
            synchronized (readCompletionHandler) {
                nRead = fillReadBuffer(block);
                
                if (nRead > 0) {
                    socketBufferHandler.configureReadBufferForRead();
                    nRead = Math.min(nRead, len);
                    socketBufferHandler.getReadBuffer().get(b, off, len);
                } else if (nRead == 0 && !block){
                    // 异步读，注册读取兴趣
                    readInterest = true;
                }
                
                return nRead;
            }
        }

        @Override
        public int read(boolean block, ByteBuffer byteBuffer) throws IOException {
            checkError();
            
            if (socketBufferHandler == null) {
                throw new IOException("socket.closed");
            }
            
            if (!readNotify) {
                if (block) {
                    try {
                        readPending.acquire();
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                } else {
                    if (!readPending.tryAcquire()) {
                        return 0;
                    }
                }
            }
            
            int nRead = populateReadBuffer(byteBuffer);
            if (nRead > 0) {
                // 数据已经从socket缓冲区处理器的读缓冲区中读取出来了
                // 因此不需要再通知取数据
                readNotify = false;
                readPending.release(); // 释放信号量
                return nRead;
            }
            
            // 走到这儿说明socket缓冲区处理器的读缓冲区中没有数据
            synchronized (readCompletionHandler) {
                int limit = socketBufferHandler.getReadBuffer().capacity();
                if (block && byteBuffer.remaining() >= limit) {
                    // 直接读到byteBuffer中
                    byteBuffer.limit(byteBuffer.position() + limit);
                    nRead = fillReadBuffer(block, byteBuffer);
                } else {
                    // 尝试读到socket缓冲区处理器的读缓冲区中再转入到byteBuffer
                    nRead = fillReadBuffer(block);
                    if (nRead > 0) {
                        nRead = populateReadBuffer(byteBuffer);
                    } else if (nRead == 0 && !block) {
                        readInterest = true;
                    }                    
                }
                
                return nRead;
            }
            
        }

        @Override
        protected void populateRemoteHost() {

        }

        @Override
        protected void populateRemoteAddr() {

        }

        @Override
        protected void populateRemotePort() {

        }

        @Override
        protected void populateLocalName() {

        }

        @Override
        protected void populateLocalAddr() {

        }

        @Override
        protected void populateLocalPort() {

        }

        @Override
        protected void doClose() {

        }

        @Override
        protected void doWrite(boolean block, ByteBuffer from) throws IOException {

        }

        /**
         * 尝试读取数据，读取到socket缓冲区处理器的读缓冲区中 
         *
         * @param block 是否阻塞读
         * @return 如果返回<b>true</b>，则表示读取到了数据
         */
        private int fillReadBuffer(boolean block) throws IOException {
            socketBufferHandler.configureReadBufferForWrite();
            return fillReadBuffer(block, socketBufferHandler.getReadBuffer());
        }


        /**
         * 尝试读取数据
         *
         * @param block 是否阻塞读
         * @param buffer 要将读取数据存放到的缓冲区
         * @return 如果返回<b>true</b>，则表示读取到了数据
         */
        private int fillReadBuffer(boolean block, ByteBuffer buffer) throws IOException {
            int nRead = 0;
            Future<Integer> integer = null; // 异步处理
            
            if (block) {
                try {
                    integer = getSocket().read(buffer);
                    long timeout = getReadTimeout();
                    if (timeout > 0) {
                        nRead = integer.get(timeout, TimeUnit.MILLISECONDS).intValue();
                    } else {
                        nRead = integer.get().intValue();
                    }
                } catch (InterruptedException e) {
                    throw new IOException(e);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    } else {
                        throw new IOException(e);
                    }
                } catch (TimeoutException e) {
                    integer.cancel(true);
                    e.printStackTrace();
                } finally {
                    readPending.release();
                }
            } else {
                // 内联的调用socket数据读取
                // 此处设定了内联调用，因此转到readCompletionHandler的completed方法
                // 时将会清除读信号量
                Nio2Endpoint.startInline(); 
                getSocket().read(buffer, toTimeout(getReadTimeout()), TimeUnit.MILLISECONDS,
                        buffer, readCompletionHandler);
                Nio2Endpoint.endInline();
                if (readPending.availablePermits() == 1) {
                    // 因为传入了readCompletionHandler，在readCompletionHandler
                    // 中必定是内联调用读取
                    nRead = buffer.position();
                }
            }
            
            return nRead;
        }
        
    }


    // ==================================== 核心方法 ====================================

    /**
     * @return 如果返回<b>true</b>，则表示当前任务正在排队处理
     */
    private static boolean isInline() {
        Boolean flag = inlineCompletion.get();
        if (flag == null) {
            return false;
        }
        return flag.booleanValue();
    }
    

    /**
     * 结束排队处理
     */
    private static void endInline() {
        inlineCompletion.set(Boolean.FALSE);
    }


    /**
     * 开始排队处理
     */
    private static void startInline() {
        inlineCompletion.set(Boolean.TRUE);
    }


    @Override
    protected InetSocketAddress getLocalAddress() throws IOException {
        return null;
    }

    @Override
    protected SocketProcessorBase<Nio2Channel> createSocketProcessor(SocketWrapperBase<Nio2Channel> socketWrapper, SocketEvent event) {
        return null;
    }


    /**
     * @return 返回socket通信通道集合
     */
    protected SynchronizedStack<Nio2Channel> getNioChannels() {
        return nioChannels;
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
            acceptor.setThreadName(getName() + "-Acceptor");
        }
        acceptor.state = Acceptor.AcceptorState.RUNNING;
        getExecutor().execute(acceptor);
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


    /**
     * 为请求分配处理的容器
     * 
     * @param socket 要处理的socket通道
     * @return 如果返回<b>true</b>，则表示处理成功
     */
    @Override
    protected boolean setSocketOptions(AsynchronousSocketChannel socket) {
        
        return false;
    }

    @Override
    protected void destroySocket(AsynchronousSocketChannel socket) {

    }
}
