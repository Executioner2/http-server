package com.ranni.container;

import com.ranni.core.FilterDef;
import com.ranni.deploy.*;
import com.ranni.util.CharsetMapper;
import com.ranni.util.http.CookieProcessor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 * ServletContext的处理类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:50
 */
public interface Context extends Container, ContextBind {
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
     * 返回此web应用程序的请求路径
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
     * 移除指定的参数
     *
     * @param name
     */
    void removeParameter(String name);


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
     * @return 返回是否对请求大小进行约束的标志位
     */
    boolean getSwallowAbortedUploads();


    /**
     * @return 返回session的cookie名
     */
    String getSessionCookieName();


    /**
     * @return 返回session的cookie路径
     */
    String getSessionCookiePath();


    /**
     * @return 返回解码后的路径
     */
    String getEncodedPath();
    
    
    boolean getSessionCookiePathUsesTrailingSlash();

    
    String getSessionCookieDomain();


    void setCookieProcessor(CookieProcessor cookieProcessor);
    
    CookieProcessor getCookieProcessor();


    /**
     * @return 如果返回true，则表示已经解析了Multipart
     */
    boolean getAllowCasualMultipartParsing();


    /**
     * @return 如果为true，则表示允许上传目标不存在时
     *         新建，否则反之
     */
    boolean getCreateUploadTargets();


    /**
     * 触发请求销毁事件
     * 
     * @param request
     */
    boolean fireRequestDestroyEvent(HttpServletRequest request);


    /**
     * @return 调度程序是否允许被编码 
     */
    boolean getDispatchersUseEncodedPaths();


    /**
     * 设置容器的默认请求包编码格式
     *
     * @param encoding 默认的请求包编码格式
     */
    void setRequestCharacterEncoding(String encoding);

    
    /**
     * @return 返回请求包的默认编码格式
     */
    String getRequestCharacterEncoding();


    /**
     * @return 返回响应包的默认编码格式
     */
    String getResponseCharacterEncoding();
    
    
    /**
     * 设置容器的默认响应包编码格式
     * 
     * @param encoding 默认的响应包编码格式
     */
    void setResponseCharacterEncoding(String encoding);


    /**
     * @return 返回容器版本 
     */
    String getWebappVersion();


    /**
     * 设置容器版本
     * 
     * @param webappVersion 容器版本
     */
    void setWebappVersion(String webappVersion);


    /**
     * @return 如果返回<b>true</b>，则表示容器处于暂停状态
     */
    boolean getPaused();


    /**
     * 设置是否支持相对重定向标志位
     * 
     * @param useRelativeRedirects 是否支持相对重定向
     */
    void setUseRelativeRedirects(boolean useRelativeRedirects);
    
    
    /**
     * @return 如果返回<b>true</b>，则表示容器支持相对向
     */
    boolean getUseRelativeRedirects();


    /**
     * 客户端进行重定向时是否包含响应体内容
     * 
     * @param enable 如果为<b>true</b>，则表示允许重定向携带响应体内容
     */
    void setSendRedirectBody(boolean enable);
    

    /**
     * 
     * @return 如果返回<b>true</b>，则表示响应体内容也会作为重定向的一部分
     */
    boolean getSendRedirectBody();


    /**
     * 根据传入的语言环境返回容器处理请求/响应的编码
     * 
     * @param loc 传入的语言环境
     * @return 返回处理这种语言环境的编码            
     */
    String getCharset(Locale loc);

    
    boolean getUseHttpOnly();
}
