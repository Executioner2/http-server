package com.ranni.startup;

import com.ranni.common.SystemProperty;
import com.ranni.container.Engine;
import com.ranni.deploy.ServerConfigure;
import com.ranni.loader.CommonClassLoader;

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
        String base = System.getProperty("user.dir"); // 服务器根目录
        if (base.endsWith("\\bin")) {
            base = base.substring(0, base.length() - 4);
        }
        System.setProperty(SystemProperty.SERVER_BASE, base);
        
        // 设置类加载器
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);
//        serverStartup.setServerStartup(true);
//        startup();
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
