package com.ranni.container;

import com.ranni.core.FilterDef;
import com.ranni.deploy.*;
import com.ranni.util.CharsetMapper;

import javax.servlet.ServletContext;

/**
 * Title: HttpServer
 * Description:
 * ServletContext的处理类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:50
 */
public interface Context extends Container {
    /**
     * 加载事件
     */
    String RELOAD_EVENT = "reload";


    /**
     * 获取servlet context
     *
     * @return
     */
    ServletContext getServletContext();


    /**
     * 获取字符编码
     *
     * @return
     */
    CharsetMapper getCharsetMapper();


    /**
     * 返回已初始化的应用程序事件监听器
     *
     * @return
     */
    Object[] getApplicationListeners();


    /**
     * 设置应用程序事件监听器
     *
     * @param listeners
     */
    void setApplicationListeners(Object listeners[]);


    /**
     * 返回此context容器可用标志
     *
     * @return
     */
    boolean getAvailable();


    /**
     * 设置此context容器可以标志
     *
     * @param available
     */
    void setAvailable(boolean available);


    /**
     * 设置本地字符编码
     *
     * @param mapper
     */
    void setCharsetMapper(CharsetMapper mapper);


    /**
     * 返回此容器是否正确配置的标志位
     * 
     * @return
     */
    boolean getConfigured();


    /**
     * 设置此容器是否正确配置的标志位
     * 
     * @param configured
     */
    void setConfigured(boolean configured);


    /**
     * 返回将cookies用于session id的标志
     *
     * @return
     */
    boolean getCookies();


    /**
     * 设置是否将cookies用于session id
     *
     * @param cookies
     */
    void setCookies(boolean cookies);


    /**
     * 返回是否允许跨域的标志
     *
     * @return
     */
    boolean getCrossContext();


    /**
     * 设置跨域标志
     *
     * @param crossContext
     */
    void setCrossContext(boolean crossContext);


    /**
     * 返回此应用程序显示的名字
     *
     * @return
     */
    String getDisplayName();


    /**
     * 设置此应用程序显示的名字
     *
     * @param displayName
     */
    void setDisplayName(String displayName);


    /**
     * 返回此web应用程序是否允许分发的标志
     *
     * @return
     */
    boolean getDistributable();


    /**
     * 设置分发标志
     *
     * @param distributable
     */
    void setDistributable(boolean distributable);


    /**
     * 返回这个web应用程序的文档路径
     *
     * @return
     */
    String getDocBase();


    /**
     * 设置这个web应用程序的文档路径
     *
     * @param docBase
     */
    void setDocBase(String docBase);


    /**
     * Return the login configuration descriptor for this web application.
     */
//    public LoginConfig getLoginConfig();


    /**
     * Set the login configuration descriptor for this web application.
     *
     * @param config The new login configuration
     */
//    public void setLoginConfig(LoginConfig config);


    /**
     * 返回JNDI资源
     * 
     * @return
     */
    NamingResources getNamingResources();


    /**
     * 设置JNDI资源
     * 
     * @param namingResources
     */
    void setNamingResources(NamingResources namingResources);


    /**
     * 返回此web应用程序的上下文路径
     *
     * @return
     */
    String getPath();


    /**
     * 设置此web应用程序的上下文路径
     *
     * @param path
     */
    void setPath(String path);


    /**
     * 设置xml解析产生的系统id
     * 
     * @param systemId
     */
    void setSystemId(String systemId);


    /**
     * 返回xml解析产生的系统id
     * @return
     */
    String getSystemId();
    

    /**
     * 返回xml解析产生的公共id
     */
    String getPublicId();


    /**
     * 设置xml解析所产生的公共id
     *
     * @param publicId
     */
    void setPublicId(String publicId);


    /**
     * 返回可重载标志
     *
     * @return
     */
    boolean getReloadable();


    /**
     * 设置可重载标志
     *
     * @param reloadable
     */
    void setReloadable(boolean reloadable);

    /**
     * 返回重写标志
     *
     * @return
     */
    boolean getOverride();


    /**
     * 设置重写标志
     *
     * @param override
     */
    void setOverride(boolean override);


    /**
     * 此容器是否有特权
     *
     * @return
     */
    boolean getPrivileged();


    /**
     * 设置容器是否具备特权
     *
     * @param privileged
     */
    void setPrivileged(boolean privileged);


    /**
     * 返回session超时时限，默认以分钟为单位
     *
     * @return
     */
    int getSessionTimeout();


    /**
     * 设置session超时时限，默认以分钟为单位
     *
     * @param timeout
     */
    void setSessionTimeout(int timeout);


