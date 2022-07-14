package com.ranni.startup;

import com.ranni.common.Globals;
import com.ranni.connector.Connector;
import com.ranni.connector.CoyoteAdapter;
import com.ranni.connector.Mapper;
import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.container.host.StandardHost;
import com.ranni.container.scope.ApplicationContext;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ApplicationConfigure;
import com.ranni.loader.WebappLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
    private static final ServerStartup serverStartup = StandardServerStartup.getInstance();
    
    private WebApplication() {}


    /**
     * 运行webapp
     * 通过资源定位协议来判断启动方式
     * 
     * 若是方式一： 
     * 需要先创建服务器的类加载器com.ranni.loader.CommonClassLoader
     * 然后启动服务器，之后实例化一个context容器添加到服务器中
     * 注意：被打成jar包后，webapp路径应该为/BOOT-INF/classes而非/WEB-INF/classes
     * 
     * 若是方式二：
     * 只用创建服务器的类加载器com.ranni.loader.WebappClassLoader
     * 然后启动服务器。
     * 注意：方式二启动的根路径为webapp的项目路径，而非服务器的根路径。需要先进入target目录
     * 
     * @param clazz
     * @param args
     */
    public static void run(Class<?> clazz, String[] args) {
        URL url = clazz.getResource("");
        String protocol = url.getProtocol();
        
        // 通过url协议判断是哪种方式启动
        if ("file".equals(protocol)) {            
            // 通过webapp启动类取得webapp所在的路径
            // XXX - 待多环境测试
            String packagePath = "/WEB-INF/classes/" + clazz.getPackageName().replaceAll("\\.", "/") + "/";
            String path = url.getPath().substring(0, url.getPath().lastIndexOf(packagePath));
            String docBase = "";
            int index = path.lastIndexOf('/');
            
            if (index > -1) {
                docBase = path.substring(index);
                path = path.substring(0, index);
            }
            
            // 解析配置文件
            try {
                
                if (serverStartup.getConfigureParse() == null)
                    serverStartup.setConfigureParse(new ServerConfigureParse());
                
                if (!serverStartup.getInitialized())
                    serverStartup.initialize();
                
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            
            
            try {
                // 解析application.yaml配置文件
                ClassLoader ccl = clazz.getClassLoader();
                InputStream resource = ccl.getResourceAsStream(Constants.APPLICATION_YAML);

                if (resource == null)
                    resource = ccl.getResourceAsStream(Constants.APPLICATION_YML);
                
                ConfigureParse<Context, ApplicationConfigure> parse = new ApplicationConfigureParse(clazz);
                ApplicationConfigure configure = parse.parse(resource).getConfigure();
                
                if (configure.getPath() == null) {
                    configure.setPath(docBase);
                }
                if (configure.getDocBase() == null) {
                    configure.setDocBase(docBase);
                }
                if (configure.getWorkDir() == null) {
                    configure.setWorkDir(path);
                }
                
                Context context = fitApplicationConfigure(clazz, serverStartup.getServer(), configure);

                // 加入到服务器中
                Engine engine = serverStartup.getEngine();
                Host host = (Host) engine.findChild(configure.getHost());
                engine.setDefaultHost(host.getName()); // 设置默认主机

                // 设置host
                Mapper mapper = engine.getService().getMapper();
                mapper.addHost(host, new String[0]);
                mapper.addContext(context, null);

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            
            // 启动服务器（如果服务器没有启动的话）
            if (!serverStartup.getStarted()) {
                try {
                    serverStartup.startup();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }    
            }            

        } else if ("jar".equals(protocol)) {
            // 通过jar取得
            System.out.println("jar");
            // TODO 待实现
            
        } else {
            throw new IllegalStateException("不合法的启动方式！");
        }
          
    }

    
    private static Context fitApplicationConfigure(Class webappBootstrapClazz, Server server, ApplicationConfigure configure) {
        URL url = webappBootstrapClazz.getResource("");
        StandardContext context = null;
        Connector connector = null;
        String docBase = configure.getDocBase();
        String path = configure.getPath();
        
        if ("file".equals(url.getProtocol())) {
            // 文件URL协议
            connector = new Connector();
            context = new StandardContext();

            Service[] services = server.findServices();
            Service service = services[services.length - 1];
            Engine engine = service.getContainer();
            Host host = (Host) engine.findChild(configure.getHost());
            Mapper mapper = new Mapper();
            service.setMapper(mapper);

            // 如果没有host，就新建一个
            if (host == null) {
                host = new StandardHost();
                host.setAppBase(configure.getWorkDir());
                host.setName(configure.getHost());
                engine.addChild(host);
            } else {
                // 把默认server配置文件中的webapps路径改掉
                host.setAppBase(configure.getWorkDir());
            }

            host.addChild(context);
            context.setDocBase(docBase);
            context.setPath(path);
            context.setReloadable(configure.isReloadable());
            context.setBackgroundProcessorDelay(configure.getBackgroundProcessorDelay());
            WebappLoader webappLoader = new WebappLoader();
            context.setLoader(webappLoader);
            ContextConfig contextConfig = new ContextConfig();
            context.addLifecycleListener(contextConfig);
            context.getServletContext().setAttribute(Globals.APPLICATION_BOOTSTRAP_CLASS, webappBootstrapClazz);
            ((ApplicationContext) context.getServletContext()).setAttributeReadOnly(Globals.APPLICATION_BOOTSTRAP_CLASS);

            connector.setDebug(configure.getDebug());
            connector.setPort(configure.getPort());
            connector.setAddress(configure.getIp());
            connector.setScheme(configure.getScheme());
            connector.setAdapter(new CoyoteAdapter(connector));

            // 将服务和连接器关联
            for (String serviceName : configure.getServices()) {
                service = server.findService(serviceName);
                service.addConnector(connector);
            }
        }
        
        return context;
    } 
    
    

    /**
     * 指定服务器配置文件
     * 
     * @param path
     */
    public static void setServerConfigurePath(String path) {
        serverStartup.setServerConfigurePath(path);
    }

}
