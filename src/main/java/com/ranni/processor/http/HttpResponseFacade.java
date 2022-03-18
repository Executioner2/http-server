package com.ranni.processor.http;

import com.ranni.processor.stream.ResponseWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description: HttpResponse的外观类，供其它包的类使用
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 20:38
 */
public class HttpResponseFacade implements HttpServletResponse {
    private HttpResponse response;

    public HttpResponseFacade() {}

    public HttpResponseFacade(HttpResponse response) {
        this.response = response;
    }

    @Override
    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    @Override
    public boolean containsHeader(String s) {
        return response.containsHeader(s);
    }

    @Override
    public String encodeURL(String s) {
        return response.encodeURL(s);
    }

    @Override
    public String encodeRedirectURL(String s) {
        return response.encodeRedirectURL(s);
    }

    @Override
    public String encodeUrl(String s) {
        return response.encodeUrl(s);
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return response.encodeRedirectUrl(s);
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        response.sendError(i, s);
    }

    @Override
    public void sendError(int i) throws IOException {
        response.sendError(i);
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        response.sendRedirect(s);
    }

    @Override
    public void setDateHeader(String s, long l) {
        addDateHeader(s, l);
    }

    @Override
    public void addDateHeader(String s, long l) {
        response.addDateHeader(s, l);
    }

    @Override
    public void setHeader(String s, String s1) {
        addHeader(s, s1);
    }

    @Override
    public void addHeader(String s, String s1) {
        response.addHeader(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        addIntHeader(s, i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        response.addIntHeader(s, i);
    }

    @Override
    public void setStatus(int i) {
        response.setStatus(i);
    }

    @Override
    public void setStatus(int i, String s) {
        response.setStatus(i, s);
    }

    @Override
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public void setContentLength(int i) {
        response.setContentLength(i);
    }

    @Override
    public void setContentType(String s) {
        response.setContentType(s);
    }

    @Override
    public void setBufferSize(int i) {
        response.setBufferSize(i);
    }

    @Override
    public int getBufferSize() {
        return response.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void reset() {
        response.reset();
    }

    @Override
    public void setLocale(Locale locale) {
        response.setLocale(locale);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }
}
