package com.ranni.buf;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 字符块
 * XXX - 后续可能有更改
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 16:17
 */
public final class CharChunk extends AbstractChunk implements CharSequence {
    private static final long serialVersionUID = 1L;

    /**
     * 字符型输入通道
     */
    public interface CharInputChannel {

        /**
         * 读取字符
         *
         * @return 返回成功读取的数据长度
         * @throws IOException 可能抛出I/O异常
         */
        int realReadChars() throws IOException;
    }

    /**
     * 字符型输出通道
     */
    public interface CharOutputChannel {

        /**
         * 写入字符数组
         *
         * @param buf 要写入的字符数组
         * @param off 要写入的字符数组的起始位置
         * @param len 要写入的字符长度
         * @throws IOException 可能抛出I/O异常
         */
        void realWriteChars(char buf[], int off, int len) throws IOException;
    }


    private char[] buff;

    private transient CharInputChannel in = null;
    private transient CharOutputChannel out = null;

    
    public CharChunk() {
    }


    public CharChunk(int initial) {
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
                throw new IllegalArgumentException("字符块初始化参数异常！ initial = " + initial);
            }
            buff = new char[initial];
        }
        setLimit(limit);
        start = 0;
        end = 0;
        isSet = true;
        hasHashCode = false;
    }

    
    public void setChars(char[] c, int off, int len) {
        buff = c;
        start = off;
        end = start + len;
        isSet = true;
        hasHashCode = false;
    }


    /**
     * @return 返回缓冲区
     */
    public char[] getChars() {
        return getBuffer();
    }


    /**
     * @return 返回缓冲区
     */
    public char[] getBuffer() {
        return buff;
    }


    /**
     * 设置字符输入通道
     * 
     * @param in 字符输入通道
     */
    public void setCharInputChannel(CharInputChannel in) {
        this.in = in;
    }


    /**
     * 设置字符输出通道
     * 
     * @param out 字符输出通道
     */
    public void setCharOutputChannel(CharOutputChannel out) {
        this.out = out;
    }


    public void append(char c) throws IOException {
        makeSpace(1);
        int limit = getLimitInternal();

        if (end >= limit) {
            flushBuffer();
        }
        buff[end++] = c;
    }


    public void append(CharChunk src) throws IOException {
        append(src.getBuffer(), src.getOffset(), src.getLength());
    }


    /**
     * 添加数据到此字符块的空间内。如果字符块容不下这么长的数据，就直接发送出去。
     *
     * @param src 要添加的数据
     * @param off 要添加的数据的偏移量（src数组中数据的起始位置）
     * @param len 要添加的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void append(char src[], int off, int len) throws IOException {
        makeSpace(len);
        int limit = getLimitInternal();

        // 被追加的数据刚好有字符块全部空间那么大且字符块中没有数据，直接发送出去
        if (len == limit && end == start && out != null) {
            out.realWriteChars(src, off, len);
            return;
        }

        // 缓冲区有富余的空间容下要追加的数据，那么复制到此字符块的end后面
        if (len <= limit - end) {
            System.arraycopy(src, off, buff, end, len);
            end += len;
            return;
        }
        
        if (len + end < 2 * limit) {
            int avail = limit - end;
            System.arraycopy(src, off, buff, end, avail);
            end += avail;

            flushBuffer();

            System.arraycopy(src, off + avail, buff, end, len - avail);
            end += len - avail;

        } else {
            flushBuffer();

            out.realWriteChars(src, off, len);
        }
    }


    /**
     * 整个字符串都添加到缓冲区
     * 
     * @param s 要添加的字符串
     * @throws IOException 可能抛出I/O异常
     */
    public void append(String s) throws IOException {
        append(s, 0, s.length());
    }


    /**
     * 添加数据到此字符块的空间内。如果字符块容不下这么长的数据，就直接发送出去。
     *
     * @param s 要添加的数据
     * @param off 要添加的数据的偏移量（s字符串数据的起始位置）
     * @param len 要添加的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public void append(String s, int off, int len) throws IOException {
        if (s == null) {
            return;
        }

        makeSpace(len);
        int limit = getLimitInternal();

        int sOff = off;
        int sEnd = off + len;
        while (sOff < sEnd) {
            int d = min(limit - end, sEnd - sOff);
            s.getChars(sOff, sOff + d, buff, end);
            sOff += d;
            end += d;
            if (end >= limit) {
                flushBuffer();
            }
        }
    }
    
    
    public int subtract() throws IOException {
        if (checkEof()) {
            return -1;
        }
        return buff[start++];
    }
    

    public int subtract(char dest[], int off, int len) throws IOException {
        if (checkEof()) {
            return -1;
        }
        int n = len;
        if (len > getLength()) {
            n = getLength();
        }
        System.arraycopy(buff, start, dest, off, n);
        start += n;
        return n;
    }


    private boolean checkEof() throws IOException {
        if ((end - start) == 0) {
            if (in == null) {
                return true;
            }
            int n = in.realReadChars();
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
            throw new IOException("输出通道为空！无法刷新缓冲数据！");
        }
        
        out.realWriteChars(buff, start, end - start);
        end = start;
    }


    /**
     * 扩容，扩容上限如果有指定那就是limit，
     * 否则扩容上限为{@link AbstractChunk#ARRAY_MAX_SIZE}
     *
     * @param count 需要的空间
     */
    public void makeSpace(int count) {
        char[] tmp = null;

        int limit = getLimitInternal();

        long newSize;
        long desiredSize = end + count;

        if (desiredSize > limit) {
            desiredSize = limit;
        }

        // 有可能此方法被调用时还未分配字符块空间
        if (buff == null) {
            if (desiredSize < 256) {
                desiredSize = 256; // 最小大小
            }
            buff = new char[(int) desiredSize];
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
        tmp = new char[(int) newSize];

        System.arraycopy(buff, 0, tmp, 0, end);
        buff = tmp;
    }


    // -------------------- Conversion and getters --------------------

    @Override
    public String toString() {
        if (isNull()) {
            return null;
        } else if (end - start == 0) {
            return "";
        }
        return toStringInternal();
    }


    public String toStringInternal() {
        return new String(buff, start, end - start);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CharChunk) {
            return equals((CharChunk) obj);
        }
        return false;
    }


    /**
     * 不忽略大小写比较
     *
     * @param s 参与比较的字符串
     * @return 内容相等返回true，否则返回false
     */
    public boolean equals(String s) {
        char[] c = buff;
        int len = end - start;
        if (c == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (c[off++] != s.charAt(i)) {
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
        char[] c = buff;
        int len = end - start;
        if (c == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower(c[off++]) != Ascii.toLower(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    public boolean equals(CharChunk cc) {
        return equals(cc.getChars(), cc.getOffset(), cc.getLength());
    }


    public boolean equals(char b2[], int off2, int len2) {
        char b1[] = buff;
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

    
    public boolean startsWith(String s) {
        char[] c = buff;
        int len = s.length();
        if (c == null || len > end - start) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (c[off++] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }


    public boolean startsWithIgnoreCase(String s, int pos) {
        char[] c = buff;
        int len = s.length();
        if (c == null || len + pos > end - start) {
            return false;
        }
        int off = start + pos;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower(c[off++]) != Ascii.toLower(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    
    public boolean endsWith(String s) {
        char[] c = buff;
        int len = s.length();
        if (c == null || len > end - start) {
            return false;
        }
        int off = end - len;
        for (int i = 0; i < len; i++) {
            if (c[off++] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected int getBufferElement(int index) {
        return buff[index];
    }


    public int indexOf(char c) {
        return indexOf(c, start);
    }

    
    public int indexOf(char c, int starting) {
        int ret = indexOf(buff, start + starting, end, c);
        return (ret >= start) ? ret - start : -1;
    }

    
    public static int indexOf(char chars[], int start, int end, char s) {
        int offset = start;

        while (offset < end) {
            char c = chars[offset];
            if (c == s) {
                return offset;
            }
            offset++;
        }
        return -1;
    }

    
    private int min(int a, int b) {
        if (a < b) {
            return a;
        }
        return b;
    }


    // ------------------------------ 字符序列化实现 ------------------------------

    @Override
    public char charAt(int index) {
        return buff[index + start];
    }


    @Override
    public CharSequence subSequence(int start, int end) {
        try {
            CharChunk result = (CharChunk) this.clone();
            result.setOffset(this.start + start);
            result.setEnd(this.start + end);
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }


    @Override
    public int length() {
        return end - start;
    }
    
}
