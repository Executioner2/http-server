package com.ranni.startup;

import com.ranni.container.DefaultContext;
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
    private String serverClass = "com.ranni.core.StandardServer"; // server全限定类名
    private String serviceClass = "com.ranni.core.StandardService"; // service全限定类名

    public ServerConfigureParse() {
        this(ServerConfigure.class);
    }

    public ServerConfigureParse(Class clazz) {
        super(clazz);
    }

    
//    @Override
//    public ConfigureMap parse(File file) throws Exception {
//        return super.parse(file);
//    }

    /**
     * 装配
     *
     * @return
     * @param load
     */
    @Override
    protected Object fit(Object load) throws Exception {
        ServerConfigure serverConfigure = (ServerConfigure) load;
        
        // XXX - 加入了一个不存在的Host容器配置也会通过，后面判断如果不存在就会跳过创建此配置的Host容器实例。（此情况出现在通过webapp启动服务器）
        // XXX - 这条判断更多的是为了通过服务器启动时有一个默认的Host容器
        if (serverConfigure.getEngine().getHosts().isEmpty())
            throw new IllegalStateException("至少要有一个Host容器！");

        if (serverConfigure.getServices().isEmpty())
            throw new IllegalStateException("至少要有一个Service实例！");

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
            host.setAutoDeploy(hostConfigure.isAutoDeploy());

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
//            for (ConnectorConfigure connectorConfigure : serviceConfigure.getConnectors()) {
//
//                Connector connector;
//
//                if (connectorConfigure.getClazz() == null) {
//                    connector = (Connector) getInstance(serviceConfigure.getConnectorClass());
//                } else {
//                    connector = (Connector) getInstance(connectorConfigure.getClazz());
//                }
//
//                connector.setDebug(connectorConfigure.getDebug());
//                service.addConnector(connector);
//            }

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
