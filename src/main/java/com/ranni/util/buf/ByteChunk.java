package com.ranni.util.buf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Title: HttpServer
 * Description:
 * 字节型数据块
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/21 19:06
 */
public final class ByteChunk extends AbstractChunk {
    

    /**
     * 字节型输入通道
     */
    public interface ByteInputChannel {
        
        /**
         * 读取数据
         * 
         * @return 返回读取的数据长度
         * @throws IOException 可能抛出I/O异常
         */
        int realReadBytes() throws IOException;
    }


    /**
     * 字节型输出通道
     */
    public interface ByteOutputChannel {

        /**
         * 写入字节数组
         * 
         * @param buf 要写入的字节数组
         * @param off 要写入的字节数组的起始位置
         * @param len 要写入的字节长度
         * @throws IOException 可能抛出I/O异常
         */
        void realWriteBytes(byte buf[], int off, int len) throws IOException;


        /**
         * 从字节缓冲区中写入数据
         * 
         * @param from 被写入的字节缓冲区
         * @throws IOException 可能抛出I/O异常
         */
        void realWriteBytes(ByteBuffer from) throws IOException;
    }

    private static final long serialVersionUID = 1L;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1; // 默认的编码格式

    private byte[] buff; // 字节缓冲区
    private Charset charset; // 字符编码格式
    private ByteInputChannel in; // 字节输入通道
    private ByteOutputChannel out; // 字节输出通道
    
    
    public ByteChunk() { }


    /**
     * 实例化一个字节块并分配块大小
     * 
     * @param initial
     */
    public ByteChunk(int initial) {
        allocate(initial, -1);
    }
    

    /**
     * 分配空的块空间
     * 
     * @param initial 块初始大小
     * @param limit 空间大小上限
     */
    public void allocate(int initial, int limit) {
        if (buff == null || buff.length < initial) {
            if (initial < 0 || initial > ARRAY_MAX_SIZE) {
                throw new IllegalArgumentException("字节块初始化参数异常！ initial = " + initial);
            }
            buff = new byte[initial];
        }
        
        setLimit(limit);
        start = 0;
        end = 0;
        isSet = true;
        hasHashCode = false;
    }


    /**
     * 直接设置字节块，和allocate是同一级别不同类型的方法
     * 
     * @param b 设置的字节块
     * @param off 设置的起始位置
     * @param len 设置的长度
     */
    public void setBytes(byte[] b, int off, int len) {
        buff = b;
        start = off;
        end = start + len;
        isSet = true;
        hasHashCode = false;
    }


    /**
     * 设置编码格式
     * 
     * @param charset 编码格式
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }


    /**
     * 返回编码格式。
     * 如果未设置编码格式，则返回默认的ISO_8859_1标准编码
     * 
     * @return 返回的编码格式
     */
    public Charset getCharset() {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        
        return charset;
    }


    /**
     * @return 返回缓冲区
     */
    public byte[] getBytes() {
        return getBuffer();
    }


    /**
     * @return 返回缓冲区
     */
    public byte[] getBuffer() {
        return buff;
    }


    /**
     * 设置字节输入通道
     * 
     * @param in 字节输入通道
     */
    public void setByteInputChannel(ByteInputChannel in) {
        this.in = in;
    }


    /**
     * 设置字节输出通道
     * 
     * @param out 字节输出通道
     */
    public void setByteOutputChannel(ByteOutputChannel out) {
        this.out = out;
    }
    

    // ------------------------------ 数据拼接 ------------------------------

    
    /**
     * 将此字节块中的数据拼接到dest的off之后，拼接的数量取dest的len与此字节块长度的最小
     * 值。拼接过去的数量记作n，如果拼接成功，此字节块的start增加n，表示已经被取出了n个字
     * 节。
     * 
     * @param dest 要拼接的字节数组
     * @param off 偏移量
     * @param len 拼接的长度
     * @return 返回拼接过去的字节数量，-1为未拼接
     * @throws IOException 可能抛出I/O异常
     */
    public int subtract(byte dest[], int off, int len) throws IOException {
        if (checkEof()) {
            return -1;
        }
        
        int n = Math.min(len, getLength());
        System.arraycopy(buff, start, dest, off, n);
        start += n;
        return n;
    }
    

    /**
     * 将此字节块中的数据拼接到to的position之后，拼接的数量取to的
     * 可用容量与此字节块长度的最小值。拼接过去的数量记作n，如果拼接
     * 成功，此字节块的start增加n，表示已经被取出了n个字节。
     * 
     * @param to 要拼接到的字节缓冲区
     * @return 返回拼接过去的字节数量，-1为未拼接
     * @throws IOException 可能抛出I/O异常
     */
    public int subtract(ByteBuffer to) throws IOException {
        if (checkEof()) {
            return -1;
        }
        
        int n = Math.min(to.remaining(), getLength());
        to.put(buff, start, n);
        to.limit(to.position());
        to.position(to.position() - n);
        start += n;
        return n;
    }


