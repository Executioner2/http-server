package com.ranni.container.scope;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 14:53
 */
public class ApplicationContextFacade implements ServletContext {
    private ServletContext context;

    public ApplicationContextFacade(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    @Override
    public ServletContext getContext(String s) {
        return context.getContext(s);
    }

    @Override
    public int getMajorVersion() {
        return context.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return context.getMinorVersion();
    }

    @Override
    public String getMimeType(String s) {
        return context.getMimeType(s);
    }

    @Override
    public Set getResourcePaths(String s) {
        return context.getResourcePaths(s);
    }

    @Override
    public URL getResource(String s) throws MalformedURLException {
        return context.getResource(s);
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        return context.getResourceAsStream(s);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return context.getRequestDispatcher(s);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return context.getNamedDispatcher(s);
    }

    @Override
    public Servlet getServlet(String s) throws ServletException {
        return context.getServlet(s);
    }

    @Override
    public Enumeration getServlets() {
        return context.getServlets();
    }

    @Override
    public Enumeration getServletNames() {
        return context.getServletNames();
    }

    @Override
    public void log(String s) {
        context.log(s);
    }

    @Override
    public void log(Exception e, String s) {
        context.log(e, s);
    }

    @Override
    public void log(String s, Throwable throwable) {
        context.log(s, throwable);
    }

    @Override
    public String getRealPath(String s) {
        return context.getRealPath(s);
    }

    @Override
    public String getServerInfo() {
        return context.getServerInfo();
    }

    @Override
    public String getInitParameter(String s) {
        return context.getInitParameter(s);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return context.getInitParameterNames();
    }

    @Override
    public Object getAttribute(String s) {
        return context.getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public void setAttribute(String s, Object o) {
        context.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        context.removeAttribute(s);
    }

    @Override
    public String getServletContextName() {
        return context.getServletContextName();
    }
}
