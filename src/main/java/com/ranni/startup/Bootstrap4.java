package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Wrapper;
import com.ranni.container.context.StandardContext;
import com.ranni.container.host.StandardHost;
import com.ranni.container.wrapper.StandardWrapper;

/**
 * Title: HttpServer
 * Description:
 * 此启动类在Bootstrap3的基础上实现了Host容器启动服务器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 14:13
 */
@Deprecated // FIXME - 已经不能通过连接器自动启动容器了
public class Bootstrap4 {
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

        StandardHost standardHost = new StandardHost();
        standardHost.addChild(context);
        standardHost.setName("Host");
        
//        context.setParent(standardHost);
//        context.setLoader(loader);

        try {
            connector.setContainer(standardHost);
            connector.setDebug(4);
            connector.initialize();
            connector.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}