    /**
     * 读取一个字节
     * 
     * @return 返回读取的一个字节
     * @throws IOException
     */
    public byte subtractB() throws IOException {
        if (checkEof()) {
            return -1;
        }
        return buff[start++];
    }
    

    // ------------------------------ 添加数据到字节块 ------------------------------

    
    /**
     * 追加数据到缓冲区。如果缓冲区已满就通过输出通道发送缓
     * 冲区中的数据。
     *
     * @param b 追加的一个字节数据
     * @throws IOException 可能抛出I/O异常
     */
    public void append(byte b) throws IOException {
        makeSpace(1);
        int limit = getLimitInternal();

        // 刷新缓冲区
        if (end >= limit) {
            flushBuffer();
        }
        buff[end++] = b;
    }
    
    
    public void append(ByteChunk src) throws IOException {
        append(src.getBytes(), src.start, src.getLength());
    }


    /**
     * 添加数据到此字节块的空间内。如果字节块容不下这么长的数据，就直接发送出去。
     * 
     * @param src 要添加的数据
     * @param off 要添加的数据的偏移量（src数组中数据的起始位置）
     * @param len 要添加的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void append(byte src[], int off, int len) throws IOException {
        makeSpace(len);
        
        int limit = getLimitInternal();
        
        // 被追加的数据刚好有字节块全部空间那么大且字节块中没有数据，直接发送出去
        if (len == limit && end == start && out != null) {
            out.realWriteBytes(src, off, len);
            return;
        }
        
        // 缓冲区有富余的空间容下要追加的数据，那么复制到此字节块的end后面
        if (len <= limit - end) {
            System.arraycopy(src, off, buff, end, len);
            end += len;
            return;
        }
        
        // 缓冲区没有富余的空间容纳全部追加的数据
        // 需要先把缓冲区填满然后发送缓冲区中的数据，再把剩余的数据放入缓冲区
        
        int surplus = limit - end;
        System.arraycopy(src, off, buff, end, surplus);
        end += surplus;
        
        flushBuffer();
        
        int remain = len - surplus;
        
        while (remain > limit - end) {
            out.realWriteBytes(src, off + len - remain, limit - end);
            remain = remain - limit + end;
        }

        System.arraycopy(src, off + len - remain, buff, end, remain);
        end += remain;
    }


    /**
     * 从字节缓冲区中添加数据到字节块中。调用此方法前应该先调用字节缓冲区的
     * {@link ByteBuffer#flip()}方法使字节缓冲区处于读模式。
     * 
     * @param from 被添加数据的字节缓冲区
     * @throws IOException 可能抛出I/O异常
     */
    public void append(ByteBuffer from) throws IOException {
        // 被读取的数据长度，默认调用此方法前已经调用过ByteBuffer的flip方法
        int len = from.remaining(); 
        
        int limit = getLimitInternal();
        
        // from可读数据长度刚好和缓冲区大小一致且字节块中没有数据，直接将from中的缓冲区数据发送出去
        if (len == limit && start == end && out != null) {
            out.realWriteBytes(from);
            from.position(len);
            return;
        }
        
        // 可以直接添加到字节块end后
        if (len <= limit - end) {
            from.get(buff, end, len);
            end += len;
            return;
        }

        // 缓冲区没有富余的空间容纳全部追加的数据
        // 需要先把缓冲区填满然后发送缓冲区中的数据，再把剩余的数据放入缓冲区
        
        int surplus = limit - end;
        from.get(buff, end, surplus);
        end += surplus;
        
        flushBuffer();
        
        int fromLimit = from.limit();
        int remain = fromLimit - surplus;
        surplus = limit - end;
        while (remain >= surplus) {
            from.limit(from.position() + surplus);
            out.realWriteBytes(from);
            from.position(from.limit());
            remain = remain - surplus;
        }
        
        from.limit(fromLimit);
        from.get(buff, end, remain);
        end += remain;        
    }
    
    
    // ------------------------------ 功能性方法 ------------------------------


