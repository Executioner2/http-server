package com.ranni.startup;

import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.EngineConfigure;
import com.ranni.deploy.HostConfigure;
import com.ranni.deploy.ServerConfigure;
import com.ranni.deploy.ServiceConfigure;

/**
 * Title: HttpServer
 * Description:
 * 服务器配置文件解析
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 20:53
 */
public class ServerConfigureParse extends ConfigureParseBase {

    // ==================================== 构造方法 ====================================

    public ServerConfigureParse() {
        this(ServerConfigure.class);
    }

    public ServerConfigureParse(Class clazz) {
        super(clazz);
    }

    
    // ==================================== 核心方法 ====================================
    
    /**
     * 装配
     *
     * @param load 配置类
     * @return 返回Server实例
     */
    @Override
    public Object fit(Object load) throws Exception {
        ServerConfigure serverConfigure = (ServerConfigure) load;

        if (serverConfigure.getServices().isEmpty())
            throw new IllegalStateException("至少要有一个Service实例！");

        // 创建一个server
        Server server = (Server) getInstance(serverConfigure.getServerClass());

        // 配置engine
        EngineConfigure engineConfigure = serverConfigure.getEngine();
        Engine engine = (Engine) getInstance(engineConfigure.getClazz());
        engine.setDefaultHost(engineConfigure.getDefaultHost());
        engine.setName(engineConfigure.getName());

        // 配置host
        for (HostConfigure hostConfigure : engineConfigure.getHosts()) {

            if (!hostConfigure.isValid()) {
                continue;
            }
            
            Host host;

            if (hostConfigure.getClazz() == null) {
                host = (Host) getInstance(engineConfigure.getHostClass());
            } else {
                host = (Host) getInstance(hostConfigure.getClazz());
            }

            host.setName(hostConfigure.getName());
            host.setAppBase(hostConfigure.getAppBase());
            host.setAutoDeploy(hostConfigure.isAutoDeploy());

            engine.addChild(host);
        }

        // 配置服务
        for (ServiceConfigure serviceConfigure : serverConfigure.getServices()) {
            String serviceClass = serviceConfigure.getServiceClass() == null ? serverConfigure.getServiceClass() : serviceConfigure.getServiceClass();
            Service service = (Service) getInstance(serviceClass);
            service.setContainer(engine);
            service.setName(serviceConfigure.getName());
            server.addService(service);
        }
        
        return server;
    }


    /**
     * 获取实例
     * 
     * @param clazz
     * @return
     */
    private Object getInstance(String clazz) throws Exception {
        Class<?> aClass = Class.forName(clazz);
        Object o = aClass.getDeclaredConstructor().newInstance();
        return o;
    }
    
}
