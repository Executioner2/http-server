package com.ranni.startup;

import com.ranni.container.Engine;
import com.ranni.deploy.ServerMap;

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
    void startup() throws Exception;


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
     * @param serverMap
     */
    void setServerMap(ServerMap serverMap);


    /**
     * 取得服务器
     *
     * @return
     */
    ServerMap getServerMap();


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


    /**
     * 设置线程数量因子
     * 
     * @param divisor
     */
    void setDivisor(float divisor);


    /**
     * 返回engine
     * 
     * @return
     */
    Engine getEngine();
    
}
