package com.ranni.util.net;

import com.ranni.connector.ApplicationBufferHandler;
import com.ranni.util.collections.SynchronizedStack;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
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
     * 文件数据类
     */
    public static class SendfileData extends SendfileDataBase {
        private FileChannel fchannel;
        // 内部使用
        private boolean doneInline = false;
        private boolean error = false;

        public SendfileData(String filename, long pos, long length) {
            super(filename, pos, length);
        }
    }
    
    
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
         * 停止接收器。仅仅更改Acceptor状态为停止状态
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
     * 
     * TODO:
     * XXX - 文件操作待实现
     */
    public static class Nio2SocketWrapper extends SocketWrapperBase<Nio2Channel> {

        // ==================================== 属性字段 ====================================
        
        private final SynchronizedStack<Nio2Channel> nioChannels;
        
        /**
         * 读处理器
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
         * 写处理器，主要用于写入单个缓冲区，如果有多个缓冲区，那么将会转到 
         * {@link #gatheringWriteCompletionHandler} 处理器中处理
         */
        private final CompletionHandler<Integer, ByteBuffer> writeCompletionHandler;

        /**
         * 写处理器，主要用于写入多个缓冲区
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
                        // 处理socket读取事件，同步执行
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
            
            // 创建写入处理器，此处理器主要用于写入单个字节缓冲区
            this.writeCompletionHandler = new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer nBytes, ByteBuffer attachment) {
                    writeNotify = false;
                    boolean notify = false;
                    
                    if (nBytes.intValue() < 0) {
                        failed(new EOFException("写入失败"), attachment);
                    } else if (!nonBlockingWriteBuffer.isEmpty()) {
                        // 有多个缓冲数据，整理一下然后转到多缓冲区写入处理器做处理
                        ByteBuffer[] array = nonBlockingWriteBuffer.toArray(attachment);
                        getSocket().write(array, 0, array.length,
                                toTimeout(getWriteTimeout()), TimeUnit.MILLISECONDS,
                                array, gatheringWriteCompletionHandler);
                    } else if (attachment.hasRemaining()) {
                        // 只有一个缓冲区的数据，写入并传递写入处理器为自身
                        // 保证一直写入到缓冲区没有数据可写为止
                        getSocket().write(attachment, toTimeout(getWriteTimeout()),
                                TimeUnit.MILLISECONDS, attachment, writeCompletionHandler);
                    } else {
                        // 已经没有数据可写了
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
            
            // 创建写入处理器，此处理器用于写入多个字节缓冲区            
            this.gatheringWriteCompletionHandler = new CompletionHandler<Long, ByteBuffer[]>() {
                @Override
                public void completed(Long nBytes, ByteBuffer[] attachment) {
                    writeNotify = false;
                    boolean notify = false;

                    // 写入处理器写入数据会不断写入，直到缓冲区中没有数据为止
                    // 可以理解为递归，因为写入的时候传入的写入处理器为自身
                    synchronized (writeCompletionHandler) {
                        if (nBytes.longValue() < 0) {
                            failed(new EOFException("写入失败"), attachment);
                        } else if (!nonBlockingWriteBuffer.isEmpty() || buffersArrayHasRemaining(attachment, 0, attachment.length)) {
                            ByteBuffer[] array = nonBlockingWriteBuffer.toArray(attachment);
                            // 传入了gatheringWriteCompletionHandler自身作为写处理器不断
                            // 写入数据，直到缓冲区数据为空（不满足进入此代码块的条件）
                            getSocket().write(array, 0, array.length, 
                                    toTimeout(getWriteTimeout()), TimeUnit.MILLISECONDS, 
                                    array, gatheringWriteCompletionHandler);
                        } else {
                            // 已经没有数据可写了
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


        /**
         * @return 如果返回<b>true</b>，则表示缓冲区可以写入数据
         */
        @Override
        public boolean isReadyForWrite() {
            synchronized (writeCompletionHandler) {
                if (writeNotify) {
                    return true;
                }
                
                if (!writePending.tryAcquire()) {
                    writeInterest = true;
                    return false;
                }
                
                if (socketBufferHandler.isWriteBufferEmpty() && nonBlockingWriteBuffer.isEmpty()) {
                    writePending.release();
                    return true;
                }
                
                boolean isReady = !flushNonBlockingInternal(true);
                if (!isReady) {
                    writeInterest = true;
                }
                return isReady;
            }
        }
        

        @Override
        public void setAppReadBufHandler(ApplicationBufferHandler handler) {
            getSocket().setAppReadBufHandler(handler);
        }

        @Override
        public boolean hasAsyncIO() {
            return getEndpoint().getUseAsyncIO();
        }
        
        @Override
        public boolean needSemaphores() {
            return true;
        }

        @Override
        public boolean hasPerOperationTimeout() {
            return true;
        }


        /**
         * 非阻塞式发送数据
         * 
         * @param buf 要发送的数据
         * @param off 偏移量
         * @param len 数据长度
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        protected void writeNonBlocking(byte[] buf, int off, int len) throws IOException {
            synchronized (writeCompletionHandler) {
                checkError();
                if (writeNotify || writePending.tryAcquire()) {
                    socketBufferHandler.configureWriteBufferForWrite();
                    int nLen = transfer(buf, off, len, socketBufferHandler.getWriteBuffer());
                    len = len - nLen;
                    off = off + nLen;
                    if (len > 0) {
                        // 还有数据要写，加入到异步待写入数据队列中
                        nonBlockingWriteBuffer.add(buf, off, len);
                    }
                    // 发送出去
                    flushNonBlockingInternal(true);
                } else {
                    // 没有写入权限，加入到异步待写入队列中
                    nonBlockingWriteBuffer.add(buf, off, len);
                }
            }
        }


        /**
         * 非阻塞式发送数据<br>
         * 直接调用的 {@link #writeNonBlockingInternal(ByteBuffer)}
         * 
         * @param buf 要写入的数据
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        protected void writeNonBlocking(ByteBuffer buf) throws IOException {
            writeNonBlockingInternal(buf);
        }


        /**
         * 非阻塞式发送数据
         * 
         * @param buf 要发送的数据
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        protected void writeNonBlockingInternal(ByteBuffer buf) throws IOException {
            synchronized (writeCompletionHandler) {
                checkError();
                if (writeNotify || writePending.tryAcquire()) {
                    socketBufferHandler.configureWriteBufferForWrite();
                    transfer(buf, socketBufferHandler.getWriteBuffer());
                    if (buf.remaining() > 0) {
                        // 还有数据要写，加入到异步待写入数据队列中
                        nonBlockingWriteBuffer.add(buf);
                    }
                    // 发送出去
                    flushNonBlockingInternal(true);
                } else {
                    // 没有写入权限，加入到异步待写入队列中
                    nonBlockingWriteBuffer.add(buf);
                }
            }
        }
        

        /**
         * 实现将socket缓冲区处理器读缓冲区的数据读取到目标字节数组中<br>
         * 在读取数据之前必须先获取读信号量，是否阻塞式获取读信号量根据
         * 传入的block参数决定。如果socket缓冲区处理器的读缓冲区没有
         * 数据，尝试从底层socket缓冲区中取得。如果socket缓冲区中还
         * 没有数据，同时该方法调用者表示愿意非阻塞式读取数据，那么就
         * 注册读感兴趣。
         * 
         * @param block 如果为<b>true</b>，表示调用者使用阻塞的方式读，否则反之
         * @param b 需要存入到的字节数组
         * @param off 存入到的字节数组的偏移量
         * @param len 存入的数据长度
         * @return 返回实际存入的数据长度
         * @throws IOException 可能抛出I/O异常
         */
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


        /**
         * 实现将socket缓冲区处理器读缓冲区的数据读取到目标缓冲区中<br>
         * 在读取数据之前必须先获取读信号量，是否阻塞式获取读信号量根据
         * 传入的block参数决定。如果socket缓冲区处理器的读缓冲区没有
         * 数据，尝试从底层socket缓冲区中取得。如果socket缓冲区中还
         * 没有数据，同时该方法调用者表示愿意非阻塞式读取数据，那么就
         * 注册读感兴趣。
         * 
         * @param block 如果为<b>true</b>，表示调用者使用阻塞的方式读，否则反之
         * @param byteBuffer 要存入到的目标缓冲区
         * @return 返回存入的数据量
         * @throws IOException 可能抛出I/O异常
         */
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


        /**
         * 取得远程主机名和远程主机地址并存入到实例属性中
         */
        @Override
        protected void populateRemoteHost() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getRemoteAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                if (socketAddress instanceof InetSocketAddress) {
                    remoteHost = ((InetSocketAddress) socketAddress).getAddress().getHostName();
                    if (remoteAddr == null) {
                        remoteAddr = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
                    }
                }
            }
        }


        /**
         * 取得远程主机地址并存入到实例属性中
         */
        @Override
        protected void populateRemoteAddr() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getRemoteAddress();
                } catch (IOException e) {
                    ;
                }
                
                if (socketAddress instanceof InetSocketAddress) {
                    remoteAddr = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
                }
            }
        }


        /**
         * 取得远程主机端口号并存入到实例属性中
         */
        @Override
        protected void populateRemotePort() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getRemoteAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (socketAddress instanceof InetSocketAddress) {
                    remotePort = ((InetSocketAddress) socketAddress).getPort();
                }
            }
        }


        /**
         * 取得接收请求的服务器名并存入到实例属性中
         */
        @Override
        protected void populateLocalName() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getLocalAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (socketAddress instanceof InetSocketAddress) {
                    localName = ((InetSocketAddress) socketAddress).getHostName();
                }
            }
        }


        /**
         * 取得接收请求的服务器地址并存入到实例属性中
         */
        @Override
        protected void populateLocalAddr() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getLocalAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (socketAddress instanceof InetSocketAddress) {
                    localAddr = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
                }
            }
        }


        /**
         * 取得接收请求的服务器端口号并存入到实例属性中
         */
        @Override
        protected void populateLocalPort() {
            AsynchronousSocketChannel sc = getSocket().getIOChannel();
            if (sc != null) {
                SocketAddress socketAddress = null;
                try {
                    socketAddress= sc.getLocalAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (socketAddress instanceof InetSocketAddress) {
                    localPort = ((InetSocketAddress) socketAddress).getPort();
                }
            }
        }


        /**
         * 关闭通信。将会关闭相同信道下所有socket
         */
        @Override
        protected void doClose() {
            try {
                getEndpoint().connections.remove(getSocket().getIOChannel());
                if (getSocket().isOpen()) {
                    getSocket().close(true);
                }
                if (getEndpoint().running) {
                    if (nioChannels == null || !nioChannels.push(getSocket())) {
                        // 无法加入到信道缓存中复用，释放分配的缓冲区空间
                        getSocket().free();
                    }
                }                
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                socketBufferHandler = SocketBufferHandler.EMPTY;
                nonBlockingWriteBuffer.clear();
                reset(Nio2Channel.CLOSED_NIO2_CHANNEL);
            }
            
            // TODO - 关闭文件发送信道
        }


        /**
         * 把缓冲区中的数据全部发送出去
         * 
         * @param block 如果为<b>true</b>，则表示以阻塞方式写入数据，否则反之
         * @param from 需要发送的数据
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        protected void doWrite(boolean block, ByteBuffer from) throws IOException {
            Future<Integer> integer = null;
            try {
                do {
                    integer = getSocket().write(from);
                    long timeout = getReadTimeout();
                    if (timeout > 0) {
                        if (integer.get(timeout, TimeUnit.MILLISECONDS).intValue() < 0) {
                            throw new EOFException("写入数据到socket channel失败");
                        }
                    } else {
                        if (integer.get().intValue() < 0) {
                            throw new EOFException("写入数据到socket channel失败");
                        }
                    }
                } while (from.hasRemaining());
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
                throw new SocketTimeoutException();
            }
        }


        @Override
        public SendfileDataBase createSendfileData(String filename, long pos, long length) {
            return new SendfileData(filename, pos, length);
        }

        
        @Override
        public SendfileState processSendfile(SendfileDataBase sendfileData) {
            return null;
        }


        @Override
        protected void flushBlocking() throws IOException {
            checkError();
            
            // 取得写入信号量，保证数据发送的顺序性
            try {
                if (writePending.tryAcquire(toTimeout(getWriteTimeout()), TimeUnit.MILLISECONDS)) {
                    writePending.release();
                } else {
                    throw new SocketTimeoutException();
                }
            } catch (InterruptedException e) {
                ;
            }
            
            super.flushBlocking();
        }


        @Override
        protected boolean flushNonBlocking() throws IOException {
            checkError();
            return flushNonBlockingInternal(false);
        }


        /**
         * 将数据发送出去
         * 
         * @param hasPermit 如果为<b>true</b>，则表示可以直接尝试发送数据
         * @return 如果返回<b>true</b>，则表示还有数据没被发送出去
         */
        private boolean flushNonBlockingInternal(boolean hasPermit) {
            synchronized (writeCompletionHandler) {
                if (writeNotify || hasPermit || writePending.tryAcquire()) {
                    // 被通知的代码正在此出执行写入操作
                    writeNotify = false;
                    socketBufferHandler.configureWriteBufferForRead();
                    if (!nonBlockingWriteBuffer.isEmpty()) {
                        ByteBuffer[] array = nonBlockingWriteBuffer.toArray(socketBufferHandler.getReadBuffer());
                        Nio2Endpoint.startInline();
                        getSocket().write(array, 0, array.length, toTimeout(getWriteTimeout()), 
                                TimeUnit.MILLISECONDS, array, gatheringWriteCompletionHandler);
                        Nio2Endpoint.endInline();
                    } else if (socketBufferHandler.getWriteBuffer().hasRemaining()) {
                        Nio2Endpoint.startInline();
                        getSocket().write(socketBufferHandler.getWriteBuffer(), toTimeout(getWriteTimeout()), 
                                TimeUnit.MILLISECONDS, socketBufferHandler.getWriteBuffer(), writeCompletionHandler);
                        Nio2Endpoint.endInline();
                    } else {
                        if (!hasPermit) {
                            writePending.release();
                        }
                        writeInterest = false;
                    }
                }
                
                return hasDataToWrite();
            }
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
                // 此处设定了内联调用，因此转到readCompletionHandler
                // 的completed方法时将会清除读信号量
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


        /**
         * 是否有数据待取出
         * 
         * @return 如果返回<b>true</b>，则表示有数据待取出
         */
        @Override
        public boolean hasDataToRead() {
            synchronized (readCompletionHandler) {
                return !socketBufferHandler.isReadBufferEmpty()
                        || readNotify || getError() != null; // 错误也算有数据待取
            }
        }


        /**
         * 是否有数据待发送
         * 
         * @return 如果返回<b>true</b>，则表示有数据待发送
         */
        @Override
        public boolean hasDataToWrite() {
            synchronized (writeCompletionHandler) {
                return !socketBufferHandler.isWriteBufferEmpty() || !nonBlockingWriteBuffer.isEmpty()
                        || writeNotify || writePending.availablePermits() == 0 || getError() != null;
            }
        }


        /**
         * @return 如果返回<b>true</b>，则表示读取正在挂起
         */
        @Override
        public boolean isReadPending() {
            synchronized (readCompletionHandler) {
                return readPending.availablePermits() == 0;
            }
        }


        /**
         * @return 如果返回<b>true</b>，则表示写入正在挂起
         */
        @Override
        public boolean isWritePending() {
            synchronized (readCompletionHandler) {
                return writePending.availablePermits() == 0;
            }
        }
        
        // TODO - 文件发送
    }


    /**
     * socket处理器类，主要就实现doRun()这个方法。在处理器中对请求选择要交付的容器
     */
    protected class SocketProcessor extends SocketProcessorBase<Nio2Channel> {
        
        public SocketProcessor(SocketWrapperBase<Nio2Channel> socketWrapper, SocketEvent event) {
            super(socketWrapper, event);
        }

        
        @Override
        protected void doRun() {
            boolean launch = false;
            try {
                int handshake = -1;

                try {
                    if (socketWrapper.getSocket().isHandshakeComplete()) {
                        // 完成SSL层的握手
                        handshake = 0;
                    } else if (event == SocketEvent.STOP || event == SocketEvent.DISCONNECT
                            || event == SocketEvent.ERROR) {
                        // socket非正常通信事件，视为握手失败
                        handshake = -1;
                    } else {
                        handshake = socketWrapper.getSocket().handshake();
                        event = SocketEvent.OPEN_READ;
                    }
                } catch (IOException e) {
                    handshake = -1;
                    e.printStackTrace();
                }

                if (handshake == 0) {
                    Handler.SocketState state = Handler.SocketState.OPEN;
                    if (event == null) {
                        state = getHandler().process(socketWrapper, SocketEvent.OPEN_READ);
                    } else {
                        state = getHandler().process(socketWrapper, event);
                    }

                    if (state == Handler.SocketState.CLOSED) {
                        socketWrapper.close();
                    } else if (state == Handler.SocketState.UPGRADING) {
                        // why - 可能是需要再次执行
                        launch = true;
                    }

                } else if (handshake == -1) {
                    getHandler().process(socketWrapper, SocketEvent.CONNECT_FAIL);
                    socketWrapper.close();
                }
            } catch (Throwable t) {
                if (socketWrapper != null) {
                    socketWrapper.close();
                }
            } finally {
                if (launch) {
                    try {
                        getExecutor().execute(new SocketProcessor(socketWrapper, SocketEvent.OPEN_READ));
                    } catch (NullPointerException npe) {
                        if (running) {
                            npe.printStackTrace();
                        }
                    }
                }
                
                socketWrapper = null;
                event = null;
                if (running && processorCache != null) {
                    processorCache.push(this);
                }
            }
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


    /**
     * @return 返回server socket channel
     */
    protected NetworkChannel getServerSocket() {
        return serverSocket;
    }


    /**
     * 创建socket处理器
     * 
     * @param socketWrapper socket包装实例
     * @param event 事件
     * @return 返回socket处理器
     */
    @Override
    protected SocketProcessorBase<Nio2Channel> createSocketProcessor(SocketWrapperBase<Nio2Channel> socketWrapper, SocketEvent event) {
        return new SocketProcessor(socketWrapper, event);
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


    /**
     * 释放NIO内存池，并关闭server socket
     * 
     * @throws Exception 可能抛出异常
     */
    @Override
    public void unbind() throws Exception {
        if (running) {
            // stop()中会再调用unbind()
            stop();
        }
        doCloseServerSocket();
        shutdownExecutor();
        if (getHandler() != null) {
            getHandler().recycle();
        }
    }


    /**
     * 关闭线程池
     */
    @Override
    public void shutdownExecutor() {
        if (threadGroup != null && internalExecutor) {
            try {
                long timeout = getExecutorTerminationTimeoutMillis();
                while (timeout > 0 && !allClosed) {
                    timeout -= 1;
                    Thread.sleep(1);
                }
                
                threadGroup.shutdownNow();
                if (timeout > 0) {
                    threadGroup.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                ;
            }
            
            if (!threadGroup.isTerminated()) {
                System.out.println("通信信道线程组已终止！"); // XXX - sout
            }
            threadGroup = null;
        }
        
        super.shutdownExecutor();
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


    /**
     * 停止此端点。但不会关闭线程池<br>
     * 执行的操作：
     * <ol>
     *     <li>线程池执行一个关闭所有socket的线程</li>
     *     <li>释放所有信道的内存空间</li>
     *     <li>清空处理器缓存</li>
     * </ol>
     * 
     * @throws Exception 可能抛出异常
     */
    @Override
    public void stopInternal() throws Exception {
        if (!paused) {
            pause();
        }
        
        if (running) {
            running = false;
            acceptor.stop(10); 
            
            // 线程池执行一个关闭所有socket的线程
            getExecutor().execute(() -> {
                try {
                    for (SocketWrapperBase<Nio2Channel> wrapper : getConnections()) {
                        wrapper.close();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    allClosed = true;
                }
            });

            // 释放所有信道的内存空间
            if (nioChannels != null) {
                Nio2Channel socket;
                while ((socket = nioChannels.pop()) != null) {
                    socket.free();
                }
                nioChannels = null;
            }
            
            // 清空处理器缓存
            if (processorCache != null) {
                processorCache.clear();
                processorCache = null;
            }
        }
    }


    /**
     * 关闭server socket
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected void doCloseServerSocket() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
    }


    /**
     * 接收请求
     * 
     * @return 返回同意连接的socket通信信道
     * @throws Exception 可能抛出异常
     */
    @Override
    protected AsynchronousSocketChannel serverSocketAccept() throws Exception {
        AsynchronousSocketChannel channel = serverSocket.accept().get();
        return channel;
    }


    /**
     * 为请求分配处理的信道，为信道创建一个socket缓冲区处理
     * 器。占用接收器线程执行一个读取事件
     * 
     * @param socket 要处理的socket通道
     * @return 如果返回<b>true</b>，则表示处理成功
     */
    @Override
    protected boolean setSocketOptions(AsynchronousSocketChannel socket) {
        Nio2SocketWrapper socketWrapper = null;
        
        try {
            Nio2Channel channel = null;
            if (nioChannels != null) {
                // 信道复用
                channel = nioChannels.pop();
            }
            
            if (channel == null) {
                // 没有可复用的信道，创建一个新的
                SocketBufferHandler sbh = new SocketBufferHandler(
                        socketProperties.getAppReadBufSize(),
                        socketProperties.getAppWriteBufSize(),
                        socketProperties.getDirectBuffer()
                );
                
                // XXX - 没有SSL
                channel = new Nio2Channel(sbh);
            }

            Nio2SocketWrapper newWrapper = new Nio2SocketWrapper(channel, this);
            channel.reset(socket, newWrapper);
            socketWrapper = newWrapper;
            
            // 设置socket属性
            socketProperties.setProperties(socket);
            
            socketWrapper.setReadTimeout(getConnectionTimeout());
            socketWrapper.setWriteTimeout(getConnectionTimeout());
            socketWrapper.setKeepAliveLeft(Nio2Endpoint.this.getMaxKeepAliveRequests());
            
            // 占用接收器线程往下执行
            return processSocket(socketWrapper, SocketEvent.OPEN_READ, false);
            
        } catch (Throwable t) {
            t.printStackTrace();
            if (socketWrapper == null) {
                destroySocket(socket);
            }
        }

        return false;
    }


    /**
     * 销毁socket。进行连接数-1并且关闭socket的操作
     * 
     * @param socket 要关闭的socket
     */
    @Override
    protected void destroySocket(AsynchronousSocketChannel socket) {
        countDownConnection();
        try {
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
