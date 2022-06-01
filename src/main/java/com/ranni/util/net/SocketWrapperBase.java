package com.ranni.util.net;


import com.ranni.connector.ApplicationBufferHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Title: HttpServer
 * Description:
 * Socket的包装抽象类。有对socket的缓冲区的读写
 * TODO:
 * XXX - 暂不支持文件操作
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/30 15:08
 * @Ref org.apache.tomcat.util.net.SocketWrapperBase
 */
public abstract class SocketWrapperBase<E> {

    // ==================================== 属性字段 ====================================

    /**
     * socket
     */
    private E socket;
    
    /**
     * 接收端点（接收器）
     */
    private final AbstractEndpoint<E, ?> endpoint;

    /**
     * socket关闭标志位
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 读取超时
     */
    private volatile long readTimeout = -1;

    /**
     * 写入超时
     */
    private volatile long writeTimeout = -1;

    /**
     * 上一个I/O异常
     */
    protected volatile IOException previousIOException;

    /**
     * 保持连接的剩余请求次数
     */
    private volatile int keepAliveLeft = 100;

    /**
     * why - 升级？
     */
    private volatile boolean upgraded;

    /**
     * 安全标志位
     */
    private boolean secure;

    /**
     * 协商协议
     */
    private String negotiatedProtocol;

    /**
     * 接收请求的服务器IP
     */
    protected String localAddr;

    /**
     * 接收请求的服务器名
     */
    protected String localName = null;

    /**
     * 接收请求的服务器端口号
     */
    protected int localPort = -1;

    /**
     * 发起请求的客户端IP
     */
    protected String remoteAddr = null;

    /**
     * 发起请求的主机名
     */
    protected String remoteHost = null;

    /**
     * 发起请求的端口号
     */
    protected int remotePort = -1;

    /**
     * 记录在非阻塞读上发生的第一个IO异常
     */
    private volatile IOException error;

    /**
     * 套接字通信的缓冲区
     */
    protected volatile SocketBufferHandler socketBufferHandler;

    /**
     * 写入缓冲区的大小
     */
    protected int bufferWriteSize = 64 * 1024; // 默认64k

    /**
     * 额外的非阻塞写入缓冲区队列，无法立即写入到socket的发送缓冲区中，可以在此缓冲区队列中暂存
     */
    protected final WriteBuffer nonBlockingWriteBuffer = new WriteBuffer(bufferWriteSize);

    /* ==================================== 多线程控制资源 start ==================================== */

    /**
     * 读操作信号量
     */
    protected final Semaphore readPending;

    /**
     * 读操作状态
     */
    protected volatile OperationState<?> readOperation = null;

    /**
     * 写操作信号量
     */
    protected final Semaphore writePending;

    /**
     * 写操作状态
     */
    protected volatile OperationState<?> writeOperation = null;

    /* ==================================== 多线程控制资源 end ==================================== */

    /**
     * 当前的处理器
     */
    private final AtomicReference<Object> currentProcessor = new AtomicReference<>();
    

    // ==================================== 构造方法 ====================================
    
    public SocketWrapperBase(E socket, AbstractEndpoint<E,?> endpoint) {
        this.socket = socket;
        this.endpoint = endpoint;
        
        // 是否需要异步IO
        if (endpoint.getUseAsyncIO() || needSemaphores()) {
            readPending = new Semaphore(1);
            writePending = new Semaphore(1);
        } else {
            readPending = null;
            writePending = null;
        }
    }
    

    // ==================================== 抽象方法 ====================================
    
    public abstract void registerWriteInterest();
    public abstract void registerReadInterest();
    public abstract boolean isReadyForRead() throws IOException;
    public abstract void setAppReadBufHandler(ApplicationBufferHandler handler);
    public abstract int read(boolean block, byte[] b, int off, int len) throws IOException;
    public abstract int read(boolean block, ByteBuffer byteBuffer) throws IOException;
    protected abstract void populateRemoteHost();
    protected abstract void populateRemoteAddr();
    protected abstract void populateRemotePort();
    protected abstract void populateLocalName();
    protected abstract void populateLocalAddr();
    protected abstract void populateLocalPort();
    protected abstract void doClose();
    protected abstract void doWrite(boolean block, ByteBuffer from) throws IOException;
    
    /* 发送文件 */
//    public abstract SendfileDataBase createSendfileData(String filename, long pos, long length);
//    public abstract SendfileState processSendfile(SendfileDataBase sendfileData);
    
    
    // ==================================== 内部类 ====================================