    /**
     * 返回在此context注册的wrapper实现类类名
     *
     * @return
     */
    String getWrapperClass();


    /**
     * 设置在此context注册的wrapper实现类类名
     *
     * @param wrapperClass
     */
    void setWrapperClass(String wrapperClass);


    /**
     * 添加应用程序监听器的类名
     *
     * @param listener
     */
    void addApplicationListener(String listener);

    /**
     * 添加应用程序监听器的类名
     *
     * @param listeners
     */
    void addApplicationListener(String[] listeners);


    /**
     * 添加应用程序参数
     *
     * @param parameter
     */
    void addApplicationParameter(ApplicationParameter parameter);


    /**
     * 添加应用程序参数
     *
     * @param parameters
     */
    void addApplicationParameter(ApplicationParameter[] parameters);


    /**
     * Add a security constraint to the set for this web application.
     */
//    public void addConstraint(SecurityConstraint constraint);


    /**
     * Add an EJB resources reference for this web application.
     *
     * @param ejb New EJB resources reference
     */
//    public void addEjb(ContextEjb ejb);


    /**
     * 添加容器环境
     *
     * @param environment
     */
    void addEnvironment(ContextEnvironment environment);



    /**
     * Add an error page for the specified error or Java exception.
     *
     * @param errorPage The error page definition to be added
     */
//    public void addErrorPage(ErrorPage errorPage);


    /**
     * 添加过滤器定义实例
     * 
     * @param filterDef
     */
    void addFilterDef(FilterDef filterDef);


    /**
     * 添加过滤器映射实例
     * 
     * @param filterMap
     */
    void addFilterMap(FilterMap filterMap);


    /**
     * 添加wrapper的实例监听器
     *
     * @param listener
     */
    void addInstanceListener(String listener);


    /**
     * 添加wrapper的实例监听器
     *
     * @param listeners
     */
    void addInstanceListener(String[] listeners);


    /**
     * Add a local EJB resources reference for this web application.
     *
     * @param ejb New local EJB resources reference
     */
//    public void addLocalEjb(ContextLocalEjb ejb);


    /**
     * Add a new MIME mapping, replacing any existing mapping for
     * the specified extension.
     *
//     * @param extension Filename extension being mapped
//     * @param mimeType Corresponding MIME type
     */
//    public void addMimeMapping(String extension, String mimeType);


    /**
     * 添加新的上下文参数，新的值将替换旧的值
     *
     * @param name
     * @param value
     */
    void addParameter(String name, String value);


    /**
     * 添加资源
     *
     * @param resource
     */
    void addResource(ContextResource resource);


    /**
     * 添加资源环境类型
     *
     * @param name
     * @param type
     */
    void addResourceEnvRef(String name, String type);


    /**
     * 添加资源连接
     *
     * @param resourceLink
     */
    void addResourceLink(ContextResourceLink resourceLink);


    /**
     * Add a security role reference for this web application.
     *
     * @param role Security role used in the application
     * @param link Actual security role to check for
     */
//    public void addRoleMapping(String role, String link);


    /**
     * Add a new security role for this web application.
     *
     * @param role New security role
     */
//    public void addSecurityRole(String role);


    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 添加pattern与wrapper对象的映射关系
     * @param pattern
     * @param name
     */
    void addServletMapping(String pattern, String name);


    /**
     * Add a JSP tag library for the specified URI.
     *
     * @param uri URI, relative to the web.xml file, of this tag library
     * @param location Location of the tag library descriptor
     */
//    public void addTaglib(String uri, String location);


    /**
     * Add a new welcome file to the set recognized by this Context.
     *
     * @param name New welcome file name
     */
//    public void addWelcomeFile(String name);


    /**
     * 添加wrapper的生命周期管理器
     *
     * @param listener
     */
    void addWrapperLifecycle(String listener);

    /**
     * 添加wrapper的生命周期管理器
     *
     * @param listeners
     */
    void addWrapperLifecycle(String[] listeners);


    /**
     * 添加wrapper监听器
     *
     * @param listener
     */
    void addWrapperListener(String listener);


    /**
     * 添加wrapper监听器
     *
     * @param listeners
     */
    void addWrapperListener(String[] listeners);


    /**
     * 工厂方法，创建并返回Wrapper容器
     * @return
     */
    Wrapper createWrapper();


    /**
     * 返回所有应用程序监听器
     *
     * @return
     */
    String[] findApplicationListeners();


    /**
     * 返回所有应用程序参数
     *
     * @return
     */
    ApplicationParameter[] findApplicationParameters();


