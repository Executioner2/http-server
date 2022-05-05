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

    /**
     * 返回这个引擎容器的默认主机名
     * 
     * @return
     */
    String getDefaultHost();

    
    /**
     * 设置这个引擎容器的默认主机名
     * 
     * @param defaultHost
     */
    void setDefaultHost(String defaultHost);

    
    /**
     * 检索此引擎的jvm路由id
     * 
     * @return
     */
    String getJvmRoute();

    
    /**
     * 设置jvm路由id，集群中的每个引擎都必须具有一个唯一的jvm路由id
     * 
     * @param jvmRouteId
     */
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
     * 设置默认Context容器
     * 
     * @param defaultContext
     */
    void addDefaultContext(DefaultContext defaultContext);


    /**
     * 返回默认的Context容器
     * 
     * @return
     */
    DefaultContext getDefaultContext();


    /**
     * 导入Context容器
     * 
     * @param context
     */
    void importDefaultContext(Context context);
}
