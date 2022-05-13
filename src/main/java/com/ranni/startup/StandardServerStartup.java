package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.core.Server;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;


/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 11:39
 */
public class StandardServerStartup implements ServerStartup {
    protected boolean serverStartup; // 是否从服务器启动的标志位
    protected boolean started; // 服务器是否已启动
    protected Server server; // 服务器
    protected ConfigureParse configureParse; // 配置文件解析实例
    

    public StandardServerStartup() {
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));
    }
    
    
    /**
     * 启动服务器
     */
    @Override
    public void startup() {
        if (started) 
            throw new IllegalStateException("服务器已启动！");
        
        started = true;

        // 解析服务器配置信息
        Server server = getServer();
        if (server == null)
            throw new IllegalArgumentException("无可用服务器！");

        if (serverStartup) {
            // 通过服务器启动，扫描所有的webapp
            scanContext(server);
        }

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
     * 扫描所有的容器
     * 只有拥有启动类的webapp才会被创建Context容器并加入到服务器中进行管理
     * webapp的启动类必须继承WebApplicationStartupBase
     * 
     * @see WebApplicationStartup
     * 
     * @param server
     */
    protected void scanContext(Server server) {
        // TODO 扫描所有的容器
        // 先扫描所有的host，再从host中扫描context
    }


    /**
     * 解析服务器配置
     * 
     * @return
     */
    protected Server parseServerConfigure() {
        if (started)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  服务器已经启动，不能再解析配置文件！");
        
        if (this.configureParse == null)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  配置文件解析实例不能为null！");

        try {
            return configureParse.parse();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
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
     * 设置服务器
     *
     * @param server
     * @return
     */
    @Override
    public Server setServer(Server server) {
        if (started)
            throw new IllegalStateException("服务器已经启动，不能修改服务器！");

        return this.server = server;
    }


    /**
     * 取得服务器配置信息
     *
     * @return
     */
    @Override
    public Server getServer() {
        if (this.server == null) {
            this.server = parseServerConfigure();
        }
        return this.server;
    }


    /**
     * 回收资源
     */
    @Override
    public void recycle() {
        this.started = false;
        this.server = null;
        this.serverStartup = false;
        this.configureParse = null;
    }


    /**
     * 配置文件解析全限定类名
     * 
     * @param parse
     */
    @Override
    public void setConfigureParse(ConfigureParse parse) {
        this.configureParse = parse;
    }


    /**
     * 返回配置文件解析全限定类名
     * 
     * @return
     */
    @Override
    public ConfigureParse getConfigureParse() {
        return this.configureParse;
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