    /**
     * 异步操作状态类。当操作完成后，此类的实例作为完成状态进行传递
     */
    protected abstract class OperationState<A> implements Runnable {
        protected final boolean read;
        protected final ByteBuffer[] buffers;
        protected final int offset;
        protected final int length;
        protected final A attachment;
        protected final long timeout;
        protected final TimeUnit unit;
        protected final BlockingMode block;
        protected final CompletionCheck check;
        protected final CompletionHandler<Long, ? super A> handler;
        protected final Semaphore semaphore;
        protected final VectoredIOCompletionHandler<A> completion;
        protected final AtomicBoolean callHandler;

        /**
         * 累计处理的字节数
         */
        protected volatile long nBytes = 0;
        protected volatile CompletionState state = CompletionState.PENDING;
        protected boolean completionDone = true;

        protected OperationState(boolean read, ByteBuffer[] buffers, int offset, int length,
                                 BlockingMode block, long timeout, TimeUnit unit, A attachment,
                                 CompletionCheck check, CompletionHandler<Long, ? super A> handler,
                                 Semaphore semaphore, VectoredIOCompletionHandler<A> completion) {
            this.read = read;
            this.buffers = buffers;
            this.offset = offset;
            this.length = length;
            this.block = block;
            this.timeout = timeout;
            this.unit = unit;
            this.attachment = attachment;
            this.check = check;
            this.handler = handler;
            this.semaphore = semaphore;
            this.completion = completion;
            callHandler = (handler != null) ? new AtomicBoolean(true) : null;
        }

        /**
         * @return 如果返回<b>true</b>，则表示操作是内联的（操作在原始调用者线程上执行的），否则反之
         */
        protected abstract boolean isInline();


        /**
         * 使用连接处理器执行当前操作。
         * 
         * @return 如果返回<b>true</b>，则表示成功执行，否则返回<b>false</b>
         */
        protected boolean process() {
            try {
                getEndpoint().getExecutor().execute(this);
            } catch (RejectedExecutionException ree) {
                System.err.println("socket接收端点执行失败！ " + ree);
            } catch (Throwable t) {
                System.err.println("socket接收端点执行时失败！ " + t);
            }
            
            return false;
        }


        /**
         * 执行run()方法
         */
        protected void start() {
            run();
        }

        
        /**
         * 操作完成
         */
        protected void end() {
            
        }
        
    }


    /**
     * I/O操作完成后的处理
     */
    protected class VectoredIOCompletionHandler<A> implements CompletionHandler<Long, OperationState<A>> {

        /**
         * 此方法在I/O操作正常完成后做后续处理
         * 
         * @param nBytes I/O操作完成的字节数
         * @param state 操作状态
         */
        @Override
        public void completed(Long nBytes, OperationState<A> state) {
            if (nBytes.longValue() < 0) {
                // 非正常完成
                failed(new EOFException(), state);
            } else {
                state.nBytes += nBytes;
                CompletionState currentState = state.isInline() ? CompletionState.INLINE : CompletionState.DONE;
                boolean complete = true; // 本次操作是否完全完成
                boolean completion = true; // 是否调用完成处理器
                
                if (state.check != null) {
                    // 对当前操作状态做下一步指示
                    // 例如，当前的操作是内联完成的，接下来该做什么
                    CompletionHandlerCall call = state.check.callHandler(currentState, state.buffers, state.offset, state.length);
                    if (call == CompletionHandlerCall.CONTINUE) {
                        // 继续操作
                        complete = false;
                    } else if (call == CompletionHandlerCall.NONE) {
                        completion = false;
                    }
                }
                
                if (complete) {
                    // 需要继续进行操作
                    boolean notify = false;
                    
                    // 需要进行读操作还是写操作，只需要释放对应的操作实例
                    if (state.read) {
                        readOperation = null;
                    } else {
                        writeOperation = null;
                    }

                    // 释放信号量，确保有线程在持有信号量的情况下对已经被置null的
                    // readOperation或writeOperation做操作
                    state.semaphore.release();
                    
                    if (state.block == BlockingMode.BLOCK && currentState != CompletionState.INLINE) {
                        // 此操作在阻塞状态下完成，并且是在不同的线程中处理的需要唤醒尝试获取state而等待的完成处理器
                        notify = true;
                    } else {
                        state.state = currentState;
                    }
                    state.end(); // XXX - 标志这个操作的后续处理也完成了，但此方法无实现
                    
                    if (completion && state.handler != null && state.callHandler.compareAndSet(true, false)) {
                        // 继续进行下一步操作
                        state.handler.completed(Long.valueOf(state.nBytes), state.attachment);
                    }
                    
                    synchronized (state) {
                       state.completionDone = true;
                       if (notify) {
                           state.state = currentState;
                           state.notify();
                       }
                    }
                    
                } else {
                    // 不需要继续进行操作
                    synchronized (state) {
                        state.completionDone = true;
                    }
                    state.run();
                }
                
            }
        }


