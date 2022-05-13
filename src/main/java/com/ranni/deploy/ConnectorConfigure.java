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
public final class ConnectorConfigure {
    private String clazz; // 连接器实现类名
    private int debug = Logger.WARNING; // debug级别

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
