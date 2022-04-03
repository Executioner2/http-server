package com.ranni.logger;

/**
 * Title: HttpServer
 * Description:
 * 将日志消息打印到控制台上的类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 20:44
 */
public class SystemOutLogger extends LoggerBase {

    @Override
    public void log(String msg) {
        System.out.println(msg);
    }
}
