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
    private boolean unpackWARS; // 是否自动解压war包
    private EngineConfigure engine; // 服务器引擎
    private List<ServiceConfigure> services; // 服务集合
    private float scanThreadDivisor = 0.4f; // 扫描线程数量因子

    public float getScanThreadDivisor() {
        return scanThreadDivisor;
    }

    public void setScanThreadDivisor(float scanThreadDivisor) {
        this.scanThreadDivisor = scanThreadDivisor;
    }

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
}


