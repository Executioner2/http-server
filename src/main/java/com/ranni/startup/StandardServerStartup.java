package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ServerConfigure;
import com.ranni.deploy.ServerMap;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;

import javax.annotation.processing.FilerException;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipFile;


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
    protected ServerMap serverMap; // 服务器实例与服务器实例映射
    protected ConfigureParse configureParse; // 配置文件解析实例
    

    public StandardServerStartup() {
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));
    }
    
    
    /**
     * 启动服务器
     */
    @Override
    public void startup() throws Exception {
        if (started) 
            throw new IllegalStateException("服务器已启动！");
        
        started = true;

        // 解析服务器配置信息
        ServerMap serverMap = getServerMap();
        
        if (serverMap == null)
            throw new IllegalArgumentException("无可用服务器！");

        Server server = serverMap.getServer();
        
        if (serverStartup) {
            // 通过服务器启动，扫描所有的webapp
            scanContext();
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
     */
    protected void scanContext() throws IOException {
        // TODO - 扫描所有的容器
        // 先扫描所有的host，再从host中扫描context
        Server server = serverMap.getServer();
        ServerConfigure serverConfigure = serverMap.getServerConfigure();
        
        Service[] services = server.findServices();
        Engine engine = (Engine) services[0].getContainer();

        Deque<ZipFile> wars = new LinkedList<>();
        Deque<File> webapps = new LinkedList<>();
        
        for (Host host : (Host[]) engine.findChildren()) {
            // XXX - 考虑要不要把webapp的遍历交给加载器
            String path = System.getProperty(SystemProperty.SERVER_BASE) + File.separator + host.getAppBase();
            File repository = new File(path);
            if (!repository.isDirectory())
                throw new FilerException("此路径应该是个目录！");
            
            // 把目录和war文件分离出来
            for (File file : repository.listFiles()) {
                if (file.isDirectory()) {
                    webapps.push(file);
                } else if (serverConfigure.isUnpackWARS() 
                        && file.getName().endsWith(".war")) {
                    
                    wars.push(new ZipFile(file));
                }
            }
            
            // 解压war包
            while (!wars.isEmpty()) {
                ZipFile war = wars.pop();
                // TODO - 明日继续
            }
        }

    }


    /**
     * 解析服务器配置
     * 
     * @return
     */
    protected ServerMap parseServerConfigure() {
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
     * @param serverMap
     */
    @Override
    public void setServerMap(ServerMap serverMap) {
        if (started)
            throw new IllegalStateException("服务器已经启动，不能修改服务器！");

        this.serverMap = serverMap;
    }


    /**
     * 取得服务器配置信息
     *
     * @return
     */
    @Override
    public ServerMap getServerMap() {
        if (this.serverMap == null) {
            this.serverMap = parseServerConfigure();
        }
        return this.serverMap;
    }


    /**
     * 回收资源
     */
    @Override
    public void recycle() {
        this.started = false;
        this.serverMap = null;
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
