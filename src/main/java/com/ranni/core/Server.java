package com.ranni.core;

import com.ranni.lifecycle.LifecycleException;
import com.ranni.deploy.NamingResources;

/**
 * Title: HttpServer
 * Description:
 * 通过Server来控制服务器的启动和关闭
 * 这样就可以创建多个连接器处理不同协议（例如HTTP和HTTPS） 或不同端口的请求
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 9:11
 */
public interface Server {
    
    
    /**
     * 返回实现类信息
     * 
     * @return
     */
    String getInfo();


    /**
     * 返回全局JNDI命名资源
     * 
     * @return
     */
    NamingResources getGlobalNamingResources();


    /**
     * 设置全局JNDI命名资源
     * 
     * @param globalNamingResources
     */
    void setGlobalNamingResources(NamingResources globalNamingResources);


    /**
     * 返回服务器的端口号，不是webapp的
     * 
     * @return
     */
    int getPort();


    /**
     * 设置服务器的端口号，不是webapp的
     * 
     * @param port
     */
    void setPort(int port);


    /**
     * 取得关闭指令
     * 
     * @return
     */
    String getShutdown();


    /**
     * 设置关闭指令
     * 
     * @param shutdown
     */
    void setShutdown(String shutdown);
    

    /**
     * 添加服务
     * 
     * @param service
     */
    void addService(Service service);


    /**
     * 等待收到关机指令
     */
    void await();


    /**
     * 根据传入参数取得对应的服务
     * 
     * @param name
     * @return
     */
    Service findService(String name);


    /**
     * 返回所有的服务
     * 
     * @return
     */
    Service[] findServices();


    /**
     * 删除指定的服务
     * 
     * @param service
     */
    void removeService(Service service);


    /**
     * 初始化
     * 
     * @throws LifecycleException
     */
    void initialize() throws LifecycleException;
}
