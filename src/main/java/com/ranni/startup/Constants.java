package com.ranni.startup;

import java.io.File;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 22:35
 */
public class Constants {
    public static final String DEFAULT_SERVER_YAML = com.ranni.common.Constants.CONF + File.separator + "server.yaml"; // 默认server.yaml名
    public static final String DEFAULT_SERVER_YML = com.ranni.common.Constants.CONF + File.separator + "server.yml"; // 默认server.yml名
    public static final String APPLICATION_YAML = com.ranni.common.Constants.WEBAPP_BASE + File.separator + "application.yaml"; // webapp的配置
    public static final String APPLICATION_YML = com.ranni.common.Constants.WEBAPP_BASE + File.separator + "application.yml"; // webapp的配置
}
