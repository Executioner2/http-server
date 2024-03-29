package com.ranni.startup;

import com.ranni.container.Engine;
import com.ranni.core.Server;
import com.ranni.deploy.ConfigureMap;
import com.ranni.deploy.ServerConfigure;

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
     * 返回engine
     * 
     * @return
     */
    Engine getEngine();


    /**
     * 初始化，解析服务器配置
     * 要在调用startup()之前调用此方法
     */
    void initialize() throws Exception;


    /**
     * 返回初始化标志位
     * 
     * @return
     */
    boolean getInitialized();


    /**
     * 指定配置文件路径
     * 若路径为null，则使用默认的配置文件
     * 
     * @param path
     */
    void setServerConfigurePath(String path);
    

    /**
     * @return 返回服务器基本路径
     */
    String getServerBase();


    /**
     * 设置等待超时时长
     * 
     * @param awaitTime 超时时长
     */
    void setAwaitTime(long awaitTime);


    /**
     * @return 返回等待超时时长，负数为无限等待
     */
    long getAwaitTime();


    /**
     * 设置服务器启动方式
     * 
     * @param startingMode 服务器启动方式
     */
    void setStartingMode(StartingMode startingMode);


    /**
     * @return 返回服务器启动方式
     */
    StartingMode getStartingMode();
}
