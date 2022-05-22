package com.ranni.connector.http.response;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:28
 */
public class ResponseFacade implements ServletResponse {
    protected Response response; // 响应对象
    protected ServletResponse servletResponse; // servlet 响应对象



    public ResponseFacade(Response response) {
        this.response = response;
        this.servletResponse = (ServletResponse)response;
    }

    @Override
    public String getCharacterEncoding() {
        return servletResponse.getCharacterEncoding();
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return servletResponse.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return servletResponse.getWriter();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        servletResponse.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int i) {
        if (isCommitted()) return;
        servletResponse.setContentLength(i);
    }

    @Override
    public void setContentLengthLong(long len) {
        servletResponse.setContentLengthLong(len);
    }

    @Override
    public void setContentType(String s) {
        if (isCommitted()) return;
        servletResponse.setContentType(s);
    }

    @Override
    public void setBufferSize(int i) {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        servletResponse.setBufferSize(i);
    }

    @Override
    public int getBufferSize() {
        return servletResponse.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        if (response.isSuspended()) return;
        response.setAppCommitted(true);
        servletResponse.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        servletResponse.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return servletResponse.isCommitted();
    }

    @Override
    public void reset() {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        servletResponse.reset();
    }

    @Override
    public void setLocale(Locale locale) {
        if (isCommitted()) return;
        servletResponse.setLocale(locale);
    }

    @Override
    public Locale getLocale() {
        return servletResponse.getLocale();
    }
}
