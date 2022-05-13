package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.Connector;
import com.ranni.container.DefaultContext;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;

/**
 * Title: HttpServer
 * Description:
 * 服务器配置文件解析
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 20:53
 */
public class ServerConfigureParse implements ConfigureParse {
    private String serverClass = "com.ranni.core.StandardServer"; // server全限定类名
    private String serviceClass = "com.ranni.core.StandardService"; // service全限定类名
    
    private Yaml yaml = new Yaml(new Constructor(ServerConfigure.class)); // 解析工具
    
    
    /**
     * 解析配置文件
     * 
     * @return
     */
    @Override
    public Server parse() throws Exception {
        String filePath = System.getProperty(SystemProperty.SERVER_BASE) + File.separator + Constants.DEFAULT_WEB_YAML;
        File file = new File(filePath);
        
        ServerConfigure serverConfigure = yaml.load(new FileInputStream(file));
        
        if (serverConfigure.getEngine().getHosts().isEmpty()) 
            throw new IllegalStateException("至少要有一个HOST容器！");

        // 创建一个server
        Server server = (Server) getInstance(serverClass);

        // 配置engine
        EngineConfigure engineConfigure = serverConfigure.getEngine();
        Engine engine = (Engine) getInstance(engineConfigure.getClazz());
        engine.setDefaultHost(engineConfigure.getDefaultHost());
        
        // 配置host
        for (HostConfigure hostConfigure : engineConfigure.getHosts()) {
            
            Host host;
            
            if (hostConfigure.getClazz() == null) {
                host = (Host) getInstance(engineConfigure.getHostClass());
            } else {
                host = (Host) getInstance(hostConfigure.getClazz());
            }
            
            host.setName(hostConfigure.getName());
            host.setAppBase(hostConfigure.getAppBase());
            
            if (hostConfigure.getDefaultContextClass() != null)
                host.addDefaultContext((DefaultContext) getInstance(hostConfigure.getDefaultContextClass()));
            
            engine.addChild(host);
        }
                
        // 配置服务
        for (ServiceConfigure serviceConfigure : serverConfigure.getServices()) {
            Service service = (Service) getInstance(serviceClass);            
            service.setContainer(engine);
            service.setName(serviceConfigure.getName());
            
            // 配置连接器
            for (ConnectorConfigure connectorConfigure : serviceConfigure.getConnectors()) {
                
                Connector connector;
                
                if (connectorConfigure.getClazz() == null) {
                    connector = (Connector) getInstance(serviceConfigure.getConnectorClass());
                } else {
                    connector = (Connector) getInstance(connectorConfigure.getClazz());
                }
                
                connector.setDebug(connectorConfigure.getDebug());
                service.addConnector(connector);
            }

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


    /**
     * 设置server实现类的全限定类名
     * 
     * @param clazz
     */
    public void setServerClass(String clazz) {
        this.serverClass = clazz;
    }


    /**
     * 返回server实现类的全限定类名
     * 
     * @return
     */
    public String getServerClass() {
        return serverClass;
    }
}
