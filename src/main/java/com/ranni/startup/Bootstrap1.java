package com.ranni.startup;

import com.ranni.connector.HttpConnector;
import com.ranni.container.wrapper.StandardWrapper;

/**
 * Title: HttpServer
 * Description: Http服务器启动类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 19:37
 */
public class Bootstrap1 {
    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        try {
            connector.initialize();
            StandardWrapper simpleWrapper = new StandardWrapper();
            connector.setContainer(simpleWrapper);
            connector.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
