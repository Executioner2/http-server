package com.ranni.startup;

import com.ranni.connector.Connector;
import com.ranni.connector.CoyoteAdapter;
import com.ranni.connector.Mapper;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.container.engine.StandardEngine;
import com.ranni.container.host.StandardHost;
import com.ranni.container.wrapper.StandardWrapper;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.core.StandardServer;
import com.ranni.core.StandardService;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.loader.WebappLoader;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/7/7 16:24
 */
public class BootstrapTest {
    public static void main(String[] args) throws UnknownHostException {

        Mapper mapper = new Mapper();

        Server server = new StandardServer();
        Service service = new StandardService();

        Connector connector = new Connector();
        connector.setAddress(InetAddress.getByName("127.0.0.1"));
        connector.setPort(8080);
        connector.setAdapter(new CoyoteAdapter(connector));
        
        service.setMapper(mapper);


        Host host = new StandardHost();
        host.setAppBase("");
        mapper.addHost("webapps", new String[0], host);
        StandardContext context = new StandardContext();
        context.setDocBase("test");
        context.setPath("/test");
//        context.setLogger();
        WebappLoader webappLoader = new WebappLoader();
        context.setLoader(webappLoader);
        
        StandardWrapper wrapper = new StandardWrapper();
        wrapper.setServletClass("test.TestServlet");
        wrapper.setName("/TestServlet");
        
        mapper.addContext("webapps", host, "/test", context);

        mapper.addWrapper("webapps", "/test", wrapper);

        Engine engine = new StandardEngine();
        service.setContainer(engine);
        service.addConnector(connector);
        server.addService(service);
        
        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).start();
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
        }
        
    }
}
