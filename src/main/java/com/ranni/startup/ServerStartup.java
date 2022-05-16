package com.ranni.startup;

import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.core.Server;
import com.ranni.deploy.ApplicationConfigure;
import com.ranni.deploy.ConfigureMap;
import com.ranni.deploy.ServerConfigure;

import java.io.IOException;

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
     * @param server
     */
    void setServer(Server server);


    /**
     * 返回服务器
     * 
     * @return
     */
    Server getServer();
    

    /**
     * 设置服务器
     *
     * @param configureMap
     */
    void setConfigureMap(ConfigureMap<Server, ServerConfigure> configureMap);
    
    
    /**
     * 取得服务器
     *
     * @return
     */
    ConfigureMap<Server, ServerConfigure> getConfigureMap();


    /**
     * 回收资源
     */
    void recycle();


    /**
     * 返回启动标志位
     */
    boolean getStarted();


    /**
     * 设置配置文件解析器
     * 
     * @param parse
     */
    void setConfigureParse(ConfigureParse<Server, ServerConfigure> parse);


    /**
     * 返回配置文件解析器
     * 
     * @return
     */
    ConfigureParse<Server, ServerConfigure> getConfigureParse();


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


    /**
     * 初始化，解析服务器配置
     * 要在调用startup()之前调用此方法
     */
    void initialize() throws IOException;


    /**
     * 返回初始化标志位
     * 
     * @return
     */
    boolean getInitialized();
        

    /**
     * 初始化webapp
     * 
     * @param applicationConfigure
     * @return
     */
    Context initializeApplication(ApplicationConfigure applicationConfigure) throws Exception;
    
}
