package com.ranni.connector.http.response;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:29
 */
public class ResponseStream extends ServletOutputStream {
    protected boolean suspended; // 挂起标志
    protected OutputStream output; // 标准输出流
    protected boolean commit; // 刷新流时是否自动提交响应
    protected int count; // 缓冲区中的数据长度

    public ResponseStream(ResponseBase response) {

        this.output = response.getStream();
    }

    @Override
    public void write(int b) throws IOException {

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
        return false;
    }

    /**
     * 刷新流时自动提交响应
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
}
