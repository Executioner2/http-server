package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * Context容器配置类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 21:05
 */
@Deprecated
public final class ContextConfigure {
    private String bootstrap; // 启动类
    private String configureClass = "com.ranni.startup.ContextConfig"; // 配置器类名
    private String path; // 路径
    private String host = "localhost"; // 所属主机
    private boolean reloadable; // 是否可重载
    private int port = 8080; // 端口号
    private int backgroundProcessorDelay; // 后台线程的休眠因子，值小于等于0表示没有单独的后台任务线程

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
    }

    public String getConfigureClass() {
        return configureClass;
    }

    public void setConfigureClass(String configureClass) {
        this.configureClass = configureClass;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    public void setBackgroundProcessorDelay(int backgroundProcessorDelay) {
        this.backgroundProcessorDelay = backgroundProcessorDelay;
    }
}
