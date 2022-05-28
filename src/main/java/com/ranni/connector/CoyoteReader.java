package com.ranni.connector;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 输入缓冲区的读取器。大部分都是直接调用InputBuffer的方法
 * TODO:
 * XXX - 应该做一些安全工作的
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/24 14:41
 */
public class CoyoteReader extends BufferedReader {

    // ------------------------------ 属性字段 ------------------------------

    /**
     * 回车换行
     * '/r' 和 '/n'
     */    
    private static final int[] LINE_SEP = {13, 10};

    /**
     * 最大长度
     */
    private static final int MAX_LINE_LENGTH = 4096;
    

    /**
     * 输入缓冲区
     */
    protected InputBuffer ib;

    
    // ------------------------------ 构造方法 ------------------------------
    
    public CoyoteReader(InputBuffer ib) {
        super(ib, 1);
        this.ib = ib;
    }


    // ------------------------------ 基本方法 ------------------------------

    /**
     * 输入缓冲区置为null
     */
    void clear() {
        ib = null;
    }


    /**
     * @throws CloneNotSupportedException 不允许克隆
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    // ------------------------------ Reader ------------------------------


    @Override
    public void close() throws IOException {
        ib.close();
    }

    @Override
    public int read() throws IOException {
        return ib.read();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return ib.read(cbuf, off, len);
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return ib.read(cbuf);
    }

    @Override
    public long skip(long n) throws IOException {
        return ib.skip(n);
    }

    @Override
    public boolean markSupported() {
        return ib.markSupported();
    }

    
    /**
     * 标记当前位置，可传入一个限制读取的数据数量 
     * @param readAheadLimit 限制可读数量
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void mark(int readAheadLimit) throws IOException {
        ib.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        ib.reset();
    }


    /**
     * 读取一行并返回这一行的字符串</br>
     * 遇到<b>'/r'</b>或<b>'/n'</b>任意一个就可以结束了。
     * 
     * @return 返回读取一行的字符串
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        
        int count = 0; // 读取的数据量
        while (true) {
            if (count++ == 0) {
                mark(MAX_LINE_LENGTH);   
            }
            
            // 先读取一轮
            int val = read();
            
            // 读到边界了
            if (val < 0) {                
                break;
            }
            
            // 遇到结束回车或换行或回车换行的情况
            if (val == LINE_SEP[0]) {
                val = read();
                if (val != LINE_SEP[1]) {
                    reset();
                    skip(count);
                }
                
                return sb.toString();
            } else if (val == LINE_SEP[1]) {
                return sb.toString();
            }
            
            sb.append((char) val);
            
            if (count % MAX_LINE_LENGTH == 0) {
                count = 0;
            }
        }
        
        return sb.length() == 0 ? null : sb.toString();
    }
    
}
