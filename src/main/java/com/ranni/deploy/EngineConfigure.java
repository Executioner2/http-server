package com.ranni.deploy;

import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * Engine配置类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 21:05
 */
public class EngineConfigure {
    private String defaultHost; // 默认的host容器名
    private String clazz = "com.ranni.container.engine.StandardEngine"; // engine的实现类
    private String hostClass = "com.ranni.container.host.StandardHost";
    private List<HostConfigure> hosts; // host容器集合

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getHostClass() {
        return hostClass;
    }

    public void setHostClass(String hostClass) {
        this.hostClass = hostClass;
    }

    public List<HostConfigure> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostConfigure> hosts) {
        this.hosts = hosts;
    }
}
