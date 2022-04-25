package com.ranni.common;

/**
 * Title: HttpServer
 * Description:
 * 这个类存放全局作用域中服务器存入的键
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 18:27
 */
public final class Globals {
    // 工作目录属性
    public static final String WORK_DIR_ATTR = "javax.servlet.context.tempdir";

    // 目录容器资源文件
    public static final String RESOURCES_ATTR = "com.ranni.resources";

    // JSP类路径
    public static final String CLASS_PATH_ATTR = "com.ranni.jsp_classpath";

    // 异常属性
    public static final String EXCEPTION_ATTR = "javax.servlet.error.exception";
}