        /**
         * 此方法在I/O操作失败后的后续处理
         * 
         * @param exc 抛出的异常
         * @param state 操作状态
         */
        @Override
        public void failed(Throwable exc, OperationState<A> state) {
            IOException ioe = null;
            if (exc instanceof InterruptedByTimeoutException) {
                ioe = new SocketTimeoutException();
                exc = ioe;
            } else if (exc instanceof IOException) {
                ioe = (IOException) exc;
            }
            setError(ioe);
            boolean notify = false;
            if (state.read) {
                readOperation = null;
            } else {
                writeOperation = null;
            }
            
            // 释放信号量，确保有线程在持有信号量的情况下对已经被置null的
            // readOperation或writeOperation做操作
            state.semaphore.release();
            if (state.block == BlockingMode.BLOCK) {
                notify = true;
            } else {
                state.state = state.isInline() ? CompletionState.ERROR : CompletionState.DONE;
            }
            state.end();
            if (state.handler != null && state.callHandler.compareAndSet(true, false)) {
                state.handler.failed(exc, state.attachment);
            }
            synchronized (state) {
                state.completionDone = true;
                if (notify) {
                    state.state = state.isInline() ? CompletionState.ERROR : CompletionState.DONE;
                    state.notify();
                }
            }
        }
    }


    /**
     * 完成检查接口
     */
    public interface CompletionCheck {
        
        /**
         * 对完成的请求做什么操作
         *
         * @param state 操作状态
         * @param buffers 数据源
         * @param offset 偏移量
         * @param length 数据长度
         * @return 返回应该做什么后续操作
         */
        CompletionHandlerCall callHandler(CompletionState state, ByteBuffer[] buffers, int offset, int length);
    }

    
    /* NIO 2 风格？ */

    /**
     * 阻塞模式
     */
    public enum BlockingMode {
        /**
         * 操作不会阻塞，但是如果有挂起操作将抛出异常
         */
        CLASSIC,

        /**
         * 操作不会阻塞，如果有待处理操作将返回CompletionState.NOT_DONE
         */
        NON_BLOCK,

        /**
         * why - 阻塞挂起操作直到完成。执行后不会阻塞？
         */
        SEMI_BLOCK,

        /**
         * 完全阻塞
         */
        BLOCK
    }


    /**
     * 完成状态
     */
    public enum CompletionState {
        /**
         * 操作仍在等待中
         */
        PENDING,

        /**
         * 操作挂起且非阻塞
         */
        NOT_DONE,

        /**
         * 操作是内联完成的
         */
        INLINE,

        /**
         * 操作内联（操作发起者和操作处理者在同一个线程中）完成但出现错误
         */
        ERROR,

        /**
         * 操作完成，但不是内联的（操作发起者和操作处理者不在同一个线程中）
         */
        DONE,
    }


    /**
     * 操作回调状态
     */
    public enum CompletionHandlerCall {
        /**
         * 操作应该继续，不应该调用完成处理程序
         */
        CONTINUE,

        /**
         * 操作完成，不应该调用完成处理程序
         */
        NONE,

        /**
         * 操作完成，应该调用完成处理程序
         */
        DONE
    }
    
    // ==================================== 核心方法 ====================================
    
    public E getSocket() {
        return socket;
    }
    
    protected void reset(E closedSocket) {
        socket = closedSocket;
    }
    
    protected AbstractEndpoint<E, ?> getEndpoint() {
        return endpoint;
    }
    
    public Object getCurrentProcessor() {
        return currentProcessor;
    }
    
    public void setCurrentProcessor(Object currentProcessor) {
        this.currentProcessor.set(currentProcessor);
    }
    
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    public void setNegotiatedProtocol(String negotiatedProtocol) {
        this.negotiatedProtocol = negotiatedProtocol;
    }
    
