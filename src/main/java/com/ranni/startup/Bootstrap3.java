package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Wrapper;
import com.ranni.container.context.StandardContext;
import com.ranni.container.loader.WebappLoader;
import com.ranni.container.wrapper.StandardWrapper;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-13 19:04
 */
public class Bootstrap3 {
    public static void main(String[] args) {
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));

        HttpConnector connector = new HttpConnector();

        StandardContext context = new StandardContext();
        WebappLoader loader = new WebappLoader();
        context.setPath("/myApp");
        context.setDocBase("myApp");

        // 创建两个wrapper
        Wrapper wrapper1 = new StandardWrapper();
        Wrapper wrapper2 = new StandardWrapper();
        wrapper1.setName("Primitive");
        wrapper1.setServletClass("PrimitiveServlet");
        wrapper2.setName("Modern");
        wrapper2.setServletClass("ModernServlet");

        context.addChild(wrapper1);
        context.addChild(wrapper2);

        context.addServletMapping("/Primitive", "Primitive");
        context.addServletMapping("/Modern", "Modern");
        context.setLoader(loader);

        try {
            connector.setContainer(context);
            connector.setDebug(4);
            connector.initialize();
            connector.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
