package com.ranni.container.scope;

import com.ranni.connector.Constants;
import com.ranni.container.Context;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.naming.Resource;
import com.ranni.util.Enumerator;

import javax.naming.directory.DirContext;
import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * servlet的全局作用域
 * XXX - 未完整实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 18:03
 */
public class ApplicationContext implements ServletContext {
    private static final List empty = new ArrayList(); // 统一空集合

    private StandardContext context;
    private String basePath; // 根路径
    private Map attributes = new HashMap(); // 属性
    private Set<String> readOnlyAttributes = new HashSet<>(); // 只读属性名
    private ServletContext facade = new ApplicationContextFacade(this); // 外观类
    private Map<String, String> parameters = null; // 参数


    public ApplicationContext(String basePath, StandardContext context) {
        super();
        this.basePath = basePath;
        this.context = context;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    /**
     * 取得ServletContext容器
     * 0、返回null
     * 1、如果请求的是当前全局作用域则直接返回this
     * 2、如果允许跨servlet访问转到3，否则转到0
     * 3、如果能从与当前全局作用域绑定的context容器的父容器找到其请求的子容器则返回该子容器的全局作用域，否则转到0
     *
     * @param uri
     * @return
     */
    @Override
    public ServletContext getContext(String uri) {
        if (uri == null || !uri.startsWith("/"))
            return null;

        // 如果请求的是当前全局作用域
        String contextPath = context.getPath();
        if (!contextPath.endsWith("/"))
            contextPath = contextPath + "/";
        if (uri.startsWith(contextPath))
            return this;

        // 如果不允许跨servlet则返回null
        if (!context.getCrossContext())
            return null;

        // 从父容器（Host）中找到uri对应的子容器
        // 如果子容器不为空则返回对应的全局作用域
        Host parent = (Host) context.getParent();
        Context child = parent.map(uri);
        if (child != null)
            return child.getServletContext();
        else
            return null;
    }

    /**
     * 取得主要版本号
     *
     * @return
     */
    @Override
    public int getMajorVersion() {
        return Constants.MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String s) {
        return null;
    }

    @Override
    public Set getResourcePaths(String s) {
        return null;
    }


    /**
     * 取得资源文件并以URL方式返回
     * 
     * @param s
     * @return
     * @throws MalformedURLException
     */
    @Override
    public URL getResource(String s) throws MalformedURLException {

        DirContext resources = context.getResources();
        if (resources != null) {
//            String fullPath = context.getName() + s;            
            String fullPath = s;            
            String hostName = context.getParent().getName();

            try {
                // 文件是否存在于JNDI容器中
                resources.lookup(s);
                
                return new URL("jndi", null, 0, 
                        getJNDIUri(hostName, fullPath));
                
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        
        return null;
    }


    /**
     * 返回资源在JNDI容器中的uri
     * FIXME - 虚拟主机名需要做区分
     * 
     * @param hostName
     * @param path
     * @return
     */
    public static String getJNDIUri(String hostName, String path) {
//        if (!path.startsWith("/"))
//            return "/" + hostName + "/" + path;
//        else
//            return "/" + hostName + path;
        
        if (!path.startsWith("/"))
            return "/" + path;
        
        return path;
    }
    

    /**
     * 取得资源文件并以输入流的方式返回
     * 
     * @param s
     * @return
     */
    @Override
    public InputStream getResourceAsStream(String s) {
        
        DirContext resources = context.getResources();
        if (resources != null) {
            try {
                Object lookup = resources.lookup(s);
                if (lookup instanceof Resource) {
                    // 是文件
                    return ((Resource) lookup).streamContent();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    @Override
    public Servlet getServlet(String s) throws ServletException {
        return null;
    }

    @Override
    public Enumeration getServlets() {
        return null;
    }

    @Override
    public Enumeration getServletNames() {
        return null;
    }

    @Override
    public void log(String s) {

    }

    @Override
    public void log(Exception e, String s) {

    }

    @Override
    public void log(String s, Throwable throwable) {

    }

    /**
     * 取得真实路径
     * 如果JNDI容器不是文件目录容器就直接返回null
     *
     * @param s
     * @return
     */
    @Override
    public String getRealPath(String s) {
        if (!context.isFilesystemBased())
            return null;
        File file = new File(basePath, s);
        return file.getAbsolutePath();
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Enumeration getInitParameterNames() {
        return null;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    /**
     * 取得指定的属性值
     *
     * @param s
     * @return
     */
    @Override
    public Object getAttribute(String s) {
        synchronized (attributes) {
            return attributes.get(s);
        }
    }

    /**
     * 取得属性名集合的迭代器
     * @return
     */
    @Override
    public Enumeration getAttributeNames() {
        synchronized (attributes) {
            return new Enumerator(attributes.keySet());
        }
    }

    /**
     * 设置属性值
     * 不允许有空的name
     * 如果传入的value为null将移除name
     *
     * @param name
     * @param value
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (name == null)
            throw new IllegalArgumentException("ApplicationContext.setAttribute:  name不能为null");

        if (value == null) {
            removeAttribute(name);
            return;
        }

        synchronized (attributes) {
            attributes.put(name, value);
        }

        // TODO 通知监听器
    }

    /**
     * 移除指定的可读写属性
     *
     * @param s
     */
    @Override
    public void removeAttribute(String s) {
        if (readOnlyAttributes.contains(s))
            return;

        synchronized (attributes) {
            attributes.remove(s);
        }

        // TODO 通知监听器
    }

    /**
     * 取得servlet全局作用域对象的名字
     * @return
     */
    @Override
    public String getServletContextName() {
        return context.getDisplayName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.EMPTY_SET;
    }

    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {

    }

    /**
     * 将原有属性改为只读属性
     *
     * @param name
     */
    public void setAttributeReadOnly(String name) {
        synchronized (attributes) {
            if (attributes.containsKey(name))
                readOnlyAttributes.add(name);
        }
    }

    /**
     * 返回外观类
     *
     * @return
     */
    public ServletContext getFacade() {
        if (facade == null)
            facade = new ApplicationContextFacade(this);

        return facade;
    }


    /**
     * 清空全部可读写属性
     * 不删除只读属性
     */
    public void clearAttributes() {
        ArrayList list = new ArrayList();
        synchronized (attributes) {
            Iterator iter = attributes.keySet().iterator();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
        }
        
        Iterator keys = list.iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            removeAttribute(key);
        }
    }
}