    public String getNegotiatedProtocol() {
        return negotiatedProtocol;
    }
    
    public boolean isSecure() {
        return secure;
    }
    
    public IOException getError() {
        return error;
    }

    public void setError(IOException error) {
        // 不可覆盖
        if (this.error != null) {
            return;
        }
        this.error = error;
    }


    /**
     * {@link #error}不为null时直接抛出
     * 
     * @throws IOException 抛出不为null的{@link #error}
     */
    public void checkError() throws IOException {
        if (error != null) {
            throw error;
        }
    }

    
    /**
     * <b>拿走</b>processor。设置此实例中的为null
     * 
     * @return 从AtomicReference中<b>拿走</b>实例
     */
    public Object takeCurrentProcessor() {
        // 返回旧值，设置新值为null
        return currentProcessor.getAndSet(null);
    }


    /**
     * 将处理转交给容器线程
     * 
     * @param runnable 容器线程
     * @throws RejectedExecutionException 如果接收器线程没有运行或者接收器的执行器为null将抛出拒绝执行异常
     */
    public void execute(Runnable runnable) {
        Executor executor = endpoint.getExecutor();
        if (!endpoint.isRunning() || executor == null) {
            throw new RejectedExecutionException();
        }
        executor.execute(runnable);
    }
    

    /**
     * 连接器是否需要信号量
     * 
     * @return 默认返回false，socket包装器根据自己去重写
     */
    public boolean needSemaphores() {
        return false;
    }
    

    /**
     * 设置读取超时，-1表示无限制。
     * 
     * @param readTimeout 小于等于 0时设置为-1
     */
    public void setReadTimeout(long readTimeout) {
        if (readTimeout > 0) { 
            this.readTimeout = readTimeout;
        } else {
            this.readTimeout = -1;
        }
    }


    /**
     * @return 返回读取超时
     */
    public long getReadTimeout() {
        return readTimeout;
    }


    /**
     * 设置写入超时，-1表示无限制。
     *
     * @param writeTimeout 小于等于 0时设置为-1
     */
    public void setWriteTimeout(long writeTimeout) {
        if (writeTimeout > 0) {
            this.writeTimeout = writeTimeout;
        } else {
            this.writeTimeout = -1;
        }
    }
    

    /**
     * @return 返回写入超时
     */
    public long getWriteTimeout() {
        return writeTimeout;
    }
    

    /**
     * @return 如果返回<b>true</b>，表示还有数据可读
     */
    public boolean hasDataToRead() {
        return true;
    }

    
    /**
     * @return 如果返回<b>true</b>，则表示套接字写入缓冲区或者异步写入缓冲区是有数据的
     */
    public boolean hasDataToWrite() {
        return !socketBufferHandler.isWriteBufferEmpty() || !nonBlockingWriteBuffer.isEmpty();
    }


    /**
     * 设置保持连接的剩余请求次数
     * 
     * @param keepAliveLeft 剩余请求次数
     */
    public void setKeepAliveLeft(int keepAliveLeft) {
        this.keepAliveLeft = keepAliveLeft;
    }


    /**
     * @return 保持连接剩余请求次数自减一后返回
     */
    public int decrementKeepAlive() {
        return --keepAliveLeft;
    }
    
    
    public String getRemoteHost() {
        if (remoteHost == null) {
            populateRemoteHost();
        }
        return remoteHost;
    }

    public String getRemoteAddr() {
        if (remoteAddr == null) {
            populateRemoteAddr();
        }
        return remoteAddr;
    }
    
    public int getRemotePort() {
        if (remotePort == -1) {
            populateRemotePort();
        }
        return remotePort;
    }

    public String getLocalName() {
        if (localName == null) {
            populateLocalName();
        }
        return localName;        
    }
    
    public String getLocalAddr() {
        if (localAddr == null) {
            populateLocalAddr();
        }
        return localAddr;
    }
    
    public int getLocalPort() {
        if (localPort == -1) {
            populateLocalPort();
        }
        return localPort;
    }

    public SocketBufferHandler getSocketBufferHandler() {
        return socketBufferHandler;
    }


