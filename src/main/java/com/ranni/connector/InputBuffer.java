package com.ranni.connector;

import com.ranni.coyote.ActionCode;
import com.ranni.coyote.Constants;
import com.ranni.coyote.Request;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.collections.SynchronizedStack;

import javax.servlet.ReadListener;
import java.io.IOException;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title: HttpServer
 * Description:
 * 输入缓冲区
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 21:04
 * @Ref org.apache.catalina.connector.InputBuffer
 */
public class InputBuffer extends Reader 
        implements ByteChunk.ByteInputChannel, ApplicationBufferHandler {
    
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024; // 默认8KB

    // 编码缓存
    private static final Map<Charset, SynchronizedStack<B2CConverter>> encoders = new ConcurrentHashMap<>();
    
    // 类型（也可称作输入缓冲是哪种状态）
    public final int INITIAL_STATE = 0;
    public final int CHAR_STATE = 1;
    public final int BYTE_STATE = 2;

    protected B2CConverter conv; // 字节转字符工具
    
    // 缓冲区
    private ByteBuffer bb;
    private CharBuffer cb;
    
    private int state; // 缓冲区状态（初始状态，字符缓冲状态，字节缓冲状态）
    private boolean closed; // 关闭标志位
    
    private Request coyoteRequest; // coyote中的请求封装
    private int markPos = -1; // 缓冲位置
    private int readLimit; // 读取边界
    private final int size; // 缓冲区大小

    
    public InputBuffer() {
        this(DEFAULT_BUFFER_SIZE);
    }
    

    /**
     * 实例化输入缓冲
     * 
     * @param size 缓冲大小
     */
    public InputBuffer(int size) {
        this.size = size;
        bb = ByteBuffer.allocate(size);
        clear(bb);
        cb = CharBuffer.allocate(size);
        clear(cb);    
        readLimit = size;
    }


    
    // ------------------------------ 通用方法 ------------------------------
    
    private void clear(Buffer buffer) {
        buffer.rewind().limit(0);
    }
    
    
    public void setRequest(Request coyoteRequest) {
        this.coyoteRequest = coyoteRequest;
    }


    /**
     * 属性复位
     * 复位的属性：
     * state = INITIAL_STATE
     * 如果cb边界值大于此输入缓冲的大小，那么重新分配为此缓冲的大小
     * 复位cb
     * 读分界重置为size
     * closed = false
     * conv不为null的话先将此编码放入编码缓存集合中再置为null
     */
    public void recycle() {
        state = INITIAL_STATE;
        
        if (cb.capacity() > size) {
            cb = CharBuffer.allocate(size);
        }
        
        clear(cb);
        readLimit = size;
        markPos = -1;
        clear(bb);
        closed = false;
        
        if (conv != null) {
            conv.recycle();
            encoders.get(conv.getCharset()).push(conv);
            conv = null;
        }
    }

    
    public int available() {
        int available = availableInThisBuffer();
        if (available == 0) {
            coyoteRequest.action(ActionCode.AVAILABLE, 
                    Boolean.valueOf(coyoteRequest.getReadListener() != null));
            available = coyoteRequest.getAvailable() > 0 ? 1 : 0;
        }        
        return available;
    }
    

    /**
     * @return 返回缓冲区可用大小
     */
    private int availableInThisBuffer() {
        if (state == BYTE_STATE) {
            return bb.remaining();
        } else if (state == CHAR_STATE) {
            return cb.remaining();
        }
        
        return 0;
    }


    public void setReadListener(ReadListener listener) {
        coyoteRequest.setReadListener(listener);
    }


    public boolean isFinished() {
        int available = 0;
        if (state == BYTE_STATE) {
            available = bb.remaining();
        } else if (state == CHAR_STATE) {
            available = cb.remaining();
        }
        if (available > 0) {
            return false;
        } else {
            return coyoteRequest.isFinished();
        }
    }


    public boolean isReady() {
        if (coyoteRequest.getReadListener() == null) {
            return false;
        }

        if (isFinished()) {
            if (!coyoteRequest.isRequestThread()) {
                coyoteRequest.action(ActionCode.DISPATCH_READ, null);
                coyoteRequest.action(ActionCode.DISPATCH_EXECUTE, null);
            }
            return false;
        }

        if (availableInThisBuffer() > 0) {
            return true;
        }

        return coyoteRequest.isReady();
    }



    /**
     * @return 返回是否阻塞，是否阻塞是根据CoyoteRequest是否有读监听器来判断
     */
    public boolean isBlocking() {
        return coyoteRequest.getReadListener() == null;
    }


    @Override
    public void expand(int size) {

    }


    /**
     * 异常关闭
     * 
     * @throws IOException 如果缓冲区被关闭将抛出I/O异常
     */
    private void throwIfClosed() throws IOException {
        if (closed) {
            IOException ioe = new IOException("inputBuffer.streamClosed");
            coyoteRequest.setErrorException(ioe);
            throw ioe;
        }
    }


    @Override
    public void close() throws IOException {
        closed = true;
    }

    
    // ------------------------------ 字节缓冲区 ------------------------------

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        bb = buffer;
    }

    
    @Override
    public ByteBuffer getByteBuffer() {
        return bb;
    }
    

    /**
     * 读取字节到此缓冲区中
     * 
     * @return 返回读取的数量
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int realReadBytes() throws IOException {
        if (closed || coyoteRequest == null) {
            return -1;
        }
        
        if (state == INITIAL_STATE) {
            state = BYTE_STATE;
        }
        
        try {
            return coyoteRequest.doRead(this);
        } catch (IOException ioe) {
            coyoteRequest.setErrorException(ioe);
            throw ioe;
        }
    }


    /**
     * 从输入缓冲区中读取<strong>一个</strong>字节
     * 
     * @return 返回读取字节
     * @throws IOException 调用此方法被如果流缓冲区被关闭将抛出I/O异常
     */
    public int readByte() throws IOException {
        throwIfClosed(); // 如果被关闭抛出异常
        
        if (checkByteBufferEof()) {
            return -1;
        }
        
        return bb.get() & 0xFF;
    }


    /**
     * 从输入缓冲区中读取字节到数组中
     * 
     * @param b 要将数据存入的数组
     * @param off 偏移量，b的off开始往后存数据
     * @param len 期望读取的数据数量
     * @return 返回读取的真实数据量
     * @throws IOException 缓冲关闭将抛出I/O异常
     */
    public int read(byte[] b, int off, int len) throws IOException {
        throwIfClosed();
        
        if (checkByteBufferEof()) {
            return -1;
        }
        
        int n = Math.min(bb.remaining(), len);
        bb.get(b, off, n);
        return n;
    }


    /**
     * 从输入缓冲区中读取字节数据。读取到ByteBuffer类型的字节缓冲中
     * 读取之后移动to的position指针到读取前的位置，limit指针到to
     * 缓冲区有效数据末尾。即position到limit之间的数据为从输入缓冲
     * 区中读取到的数据。
     * 
     * @param to 承载输入缓冲区中拿取到的数据的字节缓冲区
     * @return 返回读取的真实数据量
     * @throws IOException 缓冲关闭将抛出I/O异常
     */
    public int read(ByteBuffer to) throws IOException {
        throwIfClosed();
        
        if (checkByteBufferEof()) {
            return -1;
        }
        
        int n = Math.min(to.remaining(), bb.remaining());
        int limit = bb.limit();
        bb.limit(bb.position() + n);
        to.put(bb);
        bb.limit(limit);
        to.limit(to.position()).position(to.position() - n);
        return n;
    }
    
    
    /**
     * 如果缓冲区中已经没有字节可读了，将从输入流中取数据填充到缓冲区
     * 
     * @return 返回是否到达字节缓冲区的边界
     */
    private boolean checkByteBufferEof() throws IOException {
        
        return bb.remaining() == 0 && realReadBytes() < 0;
    }
    
    
    // ------------------------------ 字符缓冲区 ------------------------------

    /**
     * 检查编码转换器。如果编码转换器（属性名conv）不为null，就直接返回。如果编
     * 码转换器为null，则先按以下顺序依次尝试获取编码方式（Charset的实例）：
     * 1、coyoteRequest中获取
     * 2、若coyoteRequest没有，则设置为默认的编码方式
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
        if (coyoteRequest != null) {
            charset = coyoteRequest.getCharset();
        }

        if (charset == null) {
            charset = Constants.DEFAULT_BODY_CHARSET;
        }

        SynchronizedStack<B2CConverter> stack = encoders.get(charset);
        if (stack == null) {
            stack = new SynchronizedStack<>();
            encoders.put(charset, stack);
        }

        conv = stack.pop();
        if (conv == null) {
            conv = createConverter(charset);
        }
    }


    /**
     * 创建一个编码转换器
     *
     * @param charset 编码方式
     * @return 返回创建的编码转换器
     */
    private static B2CConverter createConverter(final Charset charset) {
        // XXX - 缺少安全检查
        return new B2CConverter(charset);
    }


    /**
     * 给字符缓冲区分配空间
     * 
     * @param count 希望分配的空间大小
     */
    private void makeSpace(int count) {
        int desiredSize = cb.limit() + count;
        if (desiredSize >= readLimit) {
            desiredSize = readLimit;
        }
        
        // 字符缓冲区还能装，直接返回
        if (desiredSize <= cb.capacity()) {
            return;
        }
        
        // 字符缓冲区不能装了，需要对字符缓冲区扩容，按两倍扩
        // 两倍都装不下，那么就是两倍加上需要的大小
        long newSize = 2 * cb.capacity();
        if (desiredSize >= newSize) {
            newSize += count;
        }
        
        if (newSize > readLimit) {
            newSize = readLimit;
        }

        CharBuffer tmp = CharBuffer.allocate((int) newSize);
        int oldPosition = cb.position(); // 保存之间读取到的位置
        cb.position(0);
        tmp.put(cb);
        tmp.flip(); // 切换到读模式
        tmp.position(oldPosition);
        cb = tmp;
    }
    
    
    private boolean checkCharBufferEof() throws IOException {
        
        return cb.remaining() == 0 && realReadChars() < 0;
    }
    

    /**
     * 从字符缓冲区中读取字符到字符数组中
     * 
     * @param cbuf 存入读取数据的字符数组
     * @param off 偏移量
     * @param len 期望读取的数据数量
     * @return 实际读取的数据数量
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        throwIfClosed();

        if (checkCharBufferEof()) {
            return -1;
        }

        int n = Math.min(cb.remaining(), len);
        cb.get(cbuf, off, n);
        return n;
    }


    /**
     * 从字节缓冲区中读取数据到字符缓冲区中
     * 
     * @return 返回读取到的数据量。-1表示没有数据可读了
     * @throws IOException 可能抛出I/O异常
     */
    public int realReadChars() throws IOException {
        checkConverter();
        
        boolean eof = false;
        
        if (bb.remaining() <= 0) {
            eof = realReadBytes() < 0;
        }
        
        if (markPos == -1) {
            clear(cb);
        } else {
            makeSpace(bb.remaining());    
            if (cb.capacity() == cb.limit() && bb.remaining() != 0) {
                clear(cb);
                markPos = -1;
            }
        }
        
        conv.convert(bb, cb, this, eof);
        if (cb.remaining() == 0 && eof) {
            return -1;
        } else  {
            return cb.remaining();
        }
    }


    /**
     * 从字符缓冲区中读取一个字符
     * 
     * @return 返回读取的字符
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        throwIfClosed();
        
        if (checkCharBufferEof()) {
            return -1;
        }
        
        return cb.get();
    }


    /**
     * 从字符缓冲区中读取字符到字符数组中
     * 
     * @param cbuf 要存入字符数据的数组
     * @return 返回读取的长度
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int read(char[] cbuf) throws IOException {
        
        return read(cbuf, 0, cbuf.length);
    }


    /**
     * 跳过读取一部分数据
     * 
     * @param n 跳过的数据数量
     * @return 实际跳过的数据数量
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public long skip(long n) throws IOException {
        throwIfClosed();
        
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        
        long nRead = 0;
        
        if (cb.remaining() >= n) {
            cb.position(cb.position() + (int) n);
            nRead = n;
        } else {
            while (nRead < n) {
                nRead += cb.remaining();
                cb.position(cb.limit());
                if (realReadBytes() < 0) {
                    break;
                }
            }    
        }
        
        return nRead;
    }


    /**
     * @return 字符缓冲区是否已经初始化 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public boolean ready() throws IOException {
        throwIfClosed();
        if (state == INITIAL_STATE) {
            state = CHAR_STATE;
        }
        
        return available() > 0;
    }


    @Override
    public boolean markSupported() {
        return true;
    }


    /**
     * 标记字符缓冲区读取到的位置
     * 增加读取边界
     * 如果cb进行过扩容（容量大于设定的缓冲区大小的两倍也符合条件）
     * 且可读内容不足一半时将对字符缓冲区进行压缩（读取的内容已经
     * 超过了一半）
     * 
     * @param readAheadLimit 增加的读取边界 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void mark(int readAheadLimit) throws IOException {
        throwIfClosed();
        
        if (cb.remaining() <= 0) {
            clear(cb);
        } else {
            // 如果cb进行过扩容（容量大于设定的缓冲区大小的两倍也符合条件）
            // 且读取的内容已经超过了一半，就进行压缩
            if (cb.capacity() > 2 * size
                && cb.remaining() < cb.position()) {
                cb.compact();
                cb.flip();
            }
        }
        
        readLimit = cb.position() + readAheadLimit + size;
        markPos = cb.position();
    }


    /**
     * 重置字符或字节缓冲区
     * 
     * 如果是字符缓冲区分以下两种情况：
     * 如果有标记位置，那么就退回到标记位置
     * 如果没有标记位置，那么就清空字符缓冲区并在请
     * 求中设置异常
     * 
     * 否则直接重置字节缓冲区
     * 
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void reset() throws IOException {
        throwIfClosed();

        if (state == CHAR_STATE) {
            if (markPos < 0) {
                clear(cb);
                markPos = -1;
                IOException ioe = new IOException();
                coyoteRequest.setErrorException(ioe);
                throw ioe;
            } else {
                cb.position(markPos);
            }
        } else {
            clear(bb);
        }
        
    }
        
}
