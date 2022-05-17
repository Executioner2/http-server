package com.ranni.startup;

import com.ranni.common.Constants;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.container.host.StandardHost;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ApplicationConfigure;
import com.ranni.loader.WebappLoader;

import java.io.File;
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
     * @return  返回连接器而非容器
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
        String path = "";
        String docBase = "";
        String appBase = "";

        if ("file".equals(url.getProtocol())) {
            // 文件URL协议，webapp必须从/WEB-INF/classes开始
            String packagePath = prefix + File.separator + webappBootstrapClazz.getPackageName().replaceAll("\\.", "/") + "/";
            int pos = url.toString().lastIndexOf(packagePath);
            String fullPath = "";
            
            if (Constants.WEBAPP_CLASSES.equals(prefix)) {
                if (pos > 6) {
                    path = url.toString().substring(6, pos);
                    fullPath = path;
                } else {
                    throw new IllegalArgumentException("ApplicationConfigureParse.fit  路径错误！");
                }

                pos = fullPath.lastIndexOf("/");
                if (pos >= 0 && pos + 1 < path.length()) {
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
            context.setPath(path);
            context.setReloadable(applicationConfigure.isReloadable());
            context.setBackgroundProcessorDelay(applicationConfigure.getBackgroundProcessorDelay());
            WebappLoader webappLoader = new WebappLoader();
            webappLoader.setClassesPath(prefix);
            context.setLoader(webappLoader);
            ContextConfig contextConfig = new ContextConfig();
            context.addLifecycleListener(contextConfig);

            connector.setDebug(applicationConfigure.getDebug());
            connector.setContainer(context);
            connector.setPort(applicationConfigure.getPort());
            connector.setAddress(applicationConfigure.getIp());
            
            // 将服务和连接器关联
            for (String serviceName : applicationConfigure.getServices()) {
                Service service = server.findService(serviceName);
                service.addConnector(connector);
            }
        }

        // FIXME - 当url协议是jar时的处理未实现
        
        return connector;
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