    /**
     * 是否准备好写入数据了
     * 
     * @return 如果返回<b>true</b>，则表示可以写入数据
     */
    public boolean isReadyForWrite() {
        boolean result = canWrite();
        if (!result) {
            // 不可写入，注册使得可写入
            registerWriteInterest();
        }        
        return result;
    }
    
    
    public boolean canWrite() {
        if (socketBufferHandler == null) {
            throw new IllegalStateException("socket已关闭！");
        }
        return socketBufferHandler.isWriteBufferWritable() && nonBlockingWriteBuffer.isEmpty();
    }


    /**
     * 关闭socket wrapper
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // 未关闭，关闭并设置关闭标志位为true
            try {
                getEndpoint().getHandler().release(this);
            } catch (Throwable e) {
                throw e;
            } finally {
                getEndpoint().countDownConnection();
                doClose();
            }
        }
    }


    /**
     * @return 如果返回<b>true</b>，则表示当前socket已经关闭
     */
    public boolean isClosed() {
        return closed.get();
    }


    // ==================================== 数据处理 ====================================

    /**
     * 把from中的数据从offset开始往后length个数据转移到to中
     * 
     * @param from 数据源
     * @param offset 偏移量
     * @param length 数据量
     * @param to 转移到的目标
     * @return 返回转移的数据量
     */
    protected static int transfer(byte[] from, int offset, int length, ByteBuffer to) {
        int max = Math.min(length, to.remaining());
        if (max > 0) {
            to.put(from, offset, max);
        }
        return max;
    }

    /**
     * 把from中的数据转移到to中
     *
     * @param from 数据源
     * @param to 转移到的目标
     * @return 返回转移的数据量
     */
    protected static int transfer(ByteBuffer from, ByteBuffer to) {
        int max = Math.min(from.remaining(), to.remaining());
        if (max > 0) {
            int fromLimit = from.limit();
            from.limit(from.position() + max);
            to.put(from);
            from.limit(fromLimit);
        }
        return max;
    }

    /**
     * 缓冲区中是否还有剩余的数据
     * 
     * @param buffers 缓冲区数组
     * @param offset 偏移量
     * @param length 缓冲区个数
     * @return 只要在buffers的[offset, offset + length]返回内的缓冲区有数据就返回true
     */
    protected static boolean buffersArrayHasRemaining(ByteBuffer[] buffers, int offset, int length) {
        for (int pos = offset; pos < offset + length; pos++) {
            if (buffers[pos].hasRemaining()) {
                return true;
            }
        }
        return false;
    }


    /**
     * 转移socket缓冲处理器的读缓冲区中的数据到目标字节数组中
     * 
     * @param b 目标字节数组
     * @param off 偏移量
     * @param len 数据量
     * @return 转移的真实数据量
     */
    protected int populateReadBuffer(byte[] b, int off, int len) {
        // 将读缓冲区切换至读模式后，把读缓冲区中的数据读出来
        socketBufferHandler.configureReadBufferForRead();
        ByteBuffer readBuffer = socketBufferHandler.getReadBuffer();
        int remaining = readBuffer.remaining();
        
        if (remaining > 0) {
            remaining = Math.min(remaining, len);
            readBuffer.get(b, off, remaining);
        }
        
        return remaining;
    }


    /**
     * 转移socket缓冲处理器的读缓冲区中的数据到目标字节缓冲区中
     * 
     * @param to 目标字节缓冲区
     * @return 转移的真实数据量
     */
    protected int populateReadBuffer(ByteBuffer to) {
        // 将读缓冲区切换至读模式后，把读缓冲区中的数据读出来
        socketBufferHandler.configureReadBufferForRead();
        int nRead = transfer(socketBufferHandler.getReadBuffer(), to);
        
        return nRead;
    }


    /**
     * 将已读取的数据重新放回到Socket的读缓冲区中。</br>
     * 放回到socket缓冲处理器读缓冲区未读数据之前，会将未读数据往后
     * 移，腾出可容纳回放数据的空间。
     * 
     * @param returnedInput 已读取的数据
     */
    public void unRead(ByteBuffer returnedInput) {
        if (returnedInput != null) {
            socketBufferHandler.unReadReadBuffer(returnedInput);
        }
    }


