package com.ranni.startup;

import com.ranni.container.Engine;
import com.ranni.deploy.ServerConfigure;

/**
 * Title: HttpServer
 * Description:
 * 启动服务器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/15 15:46
 */
public final class Bootstrap {
    public static final ServerStartup serverStartup = StandardServerStartup.getInstance();
    
    public static void main(String[] args) {
        serverStartup.setServerStartup(true);
        startup();
    }


    /**
     * 启动服务器
     */
    public static void startup() {
        if (!serverStartup.getStarted()) {
            try {
                serverStartup.startup();
            } catch (Exception e) {
                System.err.println("服务器启动失败！");
                e.printStackTrace();
                return;
            }
        }
    }


    /**
     * 服务器启动了才会有Engine
     * 
     * @return
     */
    public static Engine getEngine() {
        return serverStartup.getEngine();
    }


    /**
     * 取得配置信息
     * 
     * @return
     */
    public static ServerConfigure getServerConfigure () {
        return serverStartup.getServerMap().getServerConfigure();
    }
    
}
