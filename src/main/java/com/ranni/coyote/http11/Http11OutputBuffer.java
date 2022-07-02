package com.ranni.coyote.http11;

import com.ranni.coyote.ActionCode;
import com.ranni.coyote.CloseNowException;
import com.ranni.coyote.Response;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/15 19:59
 * @Ref org.apache.coyote.http11.Http11OutputBuffer
 */
public class Http11OutputBuffer implements HttpOutputBuffer {

    // ==================================== 属性字段 ====================================
    
    /**
     * CoyoteResponse
     */
    protected final Response response;

    /**
     * 响应头是否发送了
     */
    private volatile boolean ackSent = false;

    /**
     * 是否完成了响应
     */
    protected boolean responseFinished;

    /**
     * 响应头缓冲区
     */
    protected final ByteBuffer headerBuffer;
    
    /**
     * 处理响应体的输出过滤器
     */
    protected OutputFilter[] filterLibrary;

    /**
     * 当前请求的活动过滤器链
     */
    protected OutputFilter[] activeFilters;

    /**
     * 最后一个活动的过滤器
     */
    protected int lastActiveFilter;

    /**
     * 底层缓冲输出区
     */
    protected HttpOutputBuffer outputStreamOutputBuffer;

    /**
     * socket包装实例
     */
    protected SocketWrapperBase<?> socketWrapper;

    /**
     * 写入的字节数
     */
    protected long byteCount;
    
    
    // ==================================== 构造方法 ====================================    

    public Http11OutputBuffer(Response response, int headerBufferSize) {
        this.response = response;
        
        headerBuffer = ByteBuffer.allocate(headerBufferSize);
        filterLibrary = new OutputFilter[0];
        activeFilters = new OutputFilter[0];
        lastActiveFilter = -1;
        
        outputStreamOutputBuffer = new SocketOutputBuffer();
        
    }


    // ==================================== 内部类 ====================================    

    /**
     * 连接socket缓冲区处理器 {@link com.ranni.util.net.SocketBufferHandler} 
     * 和服务器输出缓冲之间的桥梁。
     */
    protected class SocketOutputBuffer implements HttpOutputBuffer {

        /**
         * 阻塞式刷新缓冲区
         * 
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        public void end() throws IOException {
            socketWrapper.flush(true);
        }
        

        /**
         * 如果CoyoteResponse支持异步，则异步刷新缓冲区，否则阻塞刷新缓冲区
         * 
         * @throws IOException 可能抛出I/O异常
         */
        @Override
        public void flush() throws IOException {
            socketWrapper.flush(isBlocking());
        }


        /**
         * 将数据写入到
         * 
         * @param chunk 要写入的数据
         * @return 返回实际写入的数据量
         * @throws IOException
         */
        @Override
        public int doWrite(ByteBuffer chunk) throws IOException {
            try {
                int len = chunk.remaining();
                SocketWrapperBase<?> socketWrapper = Http11OutputBuffer.this.socketWrapper;
                if (socketWrapper != null) {
                    socketWrapper.write(isBlocking(), chunk);
                } else {
                    throw new CloseNowException("iob.failedwrite");
                }
                
                len -= chunk.remaining();
                byteCount += len;
                return len;
            } catch (IOException ioe) {
                response.action(ActionCode.CLOSE_NOW, ioe);
                throw ioe;
            }
        }


        /**
         * @return 返回写入的字节数
         */
        @Override
        public long getBytesWritten() {
            return byteCount;
        }
    }


    // ==================================== 核心方法 ====================================    

    /**
     * 添加过滤器到过滤器库，同时清空活动过滤器集合
     * 
     * @param filter 要添加的过滤器
     */
    public void addFilter(OutputFilter filter) {
        OutputFilter[] ars = Arrays.copyOf(filterLibrary, filterLibrary.length + 1);
        ars[filterLibrary.length] = filter;
        filterLibrary = ars;
        
        activeFilters = new OutputFilter[filterLibrary.length];
    }


