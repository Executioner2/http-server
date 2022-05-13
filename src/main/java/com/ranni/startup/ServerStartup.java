package com.ranni.startup;

import com.ranni.container.Context;

/**
 * Title: HttpServer
 * Description:
 * 服务器启动接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 11:38
 */
public interface ServerStartup {
    /**
     * 启动服务器
     */
    void startup();


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     */
    void setServerStartup(boolean serverStartup);


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     * @return
     */
    boolean getServerStartup(boolean serverStartup);


    /**
     * 设置服务器配置信息
     *
     * @param serverConfigure
     * @return
     */
    ServerConfigure setServerConfigure(ServerConfigure serverConfigure);


    /**
     * 取得服务器配置信息
     *
     * @return
     */
    ServerConfigure getServerConfigure();


    /**
     * 添加需要启动的容器
     * 
     * @param context
     */
    void addContext(Context context);


    /**
     * 回收资源
     */
    void recycle();


    /**
     * 返回启动标志位
     */
    boolean getStarted();
}
