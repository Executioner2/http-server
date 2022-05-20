package com.ranni.connector.http.response;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Title: HttpServer
 * Description:
 * 这个流和RequestStream相似，只是对ServletOutputStream的封装
 * XXX - 未完整实现 
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:29
 */
public class ResponseStream extends ServletOutputStream {
    protected boolean suspended; // 挂起标志
    protected OutputStream output; // 标准输出流
    protected boolean commit; // 刷新流时是否提交数据
    protected int count; // 缓冲区中的数据长度
    protected boolean close; // 流已关闭
    protected int length = -1; // 要限制发送的长度, -1不限制
    protected Response response;

    public ResponseStream(Response response) {
        this(response, -1);
    }

    public ResponseStream(Response response, int length) {
        this.response = response;
        this.output = response.getStream();
        this.suspended = response.isSuspended();
        this.length = length;
    }

    /**
     * 写入数据到流中
     * @param b
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        if (suspended) return;

        if (close) throw new IOException("响应流已关闭！");

        if (length > -1 && count >= length) throw new IOException("已超出发送数据字节数限制！");

        ((ResponseBase) response).write(b);
        count++;
    }

    /**
     * 写入数据到流中
     * @param b
     * @throws IOException
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * 写入数据到流中
     * 超出部分丢弃
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (suspended) return;

        if (close) throw new IOException("响应流已关闭！");

        int actual = len;
        if (length > -1 && count + len >= length) {
            actual = length - count;
        }

        ((ResponseBase) response).write(b, off, actual);
        count += actual;
        if (actual < len) throw new IOException("未能完全发送完传入数据！");
    }

    /**
     * 刷新响应流中的数据并提交数据
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        if (suspended) throw new IOException("响应流已挂起！");

        if (close) throw new IOException("响应流已关闭！");

        if (commit)
            response.getResponse().flushBuffer();
    }

    /**
     * 提交响应数据并逻辑上关闭流
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (suspended) throw new IOException("响应流已挂起！");

        if (close) throw new IOException("响应流已关闭！");

        response.getResponse().flushBuffer();
        close = true;
    }

    /**
     * 设置挂起标志
     * @param suspended
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * 响应流是否关闭
     * @return
     */
    public boolean closed() {
        return this.close;
    }

    /**
     * 刷新流时提交数据
     * @param b
     */
    public void setCommit(boolean b) {
        this.commit = b;
    }

    /**
     * 清除缓冲区中所有内容
     */
    public void reset() {
        this.count = 0;
    }

//    @Override
//    public boolean isReady() {
//        return false;
//    }
//
//    @Override
//    public void setWriteListener(WriteListener writeListener) {
//
//    }
}