    /**
     * @return 返回当前的过滤器库
     */
    public OutputFilter[] getFilters() {
        return filterLibrary;
    }


    /**
     * 添加活动过滤器
     * 
     * @param filter 活动的过滤器
     */
    public void addActiveFilter(OutputFilter filter) {
        if (lastActiveFilter == -1) {
            // 当前还没有活动的过滤器。那么此filter作为第一个
            // 过滤器，将设置缓冲区为SocketOutputBuffer
            filter.setBuffer(outputStreamOutputBuffer);
        } else {
            // 检查过滤器是否重复，重复则不添加到活动过滤器中
            for (int i = 0; i <= lastActiveFilter; i++) {
                if (activeFilters[i] == filter) {
                    return;
                }
            }
            
            // 设置此过滤器是的缓冲区处理器为数组中上一个过滤器，以形成
            // 一个以数组尾为头的过滤链
            filter.setBuffer(activeFilters[lastActiveFilter]);
        }
        
        activeFilters[++lastActiveFilter] = filter;
        filter.setResponse(response);
    }


    /**
     * @return 如果返回<b>true</b>，则表示当前的请求以阻塞方式进行。否则反之
     */
    public boolean isBlocking() {
        return response.getWriteListener() == null;
    }


    /**
     * 重复使用
     */
    public void recycle() {
        nextRequest();
        socketWrapper = null;
    }


    /**
     * 重置一些属性值，准备处理下个请求
     */
    public void nextRequest() {
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        response.recycle();
        resetHeaderBuffer();
        lastActiveFilter = -1;
        ackSent = false;
        responseFinished = false;
        byteCount = 0;
    }


    public void init(SocketWrapperBase<?> socketWrapper) {
        this.socketWrapper = socketWrapper;
    }

    
    public boolean hasDataToWrite() {
        return socketWrapper.hasDataToWrite();
    }
    
    
    protected final boolean isReady() {
        boolean res = !hasDataToWrite();
        if (!res) {
            // 没有数据可以写，注册写感兴趣
            socketWrapper.registerWriteInterest();
        }
        return res;
    }
    
    
    public void registerWriteInterest() {
        socketWrapper.registerWriteInterest();
    }


    /**
     * @return 如果返回<b>true</b>，则表示有分块过滤器
     */
    boolean isChunking() {
        for (int i = 0; i < lastActiveFilter; i++) {
            if (activeFilters[i] == filterLibrary[Constants.CHUNKED_FILTER]) {
                return true;
            }
        }
        
        return false;
    }
    
    
    // ==================================== 缓冲区处理 ====================================
    
    /**
     * 和{@link #flush()}差不多。如果已经完成了
     * 响应，则直接return。否则，在刷新完缓冲区的
     * 数据后，把完成响应的标志位(responseFinished)
     * 
     * @throws IOException
     */
    @Override
    public void end() throws IOException {
        if (responseFinished) {
            return;
        }
        
        if (lastActiveFilter == -1) {
            outputStreamOutputBuffer.end();
        } else {
            activeFilters[lastActiveFilter].end();
        }
        
        responseFinished = true;
    }


    /**
     * 刷新缓冲区，如果有活动过滤器，那么调用活动过滤器
     * 的flush()，使得整条过滤器链都能够刷新
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void flush() throws IOException {
        if (lastActiveFilter == -1) {
            outputStreamOutputBuffer.flush();
        } else {
            activeFilters[lastActiveFilter].flush();
        }
    }


    /**
     * 重置缓冲区内容
     */
    void resetHeaderBuffer() {
        headerBuffer.position(0).limit(headerBuffer.capacity());
    }


    /**
     * 刷新缓冲区
     * 
     * @param block 如果为<b>true</b>，则表示阻塞式刷新缓冲区
     * @return 如果返回<b>true</b>，则表示刷新成功，否则刷新失败
     */
    private boolean flushBuffer(boolean block) throws IOException {
        return socketWrapper.flush(block);
    }



