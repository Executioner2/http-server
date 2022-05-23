package com.ranni.connector;

import com.ranni.coyote.ActionCode;
import com.ranni.coyote.CloseNowException;
import com.ranni.coyote.CoyoteAdapter;
import com.ranni.coyote.Response;
import com.ranni.util.buf.C2BConverter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 输入缓冲区
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 20:02
 */
public class OutputBuffer extends Writer {

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    // 解码器缓存
    private final Map<Charset, C2BConverter> encoders = new HashMap<>();

    private final int defaultBufferSize; // 默认的缓冲大小

    private ByteBuffer bb;
    private CharBuffer cb;
    
    private boolean initial = true; // 是否还未发送响应头

    private long bytesWritten; // 写入的字节数
    private long charsWritten; // 写入的字符数

    private volatile boolean closed;
    private volatile boolean suspended;

    private boolean doFlush; // 是否正在刷新流
    private Response coyoteResponse; 

    protected C2BConverter conv; // 字符转字节转换器


    
    
    public OutputBuffer(int size) {
        defaultBufferSize = size;
        bb = ByteBuffer.allocate(size);
        clear(bb);
        cb = CharBuffer.allocate(size);
        clear(cb);
    }


    // ------------------------------ 通用方法 ------------------------------
    
    private void clear(Buffer buffer) {
        buffer.rewind().limit(0);
    }
    
    
    public void setResponse(Response coyoteResponse) {
        this.coyoteResponse = coyoteResponse;
    }
    
    
    public Response getResponse() {
        return this.coyoteResponse;
    }


    /**
     * @return 返回挂起标志
     */
    public boolean isSuspended() {
        return suspended;
    }

    
    /**
     * 设置挂起标志
     * 
     * @param suspended 挂起标志
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }


    /**
     * @return 输出缓冲区是否已关闭
     */
    public boolean isClosed() {
        return this.closed;
    }


    /**
     * 刷新输出缓冲区并触发刷新钩子
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void flush() throws IOException {
        doFlush(true);
    }


    /**
     * 重置缓冲区
     */
    public void recycle() {
        initial = true;
        bytesWritten = 0;
        charsWritten = 0;

        if (bb.capacity() > 16L * defaultBufferSize) {
            bb = ByteBuffer.allocate(defaultBufferSize);
        }
        
        clear(bb);
        clear(cb);
        closed = false;
        suspended = false;
        doFlush = false;

        if (conv != null) {
            conv.recycle();
            conv = null;
        }
    }


    /**
     * 关闭缓冲区
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void close() throws IOException {
        if (closed || suspended) {
            return;
        }
        
        if (cb.remaining() > 0) {
            flushCharBuffer();
        }
        
        if (!coyoteResponse.isCommitted() && coyoteResponse.getContentLengthLong() == -1
            && !coyoteResponse.getRequest().method().equals("HEAD")) {
            // 如果响应没有提交，并且响应长度没有被计算，而且这不是
            // HEAD请求（HEAD请求的响应不包含响应体）。
            coyoteResponse.setContentLength(bb.remaining());
        }
        
        if (coyoteResponse.getStatus() == HttpServletResponse.SC_SWITCHING_PROTOCOLS) {
            doFlush(true);
        } else {
            doFlush(false);
        }        
        closed = true;

        // 将输入流也关闭
        Request req = (Request) coyoteResponse.getRequest().getNote(CoyoteAdapter.ADAPTER_NOTES);
        req.inputBuffer.close();
        
        coyoteResponse.action(ActionCode.CLOSE, null);
    }


    /**
     * 刷新输出缓冲区
     * 
     * @param realFlush 是否触发刷新钩子
     */
    protected void doFlush(boolean realFlush) throws IOException {
        if (suspended) {
            return;
        }
        
        try {
            doFlush = true;
            if (initial) {
                coyoteResponse.sendHeaders();
                initial = false;
            }
            if (cb.remaining() > 0) {
                flushCharBuffer();
            }
            if (bb.remaining() > 0) {
                flushByteBuffer();
            }
        } finally {
            doFlush = false;
        }
        
        if (realFlush) {
            coyoteResponse.action(ActionCode.CLIENT_FLUSH, null);
            if (coyoteResponse.isExceptionPresent()) {
                throw new ClientAbortException(coyoteResponse.getErrorException());
            }
        }
    }


    // ------------------------------ 字节缓冲区 ------------------------------

    /**
     * 刷新字节缓冲区
     *
     * @throws IOException 可能抛出I/O异常
     */
    private void flushByteBuffer() throws IOException {
        realWriteBytes(bb.slice());
        clear(bb);
    }


    /**
     * 将字节缓冲区中的数据写入到输出流中
     *
     * @param buf 要被写入的缓冲区
     * @throws IOException 可能抛出I/O异常
     */
    public void realWriteBytes(ByteBuffer buf) throws IOException {
        if (closed || coyoteResponse == null) {
            return;
        }

        if (buf.remaining() > 0) {
            try {
                coyoteResponse.doWrite(buf);
            } catch (CloseNowException cne) {
                // 异常关闭
                closed = true;
                throw cne;
            } catch (IOException ioe) {
                coyoteResponse.setErrorException(ioe);
                throw ioe;
            }
        }
    }


    // ------------------------------ 字符缓冲区 ------------------------------

    /**
     * 刷新字符缓冲区
     *
     * @throws IOException 可能抛出I/O异常
     */
    private void flushCharBuffer() throws IOException {
        realWriteChars(cb.slice()); // 将cb position后的内容作为新的缓冲序列输出出去
        clear(cb);
    }


