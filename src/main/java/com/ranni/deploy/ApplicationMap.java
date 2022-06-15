package com.ranni.deploy;

import com.ranni.connector.Connector;
import com.ranni.container.Context;

/**
 * Title: HttpServer
 * Description:
 * Application配置填充后的返回的实例
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/17 11:09
 */
public final class ApplicationMap {
    private Context context; // context容器
    private ApplicationConfigure applicationConfigure; // 配置文件解析后的实例
    private Connector connector; // 连接器
    private String appBase; // host根目录
    private String host; // host名

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ApplicationConfigure getApplicationConfigure() {
        return applicationConfigure;
    }

    public void setApplicationConfigure(ApplicationConfigure applicationConfigure) {
        this.applicationConfigure = applicationConfigure;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAppBase() {
        return appBase;
    }

    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }
}
