package com.ranni.connector.http.response;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:29
 */
public class HttpResponseFacade extends ResponseFacade implements HttpServletResponse {
    public HttpResponseFacade(Response response) {
        super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).addCookie(cookie);
    }

    @Override
    public boolean containsHeader(String s) {
        return ((HttpServletResponse)servletResponse).containsHeader(s);
    }

    @Override
    public String encodeURL(String s) {
        return ((HttpServletResponse)servletResponse).encodeURL(s);
    }

    @Override
    public String encodeRedirectURL(String s) {
        return ((HttpServletResponse)servletResponse).encodeRedirectURL(s);
    }

    @Override
    public String encodeUrl(String s) {
        return encodeURL(s);
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return encodeRedirectURL(s);
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        response.setAppCommitted(true);
        ((HttpServletResponse)servletResponse).sendError(i, s);
    }

    @Override
    public void sendError(int i) throws IOException {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        response.setAppCommitted(true);
        ((HttpServletResponse)servletResponse).sendError(i);
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        if (isCommitted()) throw new IllegalStateException("响应头已提交！");
        response.setAppCommitted(true);
        ((HttpServletResponse)servletResponse).sendRedirect(s);
    }

    @Override
    public void setDateHeader(String s, long l) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).setDateHeader(s, l);
    }

    @Override
    public void addDateHeader(String s, long l) {
        setDateHeader(s, l);
    }

    @Override
    public void setHeader(String s, String s1) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).setHeader(s, s1);
    }

    @Override
    public void addHeader(String s, String s1) {
        setHeader(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).setIntHeader(s, i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        setIntHeader(s, i);
    }

    @Override
    public void setStatus(int i) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).setStatus(i);
    }

    @Override
    public void setStatus(int i, String s) {
        if (isCommitted()) return;
        ((HttpServletResponse)servletResponse).setStatus(i, s);
    }

    @Override
    public int getStatus() {
        return ((HttpServletResponse) servletResponse).getStatus();
    }

    @Override
    public String getHeader(String name) {
        return ((HttpServletResponse) servletResponse).getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return ((HttpServletResponse) servletResponse).getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return ((HttpServletResponse) servletResponse).getHeaderNames();
    }

    @Override
    public String getContentType() {
        return servletResponse.getContentType();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        servletResponse.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLengthLong(long len) {
        servletResponse.setContentLengthLong(len);
    }
}
