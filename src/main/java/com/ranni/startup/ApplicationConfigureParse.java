package com.ranni.startup;

import com.ranni.common.Constants;
import com.ranni.common.Globals;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.container.host.StandardHost;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ApplicationConfigure;
import com.ranni.loader.WebappLoader;

import java.net.URL;

/**
 * Title: HttpServer
 * Description:
 * 解析Context配置文件(Application.yaml)
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 14:39
 */
public class ApplicationConfigureParse extends ConfigureParseBase {
    private Class webappBootstrapClazz; // webapp启动类
    private String prefix = Constants.WEBAPP_CLASSES; // 路径前缀
    private Server server; // 服务器

    public ApplicationConfigureParse(Class webappBootstrapClazz) {
        this(webappBootstrapClazz, ApplicationConfigure.class);
    }

    public ApplicationConfigureParse(Class webappBootstrapClazz, Class clazz) {
        super(clazz);
        this.webappBootstrapClazz = webappBootstrapClazz;
    }


    /**
     * 装配
     * 
     * @param   load
     * @return  返回context容器
     * @throws  Exception
     */
    @Override
    protected Object fit(Object load) throws Exception {
        if (webappBootstrapClazz == null)
            throw new IllegalArgumentException("ApplicationConfigureParse.fit  填充失败！webapp的启动类不能为null！");
        
        ApplicationConfigure applicationConfigure = (ApplicationConfigure) load;
        URL url = webappBootstrapClazz.getResource("");
        StandardContext context = null;
        HttpConnector connector = null;
        String docBase = "";
        String appBase = "";

        if ("file".equals(url.getProtocol())) {
            // 文件URL协议
            String packagePath = prefix + "/" + webappBootstrapClazz.getPackageName().replaceAll("\\.", "/") + "/";
            String fullPath = "";
            
            if (!url.toString().endsWith(packagePath)) {
                throw new IllegalArgumentException("ApplicationConfigureParse.fit  路径错误！");
            }
            
            // 标准的类仓库，剪得host和context的路径
            if (Constants.WEBAPP_CLASSES.equals(prefix)) {
                int pos = url.toString().lastIndexOf(packagePath);
                fullPath = url.toString().substring(6, pos);
                pos = fullPath.lastIndexOf("/");
                
                if (pos >= 0 && pos + 1 < fullPath.length()) {
                    docBase = fullPath.substring(pos + 1);
                    fullPath = fullPath.substring(0, pos);
                }
                
                pos = fullPath.lastIndexOf("/");
                
                if (pos >= 0) {
                    appBase = fullPath.substring(pos);
                }
            }            

            connector = new HttpConnector();
            context = new StandardContext();

            Engine engine = (Engine) server.findServices()[0].getContainer();
            Host host = (Host) engine.findChild(applicationConfigure.getHost());

            // 如果没有host，就新建一个
            if (host == null) {
                host = new StandardHost();
                host.setAppBase(appBase);
                host.setName(applicationConfigure.getHost());
                engine.addChild(host);
            }
            context.setDocBase(docBase);
            context.setPath("/" + docBase);
            context.setReloadable(applicationConfigure.isReloadable());
            context.setBackgroundProcessorDelay(applicationConfigure.getBackgroundProcessorDelay());
            WebappLoader webappLoader = new WebappLoader();
            webappLoader.setClassesPath(prefix);
            context.setLoader(webappLoader);
            ContextConfig contextConfig = new ContextConfig();
            context.addLifecycleListener(contextConfig);
            context.getServletContext().setAttribute(Globals.APPLICATION_BOOTSTRAP_CLASS, webappBootstrapClazz);

            connector.setDebug(applicationConfigure.getDebug());
            connector.setPort(applicationConfigure.getPort());
            connector.setAddress(applicationConfigure.getIp());
            
            // 将服务和连接器关联
            for (String serviceName : applicationConfigure.getServices()) {
                Service service = server.findService(serviceName);
                service.addConnector(connector);
            }
        }

        // FIXME - 当url协议是jar时的处理未实现
        
        return context;
    }


    /**
     * 返回服务器实例
     * 
     * @return
     */
    public Server getServer() {
        return server;
    }


    /**
     * 设置服务器实例
     * 
     * @param server
     */
    public void setServer(Server server) {
        this.server = server;
    }
    

    /**
     * 返回webapp启动类路径剪切部分的前缀
     * 
     * @return
     */
    public String getPrefix() {
        return prefix;
    }


    /**
     * 设置webapp启动类路径剪切部分的前缀
     * 
     * @param prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    

    /**
     * 返回webapp启动类的URL
     * 
     * @return
     */
    public Class getWebappBootstrapClazz() {
        return this.webappBootstrapClazz;
    }
}
