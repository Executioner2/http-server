package com.ranni.connector.http.request;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:09
 */
@Deprecated
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
        return ((HttpServletRequest) request).getSession();
    }

    @Override
    public String changeSessionId() {
        return ((HttpServletRequest) request).changeSessionId();
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

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return ((HttpServletRequest) request).authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        ((HttpServletRequest) request).login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        ((HttpServletRequest) request).logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return ((HttpServletRequest) request).getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return ((HttpServletRequest) request).getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return ((HttpServletRequest) request).upgrade(handlerClass);
    }

    @Override
    public long getContentLengthLong() {
        return request.getContentLengthLong();
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return request.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return request.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return request.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return request.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return request.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return request.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return request.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return request.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return request.getDispatcherType();
    }
}
