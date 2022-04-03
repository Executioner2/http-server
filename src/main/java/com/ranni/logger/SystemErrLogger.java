package com.ranni.logger;

/**
 * Title: HttpServer
 * Description:
 * 将日志消息从标准错误输出流输出到控制台中
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 20:44
 */
public class SystemErrLogger extends LoggerBase {
    @Override
    public void log(String msg) {
        System.err.println(msg);
    }
}
