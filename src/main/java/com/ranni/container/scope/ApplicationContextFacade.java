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

//    @Override
//    public String getContextPath() {
//        return context.getContextPath();
//    }

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

//    @Override
//    public int getEffectiveMajorVersion() {
//        return context.getEffectiveMajorVersion();
//    }
//
//    @Override
//    public int getEffectiveMinorVersion() {
//        return context.getEffectiveMinorVersion();
//    }

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

//    @Override
//    public boolean setInitParameter(String name, String value) {
//        return context.setInitParameter(name, value);
//    }

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

//    @Override
//    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
//        return context.addServlet(servletName, className);
//    }
//
//    @Override
//    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
//        return context.addServlet(servletName, servlet);
//    }
//
//    @Override
//    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
//        return context.addServlet(servletName, servletClass);
//    }

//    @Override
//    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
//        return context.addJspFile(servletName, jspFile);
//    }

//    @Override
//    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
//        return context.createServlet(clazz);
//    }
//
//    @Override
//    public ServletRegistration getServletRegistration(String servletName) {
//        return context.getServletRegistration(servletName);
//    }
//
//    @Override
//    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
//        return context.getServletRegistrations();
//    }
//
//    @Override
//    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
//        return context.addFilter(filterName, className);
//    }
//
//    @Override
//    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
//        return context.addFilter(filterName, filter);
//    }
//
//    @Override
//    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
//        return context.addFilter(filterName, filterClass);
//    }
//
//    @Override
//    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
//        return context.createFilter(clazz);
//    }
//
//    @Override
//    public FilterRegistration getFilterRegistration(String filterName) {
//        return context.getFilterRegistration(filterName);
//    }
//
//    @Override
//    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
//        return context.getFilterRegistrations();
//    }
//
//    @Override
//    public SessionCookieConfig getSessionCookieConfig() {
//        return context.getSessionCookieConfig();
//    }
//
//    @Override
//    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
//        context.setSessionTrackingModes(sessionTrackingModes);
//    }
//
//    @Override
//    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
//        return context.getDefaultSessionTrackingModes();
//    }
//
//    @Override
//    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
//        return context.getEffectiveSessionTrackingModes();
//    }
//
//    @Override
//    public void addListener(String className) {
//        context.addListener(className);
//    }
//
//    @Override
//    public <T extends EventListener> void addListener(T t) {
//        context.addListener(t);
//    }
//
//    @Override
//    public void addListener(Class<? extends EventListener> listenerClass) {
//        context.addListener(listenerClass);
//    }
//
//    @Override
//    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
//        return context.createListener(clazz);
//    }
//
//    @Override
//    public JspConfigDescriptor getJspConfigDescriptor() {
//        return context.getJspConfigDescriptor();
//    }
//
//    @Override
//    public ClassLoader getClassLoader() {
//        return context.getClassLoader();
//    }
//
//    @Override
//    public void declareRoles(String... roleNames) {
//        context.declareRoles(roleNames);
//    }
//
//    @Override
//    public String getVirtualServerName() {
//        return context.getVirtualServerName();
//    }

//    @Override
//    public int getSessionTimeout() {
//        return context.getSessionTimeout();
//    }
//
//    @Override
//    public void setSessionTimeout(int sessionTimeout) {
//        context.setSessionTimeout(sessionTimeout);
//    }
//
//    @Override
//    public String getRequestCharacterEncoding() {
//        return context.getRequestCharacterEncoding();
//    }
//
//    @Override
//    public void setRequestCharacterEncoding(String encoding) {
//        context.setRequestCharacterEncoding(encoding);
//    }
//
//    @Override
//    public String getResponseCharacterEncoding() {
//        return context.getResponseCharacterEncoding();
//    }
//
//    @Override
//    public void setResponseCharacterEncoding(String encoding) {
//        context.setRequestCharacterEncoding(encoding);
//    }
}
