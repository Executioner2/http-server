package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Container;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.Wrapper;
import com.ranni.container.context.StandardContext;
import com.ranni.container.engine.StandardEngine;
import com.ranni.container.host.StandardHost;
import com.ranni.loader.Loader;
import com.ranni.loader.WebappLoader;
import com.ranni.container.wrapper.StandardWrapper;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.core.StandardServer;
import com.ranni.core.StandardService;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.logger.Logger;

/**
 * Title: HttpServer
 * Description:
 * 标准的启动器，通过Server启动，Engine作为顶级容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 21:59
 */
public final class StandardApplication {

    /**
     * 通过代码配置
     */
    private static void config(Server server) {
        // 创建三个wrapper并分别设置名字和关联的Servlet类名
        Wrapper wrapper1 = new StandardWrapper();
        Wrapper wrapper2 = new StandardWrapper();
        Wrapper wrapper3 = new StandardWrapper();
        wrapper1.setName("Primitive");
        wrapper1.setServletClass("PrimitiveServlet");
        wrapper2.setName("Modern");
        wrapper2.setServletClass("ModernServlet");
        wrapper3.setName("Session");
        wrapper3.setServletClass("SessionServlet");

        // 创建一个Context容器
        StandardContext context = new StandardContext();

        // 给Context添加一个配置监听者
        context.addLifecycleListener(new ContextConfig());

        // 给Context添加子容器
        context.addChild(wrapper1);
        context.addChild(wrapper2);
        context.addChild(wrapper3);

        // 给Context添加Servlet映射
        context.addServletMapping("/Primitive", "Primitive");
        context.addServletMapping("/Modern", "Modern");
        context.addServletMapping("/Session", "Session");

        // 给Context设置路径，可重载，后台线程间隔时间
        context.setPath("/app1");
        context.setDocBase("app1");
        context.setReloadable(true);
        context.setBackgroundProcessorDelay(1);

        // 创建一个虚拟主机并添加子容器（Context）并设置此虚拟主机的名字
        Host host = new StandardHost();
        host.addChild(context);
        host.setName("Host");
        host.setAppBase("myApp");

        // 创建服务器引擎并添加一个默认的虚拟主机        
        Engine engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("Host");

        // 创建一个连接器并设置日志输出级别
        HttpConnector connector = new HttpConnector();
        connector.setDebug(4);

        // 创建一个服务并关联容器与连接器
        Service service = new StandardService();
        service.setName("StandardService");
        service.setContainer(engine);
        service.addConnector(connector);

        server.addService(service);
    }


    public void startup() {
        // 扫描
        
        // 设置服务器根目录
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));

        // 创建一个服务器并做配置
        Server server = new StandardServer();
//        config(server);

        // 创建连接器
        HttpConnector connector = new HttpConnector();
        connector.setDebug(Logger.DEBUG);

        // 创建context容器
        StandardContext context = new StandardContext();
        context.addLifecycleListener(new ContextConfig()); // 配置器

        // 给Context设置路径，可重载，后台线程间隔时间
        context.setPath("/app1");
        context.setDocBase("app1");
        context.setReloadable(true);
        context.setBackgroundProcessorDelay(1);

        Loader loader = new WebappLoader();
        context.setLoader(loader);

        // 创建并配置host
        Host host = new StandardHost();
        host.setName("host");
        host.setAppBase("webapps");
        host.addChild(context);

        // 创建并配置引擎
        Engine engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("host");

        // 创建服务
        Service service = new StandardService();
        service.addConnector(connector);
        service.setContainer(engine);
        service.setName("StandardService");

        server.addService(service);

        if (server instanceof Lifecycle) {
            try {
                server.initialize();
                ((Lifecycle) server).start(); // 启动服务器

                // 输出context的所有子容器
                Thread.sleep(1000);
                System.out.println("输出wrapper容器：");
                for (Container container : context.findChildren()) {
                    System.out.println("wrapper: " + container.getName() + "  " + container);
                }

                server.await(); // 等待关闭
            } catch (LifecycleException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).stop(); // 关闭服务器
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
        }
    }
}
