package com.ranni.startup;

import com.ranni.connector.HttpConnector;
import com.ranni.container.StandardContext;
import com.ranni.container.DefaultContextMapper;
import com.ranni.container.StandardWrapper;
import com.ranni.container.loader.SimpleLoader;
import com.ranni.test.valves.ClientIPLoggerValve;
import com.ranni.test.valves.HeaderLoggerValve;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-29 21:17
 */
public class Bootstrap2 {
    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();

        // 创建标准的wrapper并设置需要包装的servlet类名
        StandardWrapper wrapper = new StandardWrapper();
        wrapper.setName("TestServlet");
        wrapper.setServletClass("TestServlet");

        // 设置标准的context容器并添加子容器
        StandardContext context = new StandardContext();
        context.addChild(wrapper);

        // 实例化两个阀
        ClientIPLoggerValve valve1 = new ClientIPLoggerValve();
        HeaderLoggerValve valve2 = new HeaderLoggerValve();

        // 将阀添加到context容器中
        context.addValve(valve1);
        context.addValve(valve2);

        // 给容器类添加载器
        SimpleLoader loader = new SimpleLoader();
        context.setLoader(loader);

        // 创建context的默认映射器并设置协议
        DefaultContextMapper contextMapper = new DefaultContextMapper();
        contextMapper.setProtocol("http");


        // 给context容器添加刚刚创建的context映射器并添加URI与mapper名的映射
        context.addMapper(contextMapper);
        context.addServletMapping("/TestServlet", "TestServlet");

        // 设置连接器的容器
        connector.setContainer(context);

        try {
            connector.initialize();
            connector.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
