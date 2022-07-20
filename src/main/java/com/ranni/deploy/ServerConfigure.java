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

    // ==================================== 属性字段 ====================================

    /**
     * 是否自动解压war包
     */
    private boolean unpackWARS;

    /**
     * 服务器引擎
     */
    private EngineConfigure engine;

    /**
     * 服务集合
     */
    private List<ServiceConfigure> services;
    
    /**
     * 服务器全限定类名
     */
    private String serverClass = "com.ranni.core.StandardServer";

    /**
     * 服务全限定类名
     */
    private String serviceClass = "com.ranni.core.StandardService";


    // ==================================== getter and setter ====================================

    public boolean isUnpackWARS() {
        return unpackWARS;
    }

    public void setUnpackWARS(boolean unpackWARS) {
        this.unpackWARS = unpackWARS;
    }

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

    public String getServerClass() {
        return serverClass;
    }

    public void setServerClass(String serverClass) {
        this.serverClass = serverClass;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }
}


