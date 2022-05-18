package com.ranni.deploy;

import com.ranni.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * webapp 配置
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 20:45
 */
public class ApplicationConfigure {
    private String bootstrap; // 启动类
    private String path; // 路径    
    private String host = "localhost"; // 所属主机
    private String ip; // ip地址
    private boolean reloadable; // 是否可重载
    private int port = 8080; // 端口号
    private int debug = Logger.WARNING; // debug级别
    private int backgroundProcessorDelay; // 后台线程的休眠因子，值小于等于0表示没有单独的后台任务线程
    private List<String> services = new ArrayList<>(){{ add("StandardService"); }}; // 加入的服务

    public int getDebug() {
        return debug;
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
    
    public String getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    public void setBackgroundProcessorDelay(int backgroundProcessorDelay) {
        this.backgroundProcessorDelay = backgroundProcessorDelay;
    }
}
