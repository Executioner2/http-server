package com.ranni.util.net;

import com.ranni.connector.ApplicationBufferHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Title: HttpServer
 * Description:
 * 通信信道<br>
 * SocketChannel的包装类。也是相当于是ByteBuffer的包装
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/4 12:21
 * @Ref org.apache.tomcat.util.net.Nio2Channel
 */
public class Nio2Channel implements AsynchronousByteChannel {

    protected static final ByteBuffer emptyBuf = ByteBuffer.allocate(0);

    protected final SocketBufferHandler bufHandler; // 套接字缓冲区
    protected AsynchronousSocketChannel sc = null;
    protected SocketWrapperBase<Nio2Channel> socketWrapper = null;

    public Nio2Channel(SocketBufferHandler bufHandler) {
        this.bufHandler = bufHandler;
    }


    /**
     * 重置信道
     * 
     * @param channel 信道
     * @param socketWrapper 套接字包装实例
     */
    public void reset(AsynchronousSocketChannel channel, SocketWrapperBase<Nio2Channel> socketWrapper) {
        this.sc = channel;
        this.socketWrapper = socketWrapper;
        bufHandler.reset();
    }


    /**
     * 清空套接字缓冲区剩余空间
     */
    public void free() {
        bufHandler.free();
    }


    /**
     * @return 如果返回<b>true</b>，则表示socket通信信道已经打开
     */
    @Override
    public boolean isOpen() {
        return sc.isOpen();
    }


    /**
     * 关闭这个信道
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void close() throws IOException {
        sc.close();
    }


    /**
     * 关闭连接
     * 
     * @param force 是否强制关闭底层套接字
     * @throws IOException 可能抛出I/O异常
     */
    public void close(boolean force) throws IOException {
        if (isOpen() || force) {
            close();
        }
    }


    @Override
    public String toString() {
        return super.toString() + ":" + sc.toString();
    }    

    public SocketBufferHandler getBufferHandler() {
        return bufHandler;
    }

    public AsynchronousSocketChannel getIOChannel() {
        return sc;
    }

    /**
     * @return 如果返回<b>true</b>，则表示SSL层的握手完成
     */
    public boolean isHandshakeComplete() {
        return true;
    }

    /**
     * 这个SSL才有用。非SSL并无作用
     * 
     * @return 返回0
     * @throws IOException 不会抛出I/O异常
     */
    public int handshake() throws IOException {
        return 0;
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        return sc.read(dst);
    }

    @Override
    public <A> void read(ByteBuffer dst, A attachment, 
                         CompletionHandler<Integer, ? super A> handler) {
        sc.read(dst, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }

    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment,
                         CompletionHandler<Integer, ? super A> handler) {

        sc.read(dst, timeout, unit, attachment, handler);
    }
    
    public <A> void read(ByteBuffer[] dsts, int offset, int length, 
                         long timeout, TimeUnit unit, A attachment, 
                         CompletionHandler<Long, ? super A> handler) {

        sc.read(dsts, offset, length, timeout, unit, attachment, handler);
    }
    

    @Override
    public Future<Integer> write(ByteBuffer src) {
        return sc.write(src);
    }
    
    @Override
    public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
        write(src, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }
    
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit,
                     A attachment, CompletionHandler<Integer, ? super A> handler) {
        sc.write(src, timeout, unit, attachment, handler);
    }

    public <A> void write(ByteBuffer[] srcs, int offset, int length,
                          long timeout, TimeUnit unit, A attachment,
                          CompletionHandler<Long, ? super A> handler) {
        sc.write(srcs, offset, length, timeout, unit, attachment, handler);
    }


    /**
     * 异步完成
     */
    private static final Future<Boolean> DONE = new Future<Boolean>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
            return Boolean.TRUE;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return Boolean.TRUE;
        }
    };


    public Future<Boolean> flush() {
        return DONE;
    }


    private ApplicationBufferHandler appReadBufHandler;
    public void setAppReadBufHandler(ApplicationBufferHandler handler) {
        this.appReadBufHandler = handler;
    }
    protected ApplicationBufferHandler getAppReadBufHandler() {
        return appReadBufHandler;
    }


    /**
     * 用于信道已经被关闭后仍被回调的返回结果
     */
    private static final Future<Integer> DONE_INT = new Future<Integer>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Integer get() throws InterruptedException, ExecutionException {
            return Integer.valueOf(-1);
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return Integer.valueOf(-1);
        }
    };
    
    
    static final Nio2Channel CLOSED_NIO2_CHANNEL = new Nio2Channel(SocketBufferHandler.EMPTY) {
        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isOpen() {
            return false;
        }
        @Override
        public void free() {
        }
        @Override
        protected ApplicationBufferHandler getAppReadBufHandler() {
            return ApplicationBufferHandler.EMPTY;
        }
        @Override
        public void setAppReadBufHandler(ApplicationBufferHandler handler) {
        }
        @Override
        public Future<Integer> read(ByteBuffer dst) {
            return DONE_INT;
        }
        @Override
        public <A> void read(ByteBuffer dst,
                             long timeout, TimeUnit unit, A attachment,
                             CompletionHandler<Integer, ? super A> handler) {
            handler.failed(new ClosedChannelException(), attachment);
        }
        @Override
        public <A> void read(ByteBuffer[] dsts,
                             int offset, int length, long timeout, TimeUnit unit,
                             A attachment, CompletionHandler<Long,? super A> handler) {
            handler.failed(new ClosedChannelException(), attachment);
        }
        @Override
        public Future<Integer> write(ByteBuffer src) {
            return DONE_INT;
        }
        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment,
                              CompletionHandler<Integer, ? super A> handler) {
            handler.failed(new ClosedChannelException(), attachment);
        }
        @Override
        public <A> void write(ByteBuffer[] srcs, int offset, int length,
                              long timeout, TimeUnit unit, A attachment,
                              CompletionHandler<Long,? super A> handler) {
            handler.failed(new ClosedChannelException(), attachment);
        }
        @Override
        public String toString() {
            return "Closed Nio2Channel";
        }
    };
}
