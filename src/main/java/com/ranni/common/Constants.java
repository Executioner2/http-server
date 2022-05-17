package com.ranni.common;

import java.io.File;

/**
 * Title: HttpServer
 * Description:
 * 通用的常量
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 10:28
 */
public class Constants {
    public static final String LIB = File.separator + "lib"; // 依赖库路径
    public static final String BIN = File.separator + "bin"; // 服务器核心文件路径
    public static final String CONF = File.separator + "conf"; // 配置文件路径
    public static final String WEBAPP_CLASSES = File.separator + "WEB-INF" + File.separator + "classes"; // webapp的执行目录
    public static final String WEBAPP_LIB = File.separator + "WEB-INF" + File.separator + "lib"; // webapp的执行目录
}
