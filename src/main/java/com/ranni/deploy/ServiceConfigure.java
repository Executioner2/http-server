package com.ranni.deploy;

import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * 服务配置
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 18:26
 */
public final class ServiceConfigure {
    private String name; // 服务名
    private String connectorClass = "com.ranni.connector.HttpConnector"; // 连接器实现类名
    private List<ConnectorConfigure> connectors; // 连接器集合

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

    public List<ConnectorConfigure> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<ConnectorConfigure> connectors) {
        this.connectors = connectors;
    }
}
