package com.ranni.connector.http.request;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:09
 */
public final class HttpRequestFacade extends RequestFacade implements HttpServletRequest {
    public HttpRequestFacade(ServletRequest request) {
        super(request);
    }

    @Override
    public String getAuthType() {
        return ((HttpServletRequest)request).getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return ((HttpServletRequest)request).getCookies();
    }

    @Override
    public long getDateHeader(String s) {
        return ((HttpServletRequest)request).getDateHeader(s);
    }

    @Override
    public String getHeader(String s) {
        return ((HttpServletRequest)request).getHeader(s);
    }

    @Override
    public Enumeration getHeaders(String s) {
        return ((HttpServletRequest)request).getHeaders(s);
    }

    @Override
    public Enumeration getHeaderNames() {
        return ((HttpServletRequest)request).getHeaderNames();
    }

    @Override
    public int getIntHeader(String s) {
        return ((HttpServletRequest)request).getIntHeader(s);
    }

    @Override
    public String getMethod() {
        return ((HttpServletRequest)request).getMethod();
    }

    @Override
    public String getPathInfo() {
        return ((HttpServletRequest)request).getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return ((HttpServletRequest)request).getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return ((HttpServletRequest)request).getContextPath();
    }

    @Override
    public String getQueryString() {
        return ((HttpServletRequest)request).getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return ((HttpServletRequest)request).getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s) {
        return ((HttpServletRequest)request).isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal() {
        return ((HttpServletRequest)request).getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return ((HttpServletRequest)request).getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return ((HttpServletRequest)request).getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return ((HttpServletRequest)request).getRequestURL();
    }

    @Override
    public String getServletPath() {
        return ((HttpServletRequest)request).getServletPath();
    }

    @Override
    public HttpSession getSession(boolean b) {
        return ((HttpServletRequest) request).getSession(b);
    }

    @Override
    public HttpSession getSession() {
        return ((HttpServletRequest)request).getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return ((HttpServletRequest)request).isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return ((HttpServletRequest)request).isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return ((HttpServletRequest)request).isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }
}
