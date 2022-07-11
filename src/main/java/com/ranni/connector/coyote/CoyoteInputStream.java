package com.ranni.connector.coyote;

import com.ranni.connector.InputBuffer;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Title: HttpServer
 * Description:
 * 输入缓冲区转为的输入流
 * TODO:
 * XXX - 应该做一些安全工作的
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/24 12:01
 * @Ref org.apache.catalina.connector.CoyoteInputStream
 */
public class CoyoteInputStream extends ServletInputStream {
    
    // ------------------------------ 属性字段 ------------------------------
    
    /**
     * 输入缓冲区
     */
    protected InputBuffer ib;

    
    // ------------------------------ 构造方法 ------------------------------
    
    public CoyoteInputStream(InputBuffer inputBuffer) {
        this.ib = inputBuffer;
    }


    // ------------------------------ 基本方法 ------------------------------

    /**
     * 清空输入缓冲区 
     */
    public void clear() {
        ib = null;
    }


    /**
     * @return 如果返回<b>true</b>，则表示缓冲区读完了
     */
    @Override
    public boolean isFinished() {
        return ib.isFinished();
    }


    /**
     * @throws CloneNotSupportedException 不允许克隆
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }


    /**
     * @return 如果返回<b>true</b>，则表示可以进行数据读取
     */
    @Override
    public boolean isReady() {
        if (ib == null) {
            return false;
        }
        
        return ib.isReady();
    }


    /**
     * 设置读监听
     * 
     * @param readListener 读监听器
     */
    @Override
    public void setReadListener(ReadListener readListener) {   
        ib.setReadListener(readListener);
    }


    private void checkNonBlockingRead() {
        if (!ib.isBlocking() && !ib.isReady()) {
            throw new IllegalStateException("非阻塞读未准备好！");
        }
    }


    /**
     * 关闭流
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void close() throws IOException {
        ib.close();
    }


    /**
     * 
     * @return 返回流可用数据
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int available() throws IOException {
        return ib.available();
    }


    // ------------------------------ 数据读取 ------------------------------

    /**
     * 读取一个字节的数据
     * 
     * @return 返回读取的一个字节的数据
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int read() throws IOException {
        checkNonBlockingRead();
        return ib.read();
    }


    /**
     * 读取字节到一个字节数组中
     * 
     * @param b 要存入数据的字节数组
     * @return 返回读取的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }


    /**
     * 读取字节到一个字节数组中
     * 
     * @param b 要存入数据的字节数组
     * @param off 偏移量
     * @param len 数据长度
     * @return 返回读取的数据长度 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkNonBlockingRead();
        return ib.read(b, off, len);
    }


    /**
     * 读取数据到一个缓冲区中去
     * 
     * @param bb 要存入的缓冲区
     * @return 返回读取的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    public int read(final ByteBuffer bb) throws IOException {
        checkNonBlockingRead();        
        return ib.read(bb);
    }
}
