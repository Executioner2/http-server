package com.ranni.processor.stream;

import com.ranni.processor.http.HttpResponse;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 这个流和RequestStream相似，只是对ServletOutputStream的封装
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 21:35
 */
public class ResponseStream extends ServletOutputStream {
    private HttpResponse response;
    private boolean closed;
    private boolean commit; // 是否允许调用flush刷新提交buffer的内容

    public ResponseStream() {
    }

    public ResponseStream(HttpResponse response) {
        super();
        this.response = response;
        this.closed = false;
        this.commit = false;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) throw new IOException("response流已关闭，不能再写入！");
        this.response.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("response流已关闭，不能再写入！");
        this.response.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (closed) throw new IOException("response流已经关闭，不能再刷新！");
        if (isCommit()) response.flushBuffer();

    }

    @Override
    public void close() throws IOException {
        if (closed) throw new IOException("response流已经关闭，不能重复关闭！");
        response.flushBuffer();
        closed = true;
    }

    public void setCommit(boolean b) {
        this.commit = b;
    }

    public boolean isCommit() {
        return this.commit;
    }
}
