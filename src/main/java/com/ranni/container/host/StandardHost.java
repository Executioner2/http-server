package com.ranni.container.host;

import com.ranni.container.*;
import com.ranni.container.lifecycle.LifecycleException;
import com.ranni.container.pip.ErrorDispatcherValve;
import com.ranni.container.pip.Valve;

import java.util.HashSet;
import java.util.Set;

/**
 * Title: HttpServer
 * Description:
 * 标准的Host容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 15:01
 */
public class StandardHost extends ContainerBase implements Host {
    private String workDir; // 工作目录
    private DefaultContext defaultContext; // 默认容器配置
    private String errorReportValveClass = "com.ranni.container.pip.ErrorReportValve"; // 默认的错误报告阀
    private String configClass = "com.ranni.startup.ContextConfig"; // 配置类全限定类名
    private String mapperClass = "com.ranni.container.host.StandardHostMapper"; // 默认的Host映射器
    private Set<String> aliases = new HashSet<>(); // 以HashSet来存取，方便请求查询对应的Host

    protected String appBase = "."; // 根路径
    protected boolean autoDeploy = true; // 自动部署


    public StandardHost() {
        pipeline.setBasic(new StandardHostValve(this));
    }



    /**
     * 返回错误报告阀全限定类名
     *
     * @return
     */
    public String getErrorReportValveClass() {
        return errorReportValveClass;
    }


    /**
     * 设置错误的报告阀全限定类名
     *
     * @param errorReportValveClass
     */
    public void setErrorReportValveClass(String errorReportValveClass) {
        this.errorReportValveClass = errorReportValveClass;
    }


    /**
     * 返回默认的映射器全限定类名
     *
     * @return
     */
    public String getMapperClass() {
        return mapperClass;
    }


    /**
     * 设置默认的映射器类名
     *
     * @param mapperClass
     */
    public void setMapperClass(String mapperClass) {
        this.mapperClass = mapperClass;
    }


    /**
     * 取得工作路径
     *
     * @return
     */
    public String getWorkDir() {
        return workDir;
    }


    /**
     * 设置工作路径
     *
     * @param workDir
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }


    /**
     * 取得根路径
     *
     * @return
     */
    @Override
    public String getAppBase() {
        return this.appBase;
    }


    /**
     * 设置根路径
     *
     * @param appBase
     */
    @Override
    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }


    /**
     * 返回自动部署标志
     *
     * @return
     */
    @Override
    public boolean getAutoDeploy() {
        return this.autoDeploy;
    }


    /**
     * 设置自动部署标志
     *
     * @param autoDeploy
     */
    @Override
    public void setAutoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
    }


    /**
     * 设置默认Context容器
     * 如果默认容器不为空，则要先停掉原来的
     *
     * @param defaultContext
     */
    @Override
    public void addDefaultContext(DefaultContext defaultContext) {
        this.defaultContext = defaultContext;
//        if (this.defaultContext != null) {
//            if (started && this.defaultContext instanceof Lifecycle) {
//                try {
//                    ((Lifecycle) this.defaultContext).stop();
//                } catch (LifecycleException e) {
//                    log("StandardHost.stoppingDefaultContext", e);
//                }
//            }
//        }
//
//        this.defaultContext = defaultContext;
//
//        if (started && this.defaultContext instanceof Lifecycle) {
//            try {
//                ((Lifecycle) this.defaultContext).start();
//            } catch (LifecycleException e) {
//                log("StandardHost.defaultContextStartingFail", e);
//            }
//        }
    }


    /**
     * 返回默认的Context容器
     *
     * @return
     */
    @Override
    public DefaultContext getDefaultContext() {
        return this.defaultContext;
    }


    /**
     * 设置容器名字
     * 转小写
     *
     * @param name
     */
    @Override
    public void setName(String name) {
        if (name == null)
            throw new IllegalArgumentException("容器名字不能为空！");
        this.name = name.toLowerCase();
    }


    /**
     * 添加子容器，只能添加Context做子容器
     * 
     * @param child
     */
    @Override
    public void addChild(Container child) {
        if (!(child instanceof Context))
            throw new IllegalArgumentException("StandardHost.addChild  只能添加Context做子容器！");
        
        super.addChild(child);
    }


    /**
     * 导入Context容器
     * 
     * @see {@link com.ranni.container.StandardDefaultContext#importDefaultContext(Context)}
     * 
     * @param context
     */
    @Override
    public void importDefaultContext(Context context) {
        if (this.defaultContext != null)
            this.defaultContext.importDefaultContext(context);
    }


    /**
     * 添加别名
     *
     * @param alias
     */
    @Override
    public void addAlias(String alias) {
        alias = alias.toLowerCase();

        synchronized (aliases) {
            aliases.add(alias);
        }
    }

    /**
     * 查询此别名是否存在
     * 
     * @param server
     * @return
     */
    @Override
    public boolean findAliases(String server) {
        synchronized (aliases) {
            return aliases.contains(server);
        }
    }
    

    /**
     * 返回此虚拟主机所有别名
     *
     * @return
     */
    @Override
    public String[] findAliases() {
        synchronized (aliases) {
            return this.aliases.toArray(new String[aliases.size()]);
        }
    }


    /**
     * 请求取得URI中对应的context容器
     * 从请求的URI中获取容器名。
     * 从最后一个'/'开始，依次往前截取URI尝试匹配，如果URI中没有匹配到，就返回默认Context容器（如果有的话）
     * 
     * 例如：/myweb/user/find?id=123
     * 依次匹配：
     *  1、/myweb/user/find?id=123
     *  2、/myweb/user
     *  3、/myweb
     *
     * @param uri
     * @return
     */
    @Override
    public Context map(String uri) {
        if (debug > 0)
            log("请求URI  " + uri);
        
        if (uri == null)
            return null;

        if (debug > 1)
            log("依次尝试从最长的路径前缀取得Context容器");

        Context context = null;
        String prefixUri = uri;
        
        while (true) {
            context = (Context) findChild(prefixUri);
            if (context != null) break;
            int pos = prefixUri.lastIndexOf('/');
            if (pos < 0) break;
            prefixUri = prefixUri.substring(0, pos);
        }

        if (context == null) {
            if (debug > 1)
                log("尝试从默认的context容器中取得");
            context = (Context) findChild("");
        }

        if (context == null) {
            log("未能取得context容器  " + uri);
        } else if (debug > 0) {
            log("成功取得context容器  " + context.getPath());
        }

        return context;
    }


    /**
     * 移除别名
     *
     * @param alias
     */
    @Override
    public void removeAlias(String alias) {
        alias = alias.toLowerCase();

        synchronized (aliases) {
            aliases.remove(alias);
        }
    }


    @Override
    public String getInfo() {
        return null;
    }
    

    @Override
    public void backgroundProcessor() {
        
    }

    
    /**
     * 添加默认的映射器
     * 固定为StandardHost中的mapperClass属性（该属性可以修改）
     * 
     * @param mapperClass
     */
    @Override
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(this.mapperClass);
    }


    /**
     * 启动Host容器
     *
     * @throws LifecycleException
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (errorReportValveClass != null && !("".equals(errorReportValveClass))) {
            try {
                Valve valve = (Valve) Class.forName(errorReportValveClass).getConstructor().newInstance();
                addValve(valve);
            } catch (Throwable e) {
                log("StandardHost.start  实例化错误报告阀失败！" + errorReportValveClass, e);
            }
        }

        addValve(new ErrorDispatcherValve());
        super.start();
    }
    
}