    /**
     * Return the set of security constraints for this web application.
     * If there are none, a zero-length array is returned.
     */
//    public SecurityConstraint[] findConstraints();


    /**
     * Return the EJB resources reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resources reference
     */
//    public ContextEjb findEjb(String name);


    /**
     * Return the defined EJB resources references for this application.
     * If there are none, a zero-length array is returned.
     */
//    public ContextEjb[] findEjbs();


    /**
     * 返回指定的环境
     *
     * @param name
     * @return
     */
    ContextEnvironment findEnvironment(String name);


    /**
     * 返回所有环境
     *
     * @return
     */
    ContextEnvironment[] findEnvironments();


    /**
     * Return the error page entry for the specified HTTP error code,
     * if any; otherwise return <code>null</code>.
     *
     * @param errorCode Error code to look up
     */
//    public ErrorPage findErrorPage(int errorCode);


    /**
     * Return the error page entry for the specified Java exception type,
     * if any; otherwise return <code>null</code>.
     *
     * @param exceptionType Exception type to look up
     */
//    public ErrorPage findErrorPage(String exceptionType);



    /**
     * Return the set of defined error pages for all specified error codes
     * and exception types.
     */
//    public ErrorPage[] findErrorPages();


    /**
     * 返回指定的容器定义实例
     * 
     * @param filterName
     * @return
     */
    FilterDef findFilterDef(String filterName);


    /**
     * 返回此容器所有的过滤器定义实例
     * 
     * @return
     */
    FilterDef[] findFilterDefs();


    /**
     * 返回此容器所有过滤器映射
     * 
     * @return
     */
    FilterMap[] findFilterMaps();


    /**
     * 返回所有wrapper实例监听器
     *
     * @return
     */
    String[] findInstanceListeners();


    /**
     * Return the local EJB resources reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resources reference
     */
//    public ContextLocalEjb findLocalEjb(String name);


    /**
     * Return the defined local EJB resources references for this application.
     * If there are none, a zero-length array is returned.
     */
//    public ContextLocalEjb[] findLocalEjbs();


    /**
     * Return the MIME type to which the specified extension is mapped,
     * if any; otherwise return <code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    String findMimeMapping(String extension);


    /**
     * Return the extensions for which MIME mappings are defined.  If there
     * are none, a zero-length array is returned.
     */
    String[] findMimeMappings();


    /**
     * 返回指定name对应的参数value
     * @param name
     * @return
     */
    String findParameter(String name);


    /**
     * 返回所有参数
     * @return
     */
    String[] findParameters();


    /**
     * 返回指定的资源
     *
     * @param name
     * @return
     */
    ContextResource findResource(String name);


    /**
     * 返回指定的环境类型
     *
     * @param name
     * @return
     */
    String findResourceEnvRef(String name);


    /**
     * 返回所有环境类型
     *
     * @return
     */
    String[] findResourceEnvRefs();


    /**
     * 返回指定的资源连接
     *
     * @param name
     * @return
     */
    ContextResourceLink findResourceLink(String name);


    /**
     * 返回所有资源连接
     *
     * @return
     */
    ContextResourceLink[] findResourceLinks();


    /**
     * 返回所有资源
     *
     * @return
     */
    ContextResource[] findResources();


    /**
     * For the given security role (as used by an application), return the
     * corresponding role name (as defined by the underlying Realm) if there
     * is one.  Otherwise, return the specified role unchanged.
     *
     * @param role Security role to map
     */
//    public String findRoleMapping(String role);


    /**
     * Return <code>true</code> if the specified security role is defined
     * for this application; otherwise return <code>false</code>.
     *
     * @param role Security role to verify
     */
//    public boolean findSecurityRole(String role);


    /**
     * Return the security roles defined for this application.  If none
     * have been defined, a zero-length array is returned.
     */
//    public String[] findSecurityRoles();


    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 根据pattern找到具体的wrapper对象
     *
     * @param pattern
     * @return
     */
    String findServletMapping(String pattern);


    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 所以这个方法就是返回所有的key
     *
     * @return
     */
    String[] findServletMappings();


    /**
     * Return the context-relative URI of the error page for the specified
     * HTTP status code, if any; otherwise return <code>null</code>.
     *
     * @param status HTTP status code to look up
     */
//    public String findStatusPage(int status);


    /**
     * Return the set of HTTP status codes for which error pages have
     * been specified.  If none are specified, a zero-length array
     * is returned.
     */
//    public int[] findStatusPages();


