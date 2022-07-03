package com.ranni.coyote;

import com.ranni.util.http.MimeHeaders;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

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


    private String contentType;

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

    public void setContentLength(long length) {
        
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

    public String getContentType() {
        return null;
    }

    public Object getNote(int pos) {
        return null;
    }

    public void setNote(int pos, com.ranni.connector.Response response) {
        
    }

    public void setHeader(String name, String value) {
    }

    public void addHeader(String name, String value) {
    }

    public void addHeader(String name, String value, Charset charset) {
    }

    public void setHook(AbstractProcessor abstractProcessor) {
    }

    public void recycle() {
    }

    public void setCommitted(boolean b) {
    }

    public boolean isError() {
        return false;
    }


    /**
     * @return 
     */
    public boolean setError() {
        return false;
    }

    public boolean isErrorReportRequired() {
        return false;
    }

    public boolean setErrorReported() {
        return false;
    }

    public int getContentLength() {
        return 0;
    }

    public void setStatus(int sc) {
        
    }

    public void setMessage(String msg) {
        
    }

    public void setContentType(String type) {
        if (type == null) {
            this.contentType = null;
            return;
        }
    }

    public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException {
        if (isCommitted()) {
            return;
        }
        
        if (characterEncoding == null) {
            
        }
    }

    public void setContentTypeNoCharset(String type) {
    }

    public String getMessage() {
        return null;
    }

    public void reset() {
    }

    public void setLocale(Locale loc) {
    }

    public Locale getLocale() {
        return null;
    }
}
