package com.ranni.connector;

import com.ranni.coyote.ActionCode;
import com.ranni.coyote.CloseNowException;
import com.ranni.coyote.Constants;
import com.ranni.coyote.CoyoteAdapter;
import com.ranni.coyote.Response;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.C2BConverter;

import javax.servlet.WriteListener;
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


    /**
     * 检查编码转换器。如果编码转换器（属性名conv）不为null，就直接返回。如果编
     * 码转换器为null，则先按以下顺序依次尝试获取编码方式（Charset的实例）：
     * 1、coyoteResponse中获取
     * 2、若coyoteResponse没有，则设置为默认的编码方式
     *    {@link com.ranni.coyote.Constants#DEFAULT_BODY_CHARSET}
     * 取得编码方式后从编码缓存中取得编码转换器，如果缓存中没有编码缓存器，则创建
     * 一个新的编码缓存器。
     *
     * @throws IOException 有可能抛出I/O异常
     */
    public void checkConverter() throws IOException {
        if (conv != null) {
            return;
        }
        
        Charset charset = null;
        
        if (coyoteResponse != null) {
            charset = coyoteResponse.getCharset();
        }
        
        if (charset == null) {
            if (coyoteResponse.getCharacterEncoding() != null) {
                // 测试编码是否正常，如果不正常则抛出异常，
                charset = B2CConverter.getCharset(coyoteResponse.getCharacterEncoding());
            }
            charset = Constants.DEFAULT_BODY_CHARSET;
        }
        
        conv = encoders.get(charset);
        if (conv == null) {
            conv = createConverter(charset);
            encoders.put(charset, conv);
        }
    }
    

    /**
     * 创建一个编码转换器
     *
     * @param charset 编码方式
     * @return 返回创建的编码转换器
     */
    private static C2BConverter createConverter(final Charset charset) {
        // XXX - 缺少安全检查
        return new C2BConverter(charset);
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
    
    
    public void write(byte b[], int off, int len) throws IOException {
        if (suspended) {
            return;
        }
        
        writeBytes(b, off, len);
    }
    
    
    public void write(ByteBuffer from) throws IOException {
        if (suspended) {
            return;
        }
        
        writeBytes(from);
    }


    private void writeBytes(byte b[], int off, int len) throws IOException {
        if (closed) {
            return;
        }
        
        append(b, off, len);
        bytesWritten += len;
        
        if (doFlush) {
            flushByteBuffer();
        }
    }
    
    
    private void writeBytes(ByteBuffer from) throws IOException {
        if (closed) {
            return;
        }

        // Tomcat 9（后面的版本可能也有这个问题） 中把下面
        // 两行代码搞反了，导致不能正确计算出已写入的数据量
        bytesWritten += from.remaining();
        append(from);
        
        if (doFlush) {
            flushByteBuffer();
        }
    }
    
    
    public void writeByte(int b) throws IOException {
        if (suspended) {
            return;
        }
        
        if (isFull(bb)) {
            flushByteBuffer();
        }
        
        transfer((byte) b, bb);
        bytesWritten++;
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
    public void write(int c) throws IOException {

        if (suspended) {
            return;
        }

        if (isFull(cb)) {
            flushCharBuffer();
        }

        transfer((char) c, cb);
        charsWritten++;
    }
    
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (suspended) {
            return;
        }
        
        append(cbuf, off, len);
        charsWritten += len;
    }

    
    @Override
    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    
    @Override
    public void write(String str) throws IOException {
        if (str == null) {
            str = "null";
        }
        
        write(str, 0, str.length());
    }

    
    @Override
    public void write(String str, int off, int len) throws IOException {
        if (suspended) {
            return;
        }
        
        if (str == null) {
            throw new NullPointerException("传入的字符串不能为null！");
        }
        
        int sOff = off;
        int sEnd = off + len;
        while (sOff < sEnd) {
            int n = transfer(str, off, sEnd - sOff, cb);
            sOff += n;
            if (sOff < sEnd && isFull(cb)) {
                flushCharBuffer();
            }
        }
        
        charsWritten += len;
    }


    // ------------------------------ 输入到输出流 ------------------------------  

    
    public long getContentWritten() {
        return bytesWritten + charsWritten;
    }


    /**
     * 这个输入缓冲区是否使用过。根据写入的字节和字符是否都为0来
     * 判断。如果都为0，则表示这是个未使用的输出缓冲区。可以通过
     * {@link #recycle()}来初始化实例属性
     * 
     * @return 返回使用状态
     */
    public boolean isNew() {
        return bytesWritten == 0 && charsWritten == 0;
    }


    public void setBufferSize(int size) {
        if (size > bb.capacity()) {
            bb = ByteBuffer.allocate(size);
            clear(bb);
        }
    }


    public void reset() {
        reset(false);
    }


    /**
     * 清空缓冲区
     * 
     * @param resetWriterStreamFlags 是否对实例属性进行重置
     */
    public void reset(boolean resetWriterStreamFlags) {
        clear(bb);
        clear(cb);
        bytesWritten = 0;
        charsWritten = 0;
        if (resetWriterStreamFlags) {
            if (conv != null) {
                conv.recycle();
            }
            conv = null;
        }
        initial = true;
    }


    public int getBufferSize() {
        return bb.capacity();
    }


    public boolean isReady() {
        return coyoteResponse.isReady();
    }


    public void setWriteListener(WriteListener listener) {
        coyoteResponse.setWriteListener(listener);
    }


    public boolean isBlocking() {
        return coyoteResponse.getWriteListener() == null;
    }


    public void checkRegisterForWrite() {
        coyoteResponse.checkRegisterForWrite();
    }
    
    
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

        // 如果缓冲区已有数据长度加上追加的数据长度不到字符缓冲区最大
        // 值的两倍，只需要填满并发送一次后，剩下的数据就可以直接添加
        // 到缓冲区中。如果不满足前面的条件，则将字符缓冲区和追加数据
        // 全部发送出去。
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
