package com.ranni.coyote;

import java.io.IOException;
import java.nio.ByteBuffer;

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
}
