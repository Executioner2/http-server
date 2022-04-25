package com.ranni.connector;

import java.io.File;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 19:47
 */
public final class Constants {
    public static final int MAJOR_VERSION = 2;
    public static final int MINOR_VERSION = 3;

    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot"; // WEB根目录
    public static final String DEFAULT_SERVER_IPADDRESS = "127.0.0.1"; // 服务器默认IP
    public static final int DEFAULT_BACKLOG = 8; // 服务器默认最大等待队列
    public static final String JSP_SERVLET_NAME = "jsp"; // jsp文件
}
