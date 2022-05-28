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

    public static final String SENDFILE_FILENAME_ATTR = "org.apache.tomcat.sendfile.filename";

    public static final boolean IS_SECURITY_ENABLED = (System.getSecurityManager() != null);

    public static final String PARAMETER_PARSE_FAILED_REASON_ATTR = "com.ranni.parameter_parse_failed_reason";
    
    public static final String PARAMETER_PARSE_FAILED_ATTR = "com.ranni.parameter_parse_failed";

    public static final String GSS_CREDENTIAL_ATTR = "com.ranni.realm.GSS_CREDENTIAL";

    public static final String ASYNC_SUPPORTED_ATTR = "com.ranni.ASYNC_SUPPORTED";

    public static final String DISPATCHER_TYPE_ATTR = "com.ranni.core.DISPATCHER_TYPE";

    public static final String DISPATCHER_REQUEST_PATH_ATTR = "com.ranni.core.DISPATCHER_REQUEST_PATH";
    
    public static final String STREAM_ID = "com.ranni.coyote.streamID";

    public static final String CONNECTION_ID = "com.ranni.coyote.connectionID";
    
    
    public static final String SENDFILE_SUPPORTED_ATTR = "com.ranni.sendfile.support";
    
    // 工作目录属性
    public static final String WORK_DIR_ATTR = "javax.servlet.context.tempdir";

    // 目录容器资源文件
    public static final String RESOURCES_ATTR = "com.ranni.resources";

    // JSP类路径
    public static final String CLASS_PATH_ATTR = "com.ranni.jsp_classpath";

    // 异常属性
    public static final String EXCEPTION_ATTR = "javax.servlet.error.exception";
    
    // JSP文件属性
    public static final String JSP_FILE_ATTR = "com.ranni.jsp_file";

    // 写入到Cookie中的sessionId属性
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    
    // webapp的启动类
    public static final String APPLICATION_BOOTSTRAP_CLASS = "application_bootstrap_class";
    
    // webapp的controller类们
    public static final String APPLICATION_CONTROLLER_CLASSES = "application_controller_classes";
}
