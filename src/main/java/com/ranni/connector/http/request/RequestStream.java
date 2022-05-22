package com.ranni.connector.http.request;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Title: HttpServer
 * Description: ServletInputStream的包装类，增加了数据大小限制
 * XXX - 未完整实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:06
 */
public class RequestStream extends ServletInputStream {
    private boolean closed = false; // 流是否关闭
    private int count;
    private int length = -1; // 请求体内容长度
    private InputStream input;

    public RequestStream(RequestBase request) {
        super();
        this.count = 0;
        this.length = request.getContentLength();
        this.input = request.getStream();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * 只读取不超出length的部分
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("request流已关闭，不能再读取数据！");
        int toRead = len;
        if (length > -1) {
            if (count >= length) return -1;
            if (toRead + count >= length) toRead = length - count;
        }

        int actuallyRead = super.read(b, off, toRead);
        if (actuallyRead >= 0) count += actuallyRead;
        return actuallyRead;
    }

    /**
     * 读取一个字节的数据
     * @return
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("request流已关闭，不能再读取数据！");
        if (length > -1 && count >= length) return -1;
        int b = input.read();
        if (b >= 0) count++;
        return b;
    }

    /**
     * 逻辑上关闭流
     * 注意：如果关闭了InputStream或OutputStream都将关闭socket
     * 所以即使读取完了InputStream中的数据，也只能逻辑上关闭流
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (closed) throw new IOException("request流已关闭，不能重复关闭！");
        if (length > 0) {
            // 把流中剩余的数据全部扔掉
            while (count < length) {
                int b = read();
                if (b < 0) break;
            }
        }
        closed = true;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }
}
