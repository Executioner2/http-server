package com.ranni.startup;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/15 15:25
 */
public final class WebApplication {
    
    private WebApplication() {}


    /**
     * 运行webapp
     * 
     * @param clazz
     * @param args
     */
    public static void run(Class<?> clazz, String[] args) {
        System.out.println(WebApplication.class.getClassLoader());
        System.out.println(Thread.currentThread().getContextClassLoader());
        for (int i = 0; i < 10; i++) {
            try {
                System.out.println("sleep 10s, left time: " + (10 - i));
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