    /**
     * @return 返回写入的数据量
     */
    @Override
    public long getBytesWritten() {
        return byteCount;
    }


    /**
     * 写入数据到缓冲区
     *
     * @param chunk 要写入的数据
     * @return 返回实际写入的数据量
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int doWrite(ByteBuffer chunk) throws IOException {
        if (!response.isCommitted()) {
            response.action(ActionCode.COMMIT, null);
        }
        
        if (lastActiveFilter == -1) {
            return outputStreamOutputBuffer.doWrite(chunk);
        } else {
            return activeFilters[lastActiveFilter].doWrite(chunk);
        }
    }
    
    
    public void write(byte[] b) {
        checkLengthBeforeWrite(b.length);
        headerBuffer.put(b);
    }
    
    private void write(int value) {
        String s = Integer.toString(value);
        int len = s.length();
        checkLengthBeforeWrite(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            headerBuffer.put((byte) c);
        }
    }
    
    private void write(MessageBytes mb) {
        if (mb.getType() != MessageBytes.T_BYTES) {
            mb.toBytes();
            ByteChunk byteChunk = mb.getByteChunk();

            byte[] buffer = byteChunk.getBuffer();
            for (int i = byteChunk.getStart(); i < byteChunk.getLength(); i++) {
                if ((buffer[i] > -1 && buffer[i] <= 31 && buffer[i] != 9)
                     || buffer[i] == 127) {
                    buffer[i] = ' ';
                }
            }
        }
        write(mb.getByteChunk());
    }
    
    
    private void write(ByteChunk bc) {
        int length = bc.getLength();
        checkLengthBeforeWrite(length);
        headerBuffer.put(bc.getBytes(), bc.getStart(), length);
    }


    /**
     * 检查缓冲区是否有足够的空间容纳需要写入的数据量
     * @param length
     */
    private void checkLengthBeforeWrite(int length) {
        // +4是因为要为 CR LF COLON SP 预留
        // 见 public void sendHeader(MessageBytes, MessageBytes)
        if (headerBuffer.position() + length + 4 > headerBuffer.capacity()) {
            throw new HeadersTooLargeException("iob.responseheadertoolarge.error");
        }
    }


    // ==================================== 响应处理 ====================================
    
    /**
     * 发送响应头
     *
     * @throws IOException 可能抛出I/O异常
     */
    public void sendAck() throws IOException {
        if (!response.isCommitted() && !ackSent) {
            ackSent = true;
            socketWrapper.write(isBlocking(), Constants.ACK_BYTES, 0, Constants.ACK_BYTES.length);
            if (flushBuffer(true)) {
                throw new IOException("iob.failedwrite.ack");
            }
        }
    }


    /**
     * 写入响应头的标头
     * 
     * @param name 
     * @param value
     */
    public void sendHeader(MessageBytes name, MessageBytes value) {
        write(name);
        headerBuffer.put(Constants.COLON).put(Constants.SP);
        write(value);
        headerBuffer.put(Constants.CR).put(Constants.LF);
    }


    /**
     * 结束响应头标头数据写入，添加一行分隔响应头和响应体的空白行
     */
    public void endHeader() {
        headerBuffer.put(Constants.CR).put(Constants.LF);
    }


    /**
     * 提交响应
     * 
     * @throws Exception 可能抛出I/O异常
     */
    protected void commit() throws Exception {
        response.setCommitted(true);
        
        if (headerBuffer.position() > 0) {
            headerBuffer.flip();
            try {
                if (socketWrapper != null) {
                    socketWrapper.write(isBlocking(), headerBuffer);
                } else {
                    throw new CloseNowException("iob.failedwrite");
                }
            } finally {
                resetHeaderBuffer();
            }
        }
    }

}