    /**
     * 写入到socket缓冲处理器的写入缓冲区中。如果socket缓冲处理器的写入缓
     * 冲区满了，会自动写入到内核层socket的写入缓冲区中
     * 
     * @param block 是否阻塞
     * @param buf 要写入的数据
     * @param off 偏移量
     * @param len 写入的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public final void write(boolean block, byte[] buf, int off, int len) throws IOException {
        if (len == 0 || buf == null) {
            return;
        }
        
        if (block) {
            writeBlocking(buf, off, len);
        } else {
            writeNonBlocking(buf, off, len);
        }
    }


    /**
     * 阻塞式写入到socket缓冲处理器的写入缓冲区（非socket缓冲区，socket缓冲处理器是在应用层上的缓冲区。
     * socket缓冲区处于内核层）
     * 
     * @param buf 要写入的缓冲数据
     * @throws IOException 可能抛出I/O异常
     */
    protected void writeBlocking(ByteBuffer buf) throws IOException {
        if (buf.hasRemaining()) {
            socketBufferHandler.configureWriteBufferForWrite();
            transfer(buf, socketBufferHandler.getWriteBuffer());
            while (buf.hasRemaining()) {
                // 还有数据，但是socket缓冲处理器中的写缓冲区满了，需要先发送出去
                doWrite(true); 
                socketBufferHandler.configureWriteBufferForWrite();
                transfer(buf, socketBufferHandler.getWriteBuffer());    
            }
        }
    }


    /**
     * 阻塞式写入到socket缓冲处理器的写入缓冲区（非socket缓冲区，socket缓冲处理器是在应用层上的缓冲区。
     * socket缓冲区处于内核层）
     * 
     * @param buf 要写入的数据
     * @param off 偏移量
     * @param len 要写入的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void writeBlocking(byte[] buf, int off, int len) throws IOException {
        if (len > 0) {
            socketBufferHandler.configureWriteBufferForWrite();
            int nWrite = transfer(buf, off, len, socketBufferHandler.getWriteBuffer());
            len -= nWrite;
            while (len > 0) {
                off += nWrite;
                doWrite(true);
                nWrite = transfer(buf, off, len, socketBufferHandler.getWriteBuffer());
                len = len - nWrite;
            }
        }
    }


    /**
     * 非阻塞式写入数据。
     * 
     * @param buf 要写入的数据
     * @throws IOException 可能抛出I/O异常
     */
    protected void writeNonBlocking(ByteBuffer buf) throws IOException { 
        if (buf.hasRemaining() // 有要写入的数据 
            && nonBlockingWriteBuffer.isEmpty() // 如果非阻塞缓冲队列是空的（说明没有需要在此数据之前发送的数据，那此数据可以直接发送，如果有数据，需要以先来先出的顺序发送出去先。因为非阻塞式需要保证数据的顺序）
            && socketBufferHandler.isWriteBufferWritable()) { // 如果socket缓冲处理器还可以写入数据到写入缓冲区中
            
            writeNonBlockingInternal(buf);
            
        }
        
        if (buf.hasRemaining()) {
            // 还有数据，那么把要写入的缓冲区加入到非阻塞缓冲区的待写入缓冲区队列中
            nonBlockingWriteBuffer.add(buf);
        }
    }


    /**
     * 非阻塞式写入数据。
     * 
     * @param buf 要写入的数据
     * @param off 偏移量
     * @param len 数据长度
     * @throws IOException 可能抛出I/O异常
     */
    protected void writeNonBlocking(byte[] buf, int off, int len) throws IOException {
        if (len > 0 && nonBlockingWriteBuffer.isEmpty()
            && socketBufferHandler.isWriteBufferWritable()) {
            
            int nWrite = transfer(buf, off, len, socketBufferHandler.getWriteBuffer());
            len -= nWrite;
            while (len > 0) {
                off += nWrite;
                doWrite(false);
                if (len > 0 && socketBufferHandler.isWriteBufferWritable()) {
                    socketBufferHandler.configureWriteBufferForWrite();
                    nWrite = transfer(buf, off, len, socketBufferHandler.getWriteBuffer());
                } else {
                    // socket缓冲区处理器的写入缓冲区装不下了
                    // 跳出循环，装入到非阻塞写入缓冲区队列中
                    break;
                }
                
                len -= nWrite;
            }
            
            if (len > 0) {
                nonBlockingWriteBuffer.add(buf, off, len);
            }
        }
    }


