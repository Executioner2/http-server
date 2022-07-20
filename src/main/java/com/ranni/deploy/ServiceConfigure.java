package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * 服务配置
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 18:26
 */
public final class ServiceConfigure {

    // ==================================== 属性字段 ====================================
    
    /**
     * 服务名
     */
    private String name;

    /**
     * 实现类全限定类名
     */
    private String serviceClass;

    /**
     * 连接器实现类名
     */
    private String connectorClass = "com.ranni.connector.HttpConnector";

    
    // ==================================== getter and setter ====================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectorClass() {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass) {
        this.connectorClass = connectorClass;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }
    
}
