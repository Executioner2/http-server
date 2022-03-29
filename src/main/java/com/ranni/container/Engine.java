package com.ranni.container;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 14:58
 */
public interface Engine extends Container {

    // 返回这个引擎容器的默认主机名
    String getDefaultHost();

    // 设置这个引擎容器的默认主机名
    void setDefaultHost(String defaultHost);

    // 检索此引擎的jvm路由id
    String getJvmRoute();

    // 设置jvm路由id，集群中的每个引擎都必须具有一个唯一的jvm路由id
    void setJvmRoute(String jvmRouteId);


    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
//    public Service getService();


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
//    public void setService(Service service);


    /**
     * Set the DefaultContext
     * for new web applications.
     *
     * @param defaultContext The new DefaultContext
     */
//    public void addDefaultContext(DefaultContext defaultContext);


    /**
     * Retrieve the DefaultContext for new web applications.
     */
//    public DefaultContext getDefaultContext();


    // --------------------------------------------------------- Public Methods


    /**
     * Import the DefaultContext config into a web application context.
     *
     * @param context web application context to import default context
     */
//    public void importDefaultContext(Context context);
}