    /**
     * 写入对象
     * 
     * @param oos 被写入的对象
     * @throws IOException 可能抛出I/O异常
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeUTF(getCharset().name());
    }


    /**
     * 读取对象
     * 
     * @param ois 要读取的对象
     * @throws ClassNotFoundException 可能抛出类未找到异常
     * @throws IOException 可能抛出I/O异常
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.charset = Charset.forName(ois.readUTF());
    }
    

    @Override
    public void recycle() {
        super.recycle();
        charset = null;
    }
    

    /**
     * 边界检查
     *
     * @return 是否已经到达缓冲区边界
     * @throws IOException 可能抛出I/O异常
     */
    private boolean checkEof() throws IOException {
        if ((end - start) == 0) {
            if (in == null) {
                return true;
            }
            int n = in.realReadBytes();
            if (n < 0) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * 刷新缓冲区，将缓冲区中的数据从发送通道发送出去 
     */
    public void flushBuffer() throws IOException {
        if (out == null) {
            throw new BufferOverflowException();
        }

        out.realWriteBytes(buff, start, end - start);
        end = start;
    }
    

    /**
     * 扩容，扩容上限如果有指定那就是limit，
     * 否则扩容上限为{@link AbstractChunk#ARRAY_MAX_SIZE}
     * 
     * @param count 需要的空间
     */
    public void makeSpace(int count) {
        byte[] tmp = null;
        
        int limit = getLimitInternal();
        
        long newSize;
        long desiredSize = end + count;
        
        if (desiredSize > limit) {
            desiredSize = limit;
        }
        
        // 有可能此方法被调用时还未分配字节块空间
        if (buff == null) {
            if (desiredSize < 256) {
                desiredSize = 256; // 最小大小
            }
            buff = new byte[(int) desiredSize];
        }
        
        // 需要的空间小于缓冲区大小，不需要扩容，直接返回
        if (desiredSize <= buff.length) {
            return;
        }
        
        // 如果不超过两倍按两倍扩容
        // 超过两倍按两倍加超出的部分扩容
        if (desiredSize < 2L * buff.length) {
            newSize = buff.length * 2L;
        } else {
            newSize = buff.length * 2L + count;
        }
        
        if (newSize > limit) {
            newSize = limit;
        }
        
        tmp = new byte[(int) newSize];

        System.arraycopy(buff, start, tmp, 0, end - start);
        buff = tmp;
        end = end - start;
        start = 0;        
    }


    /**
     * 字符串转字节数组
     * 
     * @param value 被转换的字符串
     * @return 返回转换后的字节数组
     */
    public static final byte[] convertToBytes(String value) {
        byte[] result = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            result[i] = (byte) value.charAt(i);
        }
        return result;
    }
    
    
    // ------------------------------ toString ------------------------------

    
    @Override
    public String toString() {
        if (isNull()) {
            return null;
        } else if (end - start == 0){
            return "";
        }
        
        // XXX - 这里应该引入缓存工具
        return toStringInternal();
    }

    public String toStringInternal() {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }

        CharBuffer cb = charset.decode(ByteBuffer.wrap(buff, start, end - start));
        return new String(cb.array(), start, end - start);
    }

    public long getLong() {
        return Ascii.parseLong(buff, start, end - start);
    }
    
    
    // ------------------------------ 数据匹配 ------------------------------
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteChunk) {
            return equals((ByteChunk) obj);
        }
        return false;
    }

    public boolean equals(ByteChunk bb) {
        return equals(bb.getBytes(), bb.getStart(), bb.getLength());
    }

    public boolean equals(byte b2[], int off2, int len2) {
        byte b1[] = buff;
        if (b1 == null && b2 == null) {
            return true;
        }

        int len = end - start;
        if (len != len2 || b1 == null || b2 == null) {
            return false;
        }

        int off1 = start;

        while (len-- > 0) {
            if (b1[off1++] != b2[off2++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 不忽略大小写比较
     * 
     * @param s 参与比较的字符串
     * @return 内容相等返回true，否则返回false
     */
    public boolean equals(String s) {
        byte[] b = buff;
        int len = end - start;
        if (b == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (b[off++] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 忽略大小写的比较
     * 
     * @param s 参与比较的字符串
     * @return 内容相等返回true，不等返回false
     */
    public boolean equalsIgnoreCase(String s) {
        byte[] b = buff;
        int len = end - start;
        if (b == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower(b[off++]) != Ascii.toLower(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }



    public boolean startsWith(String s, int pos) {
        byte[] b = buff;
        int len = s.length();
        if (b == null || len + pos > end - start) {
            return false;
        }
        int off = start + pos;
        for (int i = 0; i < len; i++) {
            if (b[off++] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }


    public boolean startsWithIgnoreCase(String s, int pos) {
        byte[] b = buff;
        int len = s.length();
        if (b == null || len + pos > end - start) {
            return false;
        }
        int off = start + pos;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower(b[off++]) != Ascii.toLower(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    
    /**
     * 取得在字节缓冲中指定下标的字节
     *
     * @param index 下标
     * @return
     */
    @Override
    protected int getBufferElement(int index) {
        return buff[index];
    }


    public int indexOf(char c, int starting) {
        int ret = indexOf(buff, start + starting, end, c);
        return (ret >= start) ? ret - start : -1;
    }


    public static int indexOf(byte bytes[], int start, int end, char s) {
        return findByte(bytes, start, end, (byte) s);
    }


    public static int findByte(byte bytes[], int start, int end, byte b) {
        int offset = start;
        while (offset < end) {
            if (bytes[offset] == b) {
                return offset;
            }
            offset++;
        }
        return -1;
    }

    public static int findBytes(byte bytes[], int start, int end, byte b[]) {
        int offset = start;
        while (offset < end) {
            for (byte value : b) {
                if (bytes[offset] == value) {
                    return offset;
                }
            }
            offset++;
        }
        return -1;
    }
}
