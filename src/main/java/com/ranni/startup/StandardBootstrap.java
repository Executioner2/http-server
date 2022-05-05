package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.Wrapper;
import com.ranni.container.context.StandardContext;
import com.ranni.container.engine.StandardEngine;
import com.ranni.container.host.StandardHost;
import com.ranni.container.wrapper.StandardWrapper;

/**
 * Title: HttpServer
 * Description:
 * 标准的启动器，通过Engine启动
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 21:59
 */
public class StandardBootstrap {
    public static void main(String[] args) {
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));

        HttpConnector connector = new HttpConnector();

        StandardContext context = new StandardContext();
//        WebappLoader loader = new WebappLoader();
        context.setPath("/myApp");
        context.setDocBase("myApp");

        // 创建两个wrapper
        Wrapper wrapper1 = new StandardWrapper();
        Wrapper wrapper2 = new StandardWrapper();
        Wrapper wrapper3 = new StandardWrapper();
        wrapper1.setName("Primitive");
        wrapper1.setServletClass("PrimitiveServlet");
        wrapper2.setName("Modern");
        wrapper2.setServletClass("ModernServlet");
        wrapper3.setName("Session");
        wrapper3.setServletClass("SessionServlet");

        context.addChild(wrapper1);
        context.addChild(wrapper2);
        context.addChild(wrapper3);

        context.addServletMapping("/Primitive", "Primitive");
        context.addServletMapping("/Modern", "Modern");
        context.addServletMapping("/Session", "Session");

        context.setPath("/app1");
        context.setReloadable(true);
        context.setBackgroundProcessorDelay(1);

        Host host = new StandardHost();
        host.addChild(context);
        host.setName("Host");

        Engine engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("Host");

        try {
            connector.setContainer(engine);
            connector.setDebug(4);
            connector.initialize();
            connector.start();

            System.in.read();
            connector.stop();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
