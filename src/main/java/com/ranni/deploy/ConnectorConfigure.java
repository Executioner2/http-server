package com.ranni.deploy;

import com.ranni.logger.Logger;

/**
 * Title: HttpServer
 * Description:
 * 连接器配置
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 21:05
 */
@Deprecated
public final class ConnectorConfigure {
    private String clazz; // 连接器实现类名
    private int debug = Logger.WARNING; // debug级别
    private int port = 8080; // 端口号
    private String ipaddress; // ip地址
    private int acceptCount = 10; // 最大连接数
    private String scheme = "http"; // 协议类型
    private int redirectPort = 80; // 转发端口号

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public int getAcceptCount() {
        return acceptCount;
    }

    public void setAcceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public int getRedirectPort() {
        return redirectPort;
    }

    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }
}