    /**
     * 先将字符缓冲的内容转换到字节缓冲中，再调用字节缓冲写入
     * 方法将字节缓冲区的数据写入到输出流中
     *
     * @see OutputBuffer#flushByteBuffer()
     *
     * @param from 写入的缓冲
     * @exception IOException 可能抛出I/O异常
     */
    public void realWriteChars(CharBuffer from) throws IOException {
        while (from.remaining() > 0) {
            conv.convert(from, bb);
            if (bb.remaining() == 0) {
                break;
            }

            if (from.remaining() > 0) {
                flushByteBuffer();
            } else if (conv.isUndeflow() && bb.limit() > bb.capacity() - 4) {
                // Tomcat 9中对段代码的解释
                // Handle an edge case. There are no more chars to write at the
                // moment but there is a leftover character in the converter
                // which must be part of a surrogate pair. The byte buffer does
                // not have enough space left to output the bytes for this pair
                // once it is complete )it will require 4 bytes) so flush now to
                // prevent the bytes for the leftover char and the rest of the
                // surrogate pair yet to be written from being lost.
                // See TestOutputBuffer#testUtf8SurrogateBody()
                flushByteBuffer();
            }
        }
    }

    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {

    }


    // ------------------------------ 输入到输出流 ------------------------------  

    /**
     * 如果字节缓冲没有数据，直接把追加的数据全部添加到字节缓冲中。
     * 如果字节缓冲可以填满就填满并发送出去。发送了，src中还有多
     * 余的，就将多余的填充到字节缓冲中。
     * 
     * @param src 追加的数据
     * @param off 偏移量
     * @param len 添加的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void append(byte src[], int off, int len) throws IOException {
        if (bb.remaining() == 0) {
            appendByteArray(src, off, len);
        } else {
            int n = transfer(src, off, len, bb);
            len = len - n;
            off = off + n;
            if (len > 0 && isFull(bb)) {
                flushByteBuffer();
                appendByteArray(src, off, len);
            }
        }
    }


    /**
     * 添加数据到字符缓冲区
     * 
     * @param src 追加的字符数据
     * @param off 偏移量
     * @param len 数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void append(char src[], int off, int len) throws IOException {
        // 如果字符缓冲区剩余的空间刚好可以装下追加
        // 的数据，那么直接追加到后面后直接返回
        if(len <= cb.capacity() - cb.limit()) {
            transfer(src, off, len, cb);
            return;
        }

        // 如果需要的容量不到字符缓冲区最大值的两倍，只需要填满并发送
        // 一次后，剩下的数据就可以直接添加到缓冲区中。如果不满足前面
        // 的条件，则将字符缓冲区和追加数据全部发送出去。
        if(len * 1L + cb.limit() < 2L * cb.capacity()) {
            int n = transfer(src, off, len, cb);
            flushCharBuffer();
            transfer(src, off + n, len - n, cb);
        } else {
            flushCharBuffer();
            realWriteChars(CharBuffer.wrap(src, off, len));
        }
    }

    public void append(ByteBuffer from) throws IOException {
        if (bb.remaining() == 0) {
            appendByteBuffer(from);
        } else {
            transfer(from, bb);
            if (from.hasRemaining() && isFull(bb)) {
                flushByteBuffer();
                appendByteBuffer(from);
            }
        }
    }

    private void appendByteArray(byte src[], int off, int len) throws IOException {
        if (len == 0) {
            return;
        }

        int limit = bb.capacity();
        while (len > limit) {
            realWriteBytes(ByteBuffer.wrap(src, off, limit));
            len = len - limit;
            off = off + limit;
        }

        if (len > 0) {
            transfer(src, off, len, bb);
        }
    }

    private void appendByteBuffer(ByteBuffer from) throws IOException {
        if (from.remaining() == 0) {
            return;
        }

        int limit = bb.capacity();
        int fromLimit = from.limit();
        while (from.remaining() > limit) {
            from.limit(from.position() + limit);
            realWriteBytes(from.slice());
            from.position(from.limit());
            from.limit(fromLimit);
        }

        if (from.remaining() > 0) {
            transfer(from, bb);
        }
    }

    private void transfer(byte b, ByteBuffer to) {
        toWriteMode(to);
        to.put(b);
        toReadMode(to);
    }

    private void transfer(char b, CharBuffer to) {
        toWriteMode(to);
        to.put(b);
        toReadMode(to);
    }

    private int transfer(byte[] buf, int off, int len, ByteBuffer to) {
        toWriteMode(to);
        int max = Math.min(len, to.remaining());
        if (max > 0) {
            to.put(buf, off, max);
        }
        toReadMode(to);
        return max;
    }

    private int transfer(char[] buf, int off, int len, CharBuffer to) {
        toWriteMode(to);
        int max = Math.min(len, to.remaining());
        if (max > 0) {
            to.put(buf, off, max);
        }
        toReadMode(to);
        return max;
    }

    private int transfer(String s, int off, int len, CharBuffer to) {
        toWriteMode(to);
        int max = Math.min(len, to.remaining());
        if (max > 0) {
            to.put(s, off, off + max);
        }
        toReadMode(to);
        return max;
    }

    private void transfer(ByteBuffer from, ByteBuffer to) {
        toWriteMode(to);
        int max = Math.min(from.remaining(), to.remaining());
        if (max > 0) {
            int fromLimit = from.limit();
            from.limit(from.position() + max);
            to.put(from);
            from.limit(fromLimit);
        }
        toReadMode(to);
    }

    private boolean isFull(Buffer buffer) {
        return buffer.limit() == buffer.capacity();
    }

    private void toReadMode(Buffer buffer) {
        buffer.limit(buffer.position())
                .reset();
    }

    private void toWriteMode(Buffer buffer) {
        buffer.mark()
                .position(buffer.limit())
                .limit(buffer.capacity());
    }

}
