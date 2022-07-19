package com.ranni.core;

import com.ranni.container.Engine;
import com.ranni.connector.Mapper;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.connector.Connector;

import java.util.concurrent.Executor;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 9:20
 */
public interface Service {
    /**
     * 返回此服务关联的容器
     * 
     * @return
     */
    Engine getContainer();


    /**
     * 设置此服务关联容器
     * 
     * @param engine
     */
    void setContainer(Engine engine);


    /**
     * 返回实现类信息
     * 
     * @return
     */
    String getInfo();


    /**
     * 返回服务名
     * 
     * @return
     */
    String getName();


    /**
     * 设置服务名，唯一
     * 
     * @param name
     */
    void setName(String name);


    /**
     * 返回此服务所属的服务器
     * 
     * @return
     */
    Server getServer();


    /**
     * 设置所属的服务器
     * 
     * @param server
     */
    void setServer(Server server);


    /**
     * 添加服务的连接器
     * 
     * @param connector
     */
    void addConnector(Connector connector);


    /**
     * 返回所有连接器
     * 
     * @return
     */
    Connector[] findConnectors();


    /**
     * 移除指定的连接器
     * 
     * @param connector
     */
    void removeConnector(Connector connector);


    /**
     * 初始化
     * 
     * @throws LifecycleException
     */
    void initialize() throws LifecycleException;


    /**
     * @return 返回映射器
     */
    Mapper getMapper();


    /**
     * 设置映射实例
     * 
     * @param mapper 映射实例
     */
    void setMapper(Mapper mapper);


    /**
     * 供service管理的组件执行多线程任务。
     * 一个service下如果有多线程任务应该
     * 交给service来执行，这样可以包装线
     * 程数量的可控性。
     * 
     * @param task
     */
    void execute(Runnable task);


    /**
     * @return 返回多线程执行器
     */
    Executor getExecutor();


    /**
     * 设置线程池
     * 
     * @param executor 使用自定义的线程池
     */
    void setExecutor(Executor executor);


    /**
     * @return 返回线程池初始化线程数
     */
    int getMinSpareThreads();


    /**
     * 设置线程池初始化线程数
     * 
     * @param minSpareThreads 线程池初始化线程数
     */
    void setMinSpareThreads(int minSpareThreads);


    /**
     * @return 返回线程池最大线程数
     */
    int getMaxThreads();


    /**
     * 设置线程池最大线程数
     * 
     * @param maxThreads 线程池最大线程数
     */
    void setMaxThreads(int maxThreads);
}
