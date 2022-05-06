package com.ranni.container;

import com.ranni.core.Service;

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
     * 返回此容器关联的服务
     * 
     * @return
     */
    Service getService();


    /**
     * 设置此容器关联的服务
     * 
     * @param service
     */
    void setService(Service service);


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
