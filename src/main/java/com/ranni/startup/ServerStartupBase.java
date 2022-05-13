package com.ranni.startup;

import com.ranni.connector.HttpConnector;
import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.engine.StandardEngine;
import com.ranni.container.host.StandardHost;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.core.StandardServer;
import com.ranni.core.StandardService;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.logger.Logger;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 11:39
 */
public abstract class ServerStartupBase implements ServerStartup {
    protected boolean serverStartup; // 是否从服务器启动的标志位
    protected boolean started; // 服务器是否已启动
    protected ServerConfigure serverConfigure; // 服务器配置信息
    protected Deque<Context> contextDeque = new LinkedList<>(); // 需要启动的容器队列


    /**
     * 启动服务器
     * XXX - 考虑通过服务器配置来指定参与启动的类
     */
    @Override
    public void startup() {
        if (started) 
            throw new IllegalStateException("服务器已启动！");
        
        started = true;

        // 解析服务器配置信息
        serverConfigure = parseServerConfigure();

        // XXX - 下面的代码仅仅是测试代码
        
        // 创建一个服务器并做配置
        Server server = new StandardServer();
        
        // 创建并配置host
        Host host = new StandardHost();
        host.setName("host");
        host.setAppBase("webapps");

        // 创建并配置引擎
        Engine engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("host");

        // 创建一个连接器并设置日志输出级别
        HttpConnector connector = new HttpConnector();
        connector.setDebug(Logger.DEBUG);

        // 创建一个服务
        Service service = new StandardService();
        service.setName("StandardService");
        service.setContainer(engine);
        service.addConnector(connector);

        if (server instanceof Lifecycle) {
            try {
                server.initialize();
                ((Lifecycle) server).start(); // 启动服务器
                server.await(); // 等待关闭
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
        }
        
        try {
            if (server instanceof Lifecycle)
                ((Lifecycle) server).stop(); // 关闭服务器 
            
        } catch (LifecycleException e) {
            e.printStackTrace();
        } finally {
            // 回收资源
            this.recycle();                
        }
        
    }


    /**
     * 解析服务器配置
     */
    private ServerConfigure parseServerConfigure() {
        if (getServerConfigure() == null) {
            // TODO 解析服务器配置
        }

        return getServerConfigure();
    }


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     */
    @Override
    public void setServerStartup(boolean serverStartup) {
        this.serverStartup = serverStartup;
    }


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     * @return
     */
    @Override
    public boolean getServerStartup(boolean serverStartup) {
        return this.serverStartup;
    }


    /**
     * 设置服务器配置信息
     *
     * @param serverConfigure
     * @return
     */
    @Override
    public ServerConfigure setServerConfigure(ServerConfigure serverConfigure) {
        if (started)
            throw new IllegalStateException("服务器已经启动，不能修改服务器配置信息！");

        return this.serverConfigure = serverConfigure;
    }


    /**
     * 取得服务器配置信息
     *
     * @return
     */
    @Override
    public ServerConfigure getServerConfigure() {
        return this.serverConfigure;
    }


    /**
     * 添加需要启动的webapp
     * 
     * @param context
     */
    @Override
    public void addContext(Context context) {
        contextDeque.add(context);
    }


    /**
     * 回收资源
     */
    @Override
    public void recycle() {
        this.started = false;
        this.contextDeque.clear();
        this.serverConfigure = null;
        this.serverStartup = false;
    }


    /**
     * 返回启动标志位
     * 
     * @return
     */
    @Override
    public boolean getStarted() {
        return this.started;
    }
}
