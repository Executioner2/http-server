package com.ranni.coyote;

import com.ranni.util.http.MimeHeaders;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Title: HttpServer
 * Description:
 * 封装socket输出流，缓冲等一系列底层
 * 实现的类。
 * 并非HttpServletResponse实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 20:07
 */
public final class Response {
    
    
    public void doWrite(ByteBuffer buf) throws IOException {
    }

    public void setErrorException(IOException ioe) {
    }

    public boolean isCommitted() {
        return false;
    }

    public long getContentLengthLong() {
        return 0;
    }

    public Request getRequest() {
        return null;
    }

    public void setContentLength(int length) {
        
    }

    public void action(ActionCode close, Object o) {
        
    }

    public int getStatus() {
        return 0;
    }

    public void sendHeaders() {
        
    }

    public boolean isExceptionPresent() {
        return false;
    }

    public Exception getErrorException() {
        return null;
    }

    public Charset getCharset() {
        return null;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public boolean isReady() {
        return false;
    }

    public void setWriteListener(WriteListener listener) {
        
    }

    public WriteListener getWriteListener() {
        return null;
    }

    public void checkRegisterForWrite() {
    }

    public MimeHeaders getMimeHeaders() {
        return null;
    }
}