    /**
     * 目的和{@link #writeNonBlocking(byte[], int, int)}第一个if内的代码是一致的</br>
     * 将传来的缓冲区buf的数据写入到socket缓冲处理器的写入缓冲区中，如果buf中还有数据可以写入，
     * 就先采用非阻塞式将socket缓冲处理器中的写入缓冲区数据发送出去，然后再尝试写入到socket缓冲
     * 处理器的写入缓冲区中。如果socket缓冲处理器的写入缓冲区没有写入的空间，则结束此方法。
     * 
     * @param buf 数据源
     * @throws IOException 可能抛出I/O异常
     */
    protected void writeNonBlockingInternal(ByteBuffer buf) throws IOException {
        socketBufferHandler.configureWriteBufferForWrite();
        transfer(buf, socketBufferHandler.getWriteBuffer());
        while (buf.hasRemaining()) {
            doWrite(false);
            if (socketBufferHandler.isWriteBufferWritable()) {
                socketBufferHandler.configureWriteBufferForWrite();
                transfer(buf, socketBufferHandler.getWriteBuffer());
            } else {
                break;
            }
        }
    }


    /**
     * 写入到socket的写入缓冲区中（非socket缓冲处理器的写入缓冲区）
     * 
     * @param block 如果为<b>true</b>，则表示阻塞式写入，否则反之
     * @throws IOException 可能抛出I/O异常
     */
    protected void doWrite(boolean block) throws IOException {
        socketBufferHandler.configureReadBufferForRead();
        doWrite(block, socketBufferHandler.getWriteBuffer());
    }


    /**
     * 将缓冲区中的数据尽可能的写入到socket写入缓冲区中
     * 
     * @param block 如果为<b>true</b>，则表示阻塞式写入，否则反之
     * @return 如果为<b>true</b>，缓冲区的数据全部写完了，否则反之。阻塞式flush永远为false
     * @throws IOException 可能抛出I/O异常
     */
    public boolean flush(boolean block) throws IOException {
        boolean result = false;
        
        if (block) {
            // 阻塞式刷新，直到缓冲区数据全部清空
            flushBlocking();
        } else {
            result = flushNonBlocking();
        }
        
        return result;
    }


    /**
     * 阻塞式刷新缓冲区，直到清空socket缓冲处理器的写入缓冲区数据
     * 以及非阻塞写入缓冲区队列中的数据。
     * 
     * @throws IOException 可能抛出I/O异常
     */
    protected void flushBlocking() throws IOException {
        doWrite(true);
        
        if (!nonBlockingWriteBuffer.isEmpty()) {
            // 将非阻塞缓冲队列中的缓冲数据依次写入到socket缓冲处理器的写入缓冲区中
            // socket缓冲处理器的写入缓冲区装不下了就会先将数据发送出去再装
            // 其实就是回调了此类的writeNonBlocking(ByteBuffer)
            nonBlockingWriteBuffer.write(this, true);
            
            if (!socketBufferHandler.isWriteBufferEmpty()) {
                doWrite(true);
            }
        }
    }


    /**
     * 非阻塞式的刷新缓冲区数据
     * 
     * @return 如果为<b>true</b>，缓冲区的数据全部写完了，否则反之。
     * @throws IOException 可能抛出I/O异常
     */
    protected boolean flushNonBlocking() throws IOException {
        // 是否有剩余数据的标志位
        boolean dataLeft = !socketBufferHandler.isWriteBufferEmpty(); 

        // 如果socket缓冲处理器的写入缓冲区中有数据，先将其以非阻塞式写入到socket的写入缓冲区中
        if (dataLeft) {
            doWrite(false);
            dataLeft = !socketBufferHandler.isWriteBufferEmpty();
        }
        
        if (!dataLeft && !nonBlockingWriteBuffer.isEmpty()) {
            // 将非阻塞缓冲队列中的缓冲数据依次写入到socket缓冲处理器的写入缓冲区中
            // socket缓冲处理器的写入缓冲区装不下了就会先将数据发送出去再装
            // 其实就是回调了此类的writeNonBlockingInternal(ByteBuffer)
            dataLeft = nonBlockingWriteBuffer.write(this, false);
            
            // 这里dataLeft为false表示非阻塞缓冲区队列中的缓冲数据已经全部清空
            if (!dataLeft && !socketBufferHandler.isWriteBufferEmpty()) {
                doWrite(false);
                dataLeft = !socketBufferHandler.isWriteBufferEmpty();
            }
        }
        
        return dataLeft;
    }


    /**
     * 处理SocketWrapper
     * 
     * @param socketStatus socket状态
     * @param dispatch 是否在新的容器线程上执行
     */
    public void processSocket(SocketEvent socketStatus, boolean dispatch) {
        endpoint.processSocket(this, socketStatus, dispatch);
    }
    
}
