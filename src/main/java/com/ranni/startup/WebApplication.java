package com.ranni.startup;

/**
 * Title: HttpServer
 * Description:
 * 通过以下两种方式启动webapp将用到此类
 * 
 * 方式一：
 * 经spring-boot-maven-plugin打包插件打成all in one的jar包
 * 此类通过org.springframework.boot.loader.LaunchedURLClassLoader被加载
 * 然后该webapp可以独立启动
 * 
 * 方式二：
 * webapp在开发阶段还未打包，经启动类启动。
 * 此类通过jdk.internal.loader.ClassLoaders$AppClassLoader被加载
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/15 15:25
 */
public final class WebApplication {
    
    private WebApplication() {}


    /**
     * 运行webapp
     * 可以通过WebApplication的类加载器判断是哪种启动方式
     * 
     * 若是方式一： 
     * 需要先创建服务器的类加载器com.ranni.loader.CommonClassLoader
     * 然后启动服务器，之后实例化一个context容器添加到服务器中
     * 注意：被打成jar包后，webapp路径应该为/BOOT-INF/classes而非/WEB-INF/classes
     * 
     * 若是方式二：
     * 同样需要创建服务器的类加载器com.ranni.loader.CommonClassLoader
     * 然后启动服务器。
     * 注意：方式二启动的根路径为webapp的项目路径，而非服务器的根路径。需要先进入target目录
     * 
     * @param clazz
     * @param args
     */
    public static void run(Class<?> clazz, String[] args) {
        System.out.println(System.getProperty("user.dir"));
        
        // 通过WebApplication的类加载器判断是哪种启动方式
        // FIXME - 需要更改判定方式
        if (WebApplication.class.getClassLoader().getClass().toString().equals("class jdk.internal.loader.ClassLoaders$AppClassLoader")) {
            
            
            
        } else if (WebApplication.class.getClassLoader().getClass().toString().equals("class org.springframework.boot.loader.LaunchedURLClassLoader")) {
            
            
            
        } else {
            throw new IllegalStateException("不合法的类加载器！");
        }
        
        
//        for (int i = 0; i < 10; i++) {
//            try {
//                System.out.println("sleep 10s, left time: " + (10 - i));
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        // 启动服务器，如果服务器未启动的话
//        Bootstrap.startup();
//
//        Engine engine = Bootstrap.getEngine();
//        
//        // 取得host路径和context路径
//        System.out.println("AMD YES");
//        String path = clazz.getResource("/").getPath();
//        path = path.substring(0, path.lastIndexOf("/WEB-INF/classes/"));
//        String docBase = path.substring(path.lastIndexOf("/") + 1);
//        path = path.substring(0, path.lastIndexOf("/"));
//        String appBase = path.substring(path.lastIndexOf("/") + 1);
//
//        // 创建一个属于该webapp的类加载器
//        WebappLoader webappLoader = new WebappLoader();
//        StandardContext context = new StandardContext();
//
//        ServerConfigure serverConfigure = Bootstrap.getServerConfigure();
//        EngineConfigure engineConfigure = serverConfigure.getEngine();
//        List<HostConfigure> hosts = engineConfigure.getHosts();
        
        
    }
}
