package com.ranni.startup;

import com.ranni.core.Server;

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
     * 设置服务器
     *
     * @param server
     * @return
     */
    Server setServer(Server server);


    /**
     * 取得服务器
     *
     * @return
     */
    Server getServer();


    /**
     * 回收资源
     */
    void recycle();


    /**
     * 返回启动标志位
     */
    boolean getStarted();


    /**
     * 配置文件解析实例
     * 
     * @param parse
     */
    void setConfigureParse(ConfigureParse parse);


    /**
     * 返回配置文件解析实例
     * 
     * @return
     */
    ConfigureParse getConfigureParse();
}
