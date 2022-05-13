package com.ranni.deploy;

import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * 服务器配置
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 18:26
 */
public final class ServerConfigure {
    private EngineConfigure engine; // 服务器引擎
    private List<ServiceConfigure> services; // 服务集合

    public EngineConfigure getEngine() {
        return engine;
    }

    public List<ServiceConfigure> getServices() {
        return services;
    }

    public void setEngine(EngineConfigure engine) {
        this.engine = engine;
    }

    public void setServices(List<ServiceConfigure> services) {
        this.services = services;
    }
}


