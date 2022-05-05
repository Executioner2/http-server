package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Context;
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
        // 设置服务器根目录
        System.setProperty(SystemProperty.SERVER_BASE, System.getProperty("user.dir"));

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
        Context context = new StandardContext();
        
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
        context.setReloadable(true);
        context.setBackgroundProcessorDelay(1);

        // 创建一个虚拟主机并添加子容器（Context）并设置此虚拟主机的名字
        Host host = new StandardHost();
        host.addChild(context);
        host.setName("Host");

        // 创建服务器引擎并添加一个默认的虚拟主机        
        Engine engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("Host");

        try {
            // 创建一个连接器
            HttpConnector connector = new HttpConnector();
            
            // 设置关联的容器和日志输出级别
            connector.setContainer(engine);
            connector.setDebug(4);
            
            // 初始化连接器并启动
            connector.initialize();
            connector.start();
            
            // 按下任意键停止服务器
            System.in.read();            
            connector.stop();
            
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