    /**
     * Return the tag library descriptor location for the specified taglib
     * URI, if any; otherwise, return <code>null</code>.
     *
     * @param uri URI, relative to the web.xml file
     */
//    public String findTaglib(String uri);


    /**
     * Return the URIs of all tag libraries for which a tag library
     * descriptor location has been specified.  If none are specified,
     * a zero-length array is returned.
     */
//    public String[] findTaglibs();


    /**
     * Return <code>true</code> if the specified welcome file is defined
     * for this Context; otherwise return <code>false</code>.
     *
     * @param name Welcome file to verify
     */
//    public boolean findWelcomeFile(String name);


    /**
     * Return the set of welcome files defined for this Context.  If none are
     * defined, a zero-length array is returned.
     */
//    public String[] findWelcomeFiles();


    /**
     * 返回所有wrapper生命周期监听器类名
     *
     * @return
     */
    String[] findWrapperLifecycles();


    /**
     * 返回所有wrapper监听器
     *
     * @return
     */
    String[] findWrapperListeners();


    /**
     * 如果支持重新载入，则重新载入此web应用程序
     */
    void reload();


    /**
     * 移除指定的应用程序监听器
     *
     * @param listener
     */
    void removeApplicationListener(String listener);


    /**
     * 移除指定的应用程序参数
     *
     * @param name
     */
    void removeApplicationParameter(String name);


    /**
     * Remove the specified security constraint from this web application.
     *
     * @param constraint Constraint to be removed
     */
//    void removeConstraint(SecurityConstraint constraint);


    /**
     * Remove any EJB resources reference with the specified name.
     *
     * @param name Name of the EJB resources reference to remove
     */
//    public void removeEjb(String name);


    /**
     * Remove any environment entry with the specified name.
     *
     * @param name Name of the environment entry to remove
     */
//    public void removeEnvironment(String name);


    /**
     * Remove the error page for the specified error code or
     * Java language exception, if it exists; otherwise, no action is taken.
     *
     * @param errorPage The error page definition to be removed
     */
//    public void removeErrorPage(ErrorPage errorPage);


    /**
     * 移除此容器的过滤器定义
     * 
     * @param filterDef
     */
    void removeFilterDef(FilterDef filterDef);


    /**
     * 删除与此容关联的过滤器映射实例
     * 
     * @param filterMap
     */
    void removeFilterMap(FilterMap filterMap);


    /**
     * 移除指定的wrapper实例监听器类名
     *
     * @param listener
     */
    void removeInstanceListener(String listener);


    /**
     * Remove any local EJB resources reference with the specified name.
     *
     * @param name Name of the EJB resources reference to remove
     */
//    public void removeLocalEjb(String name);


    /**
     * Remove the MIME mapping for the specified extension, if it exists;
     * otherwise, no action is taken.
     *
//     * @param extension Extension to remove the mapping for
     */
//    public void removeMimeMapping(String extension);


    /**
     * 移除指定的参数
     *
     * @param name
     */
    void removeParameter(String name);


    /**
     * 移除指定资源
     *
     * @param name
     */
    void removeResource(String name);


    /**
     * 移除指定环境类型
     *
     * @param name
     */
    void removeResourceEnvRef(String name);


    /**
     * 移除指定资源连接
     *
     * @param name
     */
    void removeResourceLink(String name);


    /**
     * Remove any security role reference for the specified name
     *
     * @param role Security role (as used in the application) to remove
     */
//    public void removeRoleMapping(String role);


    /**
     * Remove any security role with the specified name.
     *
     * @param role Security role to remove
     */
//    public void removeSecurityRole(String role);


    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 移除pattern与指定指定的wrapper映射
     *
     * @param pattern
     */
    void removeServletMapping(String pattern);


    /**
     * Remove the tag library location forthe specified tag library URI.
     *
     * @param uri URI, relative to the web.xml file
     */
//    public void removeTaglib(String uri);


    /**
     * Remove the specified welcome file name from the list recognized
     * by this Context.
     *
     * @param name Name of the welcome file to be removed
     */
//    public void removeWelcomeFile(String name);


    /**
     * 移除指定的wrapper生命周期监听器类名
     *
     * @param listener
     */
    void removeWrapperLifecycle(String listener);


    /**
     * 移除指定的wrapper监听器类名
     *
     * @param listener
     */
    void removeWrapperListener(String listener);


    /**
     * 添加controller类
     *
     * @param controller
     * @exception Exception
     */
//    void addController(String controller) throws Exception;


    /**
     * 查询controller类
     *
     * @param name
     * @return
     */
//    Class findController(String name);
}
