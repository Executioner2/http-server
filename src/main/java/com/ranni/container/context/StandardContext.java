package com.ranni.container.context;

import com.ranni.common.Globals;
import com.ranni.common.SystemProperty;
import com.ranni.container.*;
import com.ranni.container.host.StandardHost;
import com.ranni.container.scope.ApplicationContext;
import com.ranni.deploy.*;
import com.ranni.exception.LifecycleException;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.resource.BaseDirContext;
import com.ranni.resource.FileDirContext;
import com.ranni.resource.ProxyDirContext;
import com.ranni.resource.WARDirContext;
import com.ranni.util.CharsetMapper;
import com.ranni.util.LifecycleSupport;
import com.ranni.util.RequestUtil;

import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 简单context容器实现类，等该实现类足够完整再晋升为标准实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-28 17:29
 */
public class StandardContext extends ContainerBase implements Context, Lifecycle {
    private ApplicationContext context; // servlet的全局作用域
    private String docBase; // web应用程序文档根目录
    private boolean crossContext; // 跨servlet访问
    private String displayName; // 显示的名称
    private boolean distributable; // 可分发标志
    private Map exceptionPages = new HashMap(); // 异常页面，以异常的全限定类名作为key
    private Map filterConfigs = new HashMap(); // 过滤器配置，以过滤器名作为key
    private Map filterDefs = new HashMap(); // 过滤器定义，以过滤器名作为key
    private boolean useNaming = true; // 是否使用JNDI
    private boolean swallowOutput;
    private String[] wrapperLifecycles = new String[0]; // wrapper生命周期监听器类名
    private String[] wrapperListeners = new String[0]; // wrapper监听器类名
    private String[] applicationListeners = new String[0]; // 应用程序监听器类名，按照在web.xml文件中出现顺序排列
    private String[] instanceListeners = new String[0]; // 将新创建的Wrapper实例监听器加入到该集合
    private ApplicationParameter[] applicationParameters = new ApplicationParameter[0]; // 应用程序参数集
    private Map<String, String> parameters = new HashMap<>(); // 参数集合
    private NamingResources namingResources = new NamingResources(); // 命名资源管理实例
    private String namingContextName; // 命名容器全名
    private boolean paused; // 是否开始接收请求
    private String workDir; // 工作目录
    private String mapperClass = "com.ranni.container.context.StandardContextMapper"; // 默认映射器

    protected boolean cachingAllowed = true; // 是否允许在代理容器对象中缓存目录容器中的资源
    protected String servletClass; // 要加载的servlet类全限定名
    protected Map<String, String> servletMappings = new HashMap<>(); // 请求servlet与wrapper容器的映射
    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具实例
    protected boolean filesystemBased; // 关联的目录容器是否是文件类型的目录容器

    public StandardContext() {
        pipeline.setBasic(new StandardContextValve(this));
    }


    /**
     * 返回默认的映射器
     *
     * @return
     */
    public String getMapperClass() {
        return mapperClass;
    }

    /**
     * 设置默认的映射器
     *
     * @param mapperClass
     */
    public void setMapperClass(String mapperClass) {
        this.mapperClass = mapperClass;
    }

    /**
     * 取得工作目录
     *
     * @return
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * 设置工作目录
     *
     * @param workDir
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;

        // 如果容器在运行时更改了工作目录就要重新配置
        if (started)
            postWorkDirectory();
    }

    public boolean isSwallowOutput() {
        return swallowOutput;
    }

    public void setSwallowOutput(boolean swallowOutput) {
        this.swallowOutput = swallowOutput;
    }

    /**
     * 是否开始接收请求
     *
     * @return
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 设置是否开始接受请求
     *
     * @param paused
     */
    private void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * 设置是否使用JNDI
     *
     * @param useNaming
     */
    public void setUseNaming(boolean useNaming) {
        this.useNaming = useNaming;
    }


    /**
     * 是否使用JNDI
     *
     * @return
     */
    public boolean isUseNaming() {
        return this.useNaming;
    }


    /**
     * 返回servlet context（全局作用域）
     *
     * @return
     */
    @Override
    public ServletContext getServletContext() {
        if (context == null)
            context = new ApplicationContext(getBasePath(), this);
        return context;
    }

    /**
     * 取得基本路径
     *
     * @return
     */
    private String getBasePath() {
        String docBase = null;
        Container container = this;

        // 向上查询Host容器
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }

        if (container == null) {
            docBase = (new File(engineBase(), getDocBase())).getPath();
        } else {
            File file = new File(getDocBase());
            if (!file.isAbsolute()) {
                String appBase = ((Host) container).getAppBase();
                file = new File(appBase);
                if (!file.isAbsolute()) {
                    file = new File(engineBase(), appBase);
                }
                docBase = (new File(file, getDocBase())).getPath();
            } else {
                docBase = file.getPath();
            }
        }

        return docBase;
    }

    /**
     * 是否是文件目录的目录容器
     *
     * @return
     */
    public boolean isFilesystemBased() {
        return filesystemBased;
    }


    /**
     * 设置目录容器
     *
     * @param resources
     */
    @Override
    public synchronized void setResources(DirContext resources) {
        if (resources instanceof BaseDirContext) {
            ((BaseDirContext) resources).setCached(isCachingAllowed());
        }
        if (resources instanceof FileDirContext) {
            filesystemBased = true;
        }
        super.setResources(resources);

        // 将此目录容器存入Servlet的全局作用域中
        if (started)
            postResources();
    }


    /**
     * 存入到Servlet的全局作用域中
     */
    private void postResources() {
        getServletContext().setAttribute(Globals.RESOURCES_ATTR, getResources());
    }


    /**
     * 设置是否允许缓存目录容器中的资源
     *
     * @param cachingAllowed
     */
    public void setCachingAllowed(boolean cachingAllowed) {
        this.cachingAllowed = cachingAllowed;
    }

    /**
     * 返回是否允许缓存目录容器中的资源
     *
     * @return
     */
    private boolean isCachingAllowed() {
        return this.cachingAllowed;
    }

    /**
     * 取得命名容器的全名
     *
     * @return
     */
    private String getNamingContextName() {
        if (namingContextName == null) {
            // 尝试取得命名容器全名
            Container parent = getParent();
            if (parent == null) {
                namingContextName = getName();
            } else {
                Deque<String> deque = new LinkedList<>();
                StringBuffer sb = new StringBuffer();
                while (parent != null) {
                    deque.push(parent.getName());
                    parent = parent.getParent();
                }

                while (!deque.isEmpty()) {
                    sb.append("/" + deque.pop());
                }
                sb.append("/" + getName());
                namingContextName = sb.toString();
            }
        }

        return namingContextName;
    }


    /**
     * 返回服务器的文件根目录
     *
     * @return
     */
    protected File engineBase() {
        return new File(System.getProperty("ranni.base"));
    }

    @Override
    public CharsetMapper getCharsetMapper() {
        return null;
    }

    @Override
    public Object[] getApplicationListeners() {
        return new Object[0];
    }

    @Override
    public void setApplicationListeners(Object[] listeners) {

    }

    @Override
    public boolean getAvailable() {
        return false;
    }

    @Override
    public void setAvailable(boolean available) {

    }

    @Override
    public void setCharsetMapper(CharsetMapper mapper) {

    }

    @Override
    public boolean getConfigured() {
        return false;
    }

    @Override
    public void setConfigured(boolean configured) {

    }

    @Override
    public boolean getCookies() {
        return false;
    }

    @Override
    public void setCookies(boolean cookies) {

    }

    /**
     * 跨servlet访问
     *
     * @return
     */
    @Override
    public boolean getCrossContext() {
        return crossContext;
    }

    @Override
    public void setCrossContext(boolean crossContext) {

    }

    /**
     * 返回容器显示名字
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * 设置容器显示名字
     *
     * @param displayName
     */
    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean getDistributable() {
        return false;
    }

    @Override
    public void setDistributable(boolean distributable) {

    }

    /**
     * 返回此web应用程序的文档根目录
     *
     * @return
     */
    @Override
    public String getDocBase() {
        return this.docBase;
    }

    /**
     * 设置这个web应用程序的文档路径
     *
     * @param docBase
     */
    @Override
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    /**
     * 返回此容器的名字
     *
     * @return
     */
    @Override
    public String getPath() {
        return getName();
    }

    /**
     * 设置容器在URL中的路径
     *
     * @param path
     */
    @Override
    public void setPath(String path) {
        setName(RequestUtil.URLDecode(path));
    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public void setPublicId(String publicId) {

    }

    @Override
    public boolean getReloadable() {
        return false;
    }

    @Override
    public void setReloadable(boolean reloadable) {

    }

    @Override
    public boolean getOverride() {
        return false;
    }

    @Override
    public void setOverride(boolean override) {

    }

    @Override
    public boolean getPrivileged() {
        return false;
    }

    @Override
    public void setPrivileged(boolean privileged) {

    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int timeout) {

    }

    @Override
    public String getWrapperClass() {
        return null;
    }

    @Override
    public void setWrapperClass(String wrapperClass) {

    }


    /**
     * 设置应用监听器
     * 如果有重复的监听器则不添加
     *
     * @param listener
     */
    @Override
    public void addApplicationListener(String listener) {
        synchronized (applicationListeners) {
            String[] strings = new String[applicationListeners.length + 1];

            for (int i = 0; i < applicationListeners.length; i++) {
                if (listener.equals(applicationListeners[i])) {
                    return;
                }
                strings[i] = applicationListeners[i];
            }

            strings[applicationListeners.length] = listener;
            applicationListeners = strings;
        }
    }


    /**
     * 批量添加应用程序监听器，有重复的则不添加重复的
     *
     * @param listeners
     */
    @Override
    public void addApplicationListener(String[] listeners) {
        if (listeners == null || listeners.length == 0) return;

        synchronized (applicationListeners) {
            Set<String> set = new HashSet<>(applicationListeners.length);
            List<String> list = new ArrayList<>(listeners.length);

            for (int i = 0; i < applicationListeners.length; i++) {
                set.add(applicationListeners[i]);
            }

            for (int i = 0; i < listeners.length; i++) {
                if (set.contains(listeners[i])) continue;
                set.add(listeners[i]);
                list.add(listeners[i]);
            }

            String[] strings = new String[set.size()];
            System.arraycopy(applicationListeners, 0, strings, 0, applicationListeners.length);
            for (int i = 0, j = applicationListeners.length; i < list.size(); i++) {
                strings[j++] = list.get(i);
            }

            applicationListeners = strings;
        }
    }

    /**
     * 添加应用程序参数
     * 如果传入的parameter的name与当前容器name相同且不允许重载，那将不会添加到集合中
     *
     * @param parameter
     */
    @Override
    public void addApplicationParameter(ApplicationParameter parameter) {
        if (name.equals(parameter.getName()) && !parameter.isOverride()) return;

        synchronized (applicationParameters) {
            ApplicationParameter[] parameters = new ApplicationParameter[this.applicationParameters.length + 1];
            System.arraycopy(applicationParameters, 0, parameter, 0, applicationParameters.length);
            parameters[applicationParameters.length] = parameter;
            applicationParameters = parameters;
        }
    }


    /**
     * 添加应用程序参数
     * 如果传入的parameters中有name与当前容器name相同且不允许重载，那将不会添加到集合中
     *
     * @param parameters
     */
    @Override
    public void addApplicationParameter(ApplicationParameter[] parameters) {
        if (parameters == null || parameters.length == 0) return;

        List<ApplicationParameter> list = new ArrayList<>(parameters.length);
        for (ApplicationParameter app : parameters) {
            if (name.equals(app.getName()) && !app.isOverride())
                continue;
            list.add(app);
        }

        synchronized (applicationParameters) {
            ApplicationParameter[] apps = new ApplicationParameter[this.applicationParameters.length + list.size()];
            System.arraycopy(applicationParameters, 0, apps, 0, applicationParameters.length);

            for (int i = 0, j = applicationListeners.length; i < list.size(); i++) {
                apps[j++] = list.get(i);
            }

            applicationParameters = apps;
        }
    }

    /**
     * 添加环境
     *
     * @param environment
     */
    @Override
    public void addEnvironment(ContextEnvironment environment) {
        namingResources.addEnvironment(environment);
    }


    /**
     * 添加实例监听器
     *
     * @param listener
     */
    @Override
    public void addInstanceListener(String listener) {
        synchronized (instanceListeners) {
            String[] strings = new String[instanceListeners.length + 1];
            System.arraycopy(instanceListeners, 0, strings, 0, instanceListeners.length);
            strings[instanceListeners.length] = listener;
            instanceListeners = strings;
        }
    }


    /**
     * 批量添加实例监听器
     *
     * @param listeners
     */
    @Override
    public void addInstanceListener(String[] listeners) {
        if (listeners == null || listeners.length == 0) return;

        synchronized (instanceListeners) {
            String[] strings = new String[instanceListeners.length + listeners.length];
            System.arraycopy(instanceListeners, 0, strings, 0, instanceListeners.length);
            for (int i = 0, j = instanceListeners.length; i < listeners.length; i++) {
                strings[j++] = listeners[i];
            }
            instanceListeners = strings;
        }
    }

    /**
     * 添加参数
     *
     * @param name
     * @param value
     *
     * @exception IllegalArgumentException 不允许name和value为null，不允许有重复的name
     */
    @Override
    public void addParameter(String name, String value) {
        if (name == null || value == null)
            throw new IllegalArgumentException("name和value不能为null！");

        if (parameters.containsKey(name))
            throw new IllegalArgumentException("参数已经存在: " + name);

        synchronized (parameters) {
            parameters.put(name, value);
        }
    }

    /**
     * 添加资源
     *
     * @param resource
     */
    @Override
    public void addResource(ContextResource resource) {
        namingResources.addResource(resource);
    }


    /**
     * 添加资源环境类型
     *
     * @param name
     * @param type
     */
    @Override
    public void addResourceEnvRef(String name, String type) {
        namingResources.addResourceEnvRef(name, type);
    }


    /**
     * 添加资源连接
     *
     * @param resourceLink
     */
    @Override
    public void addResourceLink(ContextResourceLink resourceLink) {
        namingResources.addResourceLink(resourceLink);
    }

    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 添加pattern与wrapper对象的映射关系
     *
     * @param pattern
     * @param name
     */
    @Override
    public void addServletMapping(String pattern, String name) {
        synchronized (servletMappings) {
            servletMappings.put(pattern, name);
        }
    }

    /**
     * 添加wrapper容器生命周期监听器类名
     *
     * @param listener
     */
    @Override
    public void addWrapperLifecycle(String listener) {
        synchronized (wrapperLifecycles) {
            String[] strings = new String[wrapperLifecycles.length + 1];
            System.arraycopy(wrapperLifecycles, 0, strings, 0, wrapperLifecycles.length + 1);
            strings[wrapperLifecycles.length] = listener;
            wrapperLifecycles = strings;
        }
    }


    /**
     * 批量添加wrapper容器生命周期监听器类名
     *
     * @param listeners
     */
    @Override
    public void addWrapperLifecycle(String[] listeners) {
        if (listeners == null || listeners.length == 0) return;

        synchronized (wrapperLifecycles) {
            String[] strings = new String[wrapperLifecycles.length + listeners.length];
            System.arraycopy(wrapperLifecycles, 0, strings, 0, wrapperLifecycles.length + 1);
            for (int i = 0, j = wrapperLifecycles.length; i < listeners.length; i++) {
                strings[j++] = listeners[i];
            }
            wrapperLifecycles = strings;
        }
    }

    /**
     * 添加wrapper上的容器监听类名
     *
     * @param listener
     */
    @Override
    public void addWrapperListener(String listener) {
        synchronized (wrapperListeners) {
            String[] strings = new String[wrapperListeners.length + 1];
            System.arraycopy(wrapperListeners, 0, strings, 0, wrapperListeners.length + 1);
            strings[wrapperListeners.length] = listener;
            wrapperListeners = strings;
        }
    }


    /**
     * 批量添加wrapper上的容器监听类名
     *
     * @param listeners
     */
    @Override
    public void addWrapperListener(String[] listeners) {
        if (listeners == null || listeners.length == 0) return;

        synchronized (wrapperListeners) {
            String[] strings = new String[wrapperListeners.length + 1];
            System.arraycopy(wrapperListeners, 0, strings, 0, wrapperListeners.length + 1);
            for (int i = 0, j = wrapperListeners.length; i < listeners.length; i++) {
                strings[j++] = listeners[i];
            }
            wrapperListeners = strings;
        }
    }

    @Override
    public Wrapper createWrapper() {
        return null;
    }

    @Override
    public String[] findApplicationListeners() {
        return new String[0];
    }

    @Override
    public ApplicationParameter[] findApplicationParameters() {
        return new ApplicationParameter[0];
    }

    @Override
    public ContextEnvironment findEnvironment(String name) {
        return null;
    }

    @Override
    public ContextEnvironment[] findEnvironments() {
        return new ContextEnvironment[0];
    }

    @Override
    public String[] findInstanceListeners() {
        return new String[0];
    }

    @Override
    public String findMimeMapping(String extension) {
        return null;
    }

    @Override
    public String[] findMimeMappings() {
        return new String[0];
    }

    @Override
    public String findParameter(String name) {
        return null;
    }

    @Override
    public String[] findParameters() {
        return new String[0];
    }

    @Override
    public ContextResource findResource(String name) {
        return null;
    }

    @Override
    public String findResourceEnvRef(String name) {
        return null;
    }

    @Override
    public String[] findResourceEnvRefs() {
        return new String[0];
    }

    @Override
    public ContextResourceLink findResourceLink(String name) {
        return null;
    }

    @Override
    public ContextResourceLink[] findResourceLinks() {
        return new ContextResourceLink[0];
    }

    @Override
    public ContextResource[] findResources() {
        return new ContextResource[0];
    }

    /**
     * 返回servletMapping中指定pattern对应的wrapper名
     *
     * @param pattern
     * @return
     */
    @Override
    public String findServletMapping(String pattern) {
        synchronized (servletMappings) {
            return servletMappings.get(pattern);
        }
    }

    /**
     * 返回servletMapping所有的key
     *
     * @return
     */
    @Override
    public String[] findServletMappings() {
        synchronized (servletMappings) {
            Set<String> keys = servletMappings.keySet();
            return keys.toArray(new String[keys.size()]);
        }
    }

    @Override
    public String[] findWrapperLifecycles() {
        return new String[0];
    }

    @Override
    public String[] findWrapperListeners() {
        return new String[0];
    }

    /**
     * TODO 容器重载
     */
    @Override
    public synchronized void reload() {
        if (!started)
            throw new IllegalStateException("容器还没有启动！");

        log("开始重载容器！");

        // 停止接收请求
//        setPaused(true);


    }

    @Override
    public void removeApplicationListener(String listener) {

    }

    @Override
    public void removeApplicationParameter(String name) {

    }

    @Override
    public void removeInstanceListener(String listener) {

    }

    @Override
    public void removeParameter(String name) {

    }

    @Override
    public void removeResource(String name) {

    }

    @Override
    public void removeResourceEnvRef(String name) {

    }

    @Override
    public void removeResourceLink(String name) {

    }

    /**
     * 移除servletMapping中指定pattern对应的wrapper
     *
     * @param pattern
     */
    @Override
    public void removeServletMapping(String pattern) {
        synchronized (servletMappings) {
            servletMappings.remove(pattern);
        }
    }

    @Override
    public void removeWrapperLifecycle(String listener) {

    }

    @Override
    public void removeWrapperListener(String listener) {

    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 添加监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * 返回所有监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()}
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * 移除指定监听器
     *
     * @see {@link LifecycleSupport#removeLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * context容器启动
     * 要做的操作：
     *  1、配置资源
     *  2、启动加载器
     *  3、设置工作目录
     *  4、设置默认映射器
     *  5、启动子容器
     *  6、启动管道
     *  7、启动容器自身
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws Exception {
        if (started) throw new LifecycleException("此context容器实例已经启动！");
        boolean ok = true;

        // 容器启动前
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);


        try {
            // 设置资源
            if (getResources() == null) {
                log("开始配置默认资源");
                try {
                    if (docBase != null && docBase.endsWith(".war")) {
                        setResources(new WARDirContext());
                    } else {
                        setResources(new FileDirContext());
                    }
                } catch (IllegalArgumentException e) {
                    log("资源文件初始化失败： " + e.getMessage());
                    ok = false;
                }
            }

            // 设置目录根路径
            if (ok && resources instanceof ProxyDirContext) {
                DirContext dirContext = ((ProxyDirContext) resources).getDirContext();
                if (dirContext instanceof BaseDirContext) {
                    ((BaseDirContext) dirContext).setDocBase(getBasePath());
                }
            }

            // 设置工作目录
            postWorkDirectory();

            // 设置默认映射器
            addDefaultMapper(this.mapperClass);

            // 启动加载器
            if (loader != null && loader instanceof Lifecycle)
                ((Lifecycle) loader).start();

            // 启动子容器
            Container[] children = findChildren();
            for (Container child : children) {
                if (child instanceof Lifecycle)
                    ((Lifecycle) child).start();
            }

            // 启动管道
            if (pipeline instanceof Lifecycle)
                ((Lifecycle) pipeline).start();

            // context容器自身启动
            lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        } catch (Exception e) {
            ok = false;
            log("context容器启动失败： " + e.getMessage());
        }

        if (ok) {
            started = true;
        }

        // 容器启动后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }

    /**
     * 添加默认映射器
     *
     * @param mapperClass
     */
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(mapperClass);
    }

    /**
     * 设置工作目录，把工作目录属性存入ServletContext中
     * 会先查询engine和host的路径，为空的话就用"_"代替
     */
    private void postWorkDirectory() {
        String workDir = getWorkDir();

        if (workDir == null) {
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            Container parentHost = getParent();

            if (parentHost != null) {
                hostName = parentHost.getName();
                // 取得主机的工作路径
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost) parentHost).getWorkDir();
                }

                Container parentEngine = parentHost.getParent();

                if (parentEngine != null) {
                    engineName = parentEngine.getName();
                }
            }

            if (engineName == null || engineName.isEmpty()) {
                engineName = "_";
            }
            if (hostName == null || hostName.isEmpty()) {
                hostName = "_";
            }

            String path = getPath();

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (hostWorkDir != null) {
                workDir = hostWorkDir + File.separator + path;
            } else {
                workDir = "work" + File.separator + engineName +
                        File.separator + hostName + File.separator + path;
            }

            setWorkDir(workDir);
        } // if (workDir == null) end

        // 创建必要的目录
        File dir = new File(workDir);
        if (!dir.isAbsolute()) {
            // 不是绝对路径，那么就取得服务器的工作路径
            File ranniHome = new File(System.getProperty(SystemProperty.SERVER_BASE));
            String ranniHomePath = null;
            try {
                ranniHomePath = ranniHome.getCanonicalPath();
                dir = new File(ranniHomePath, workDir);
            } catch (IOException e) {
                ;
            }
        }
        dir.mkdirs();

        // 添加到ServletContext中并设为只读属性
        getServletContext().setAttribute(Globals.WORK_DIR_ATTR, dir);
        if (getServletContext() instanceof ApplicationContext)
            ((ApplicationContext) getServletContext()).setAttributeReadOnly(Globals.WORK_DIR_ATTR);
    }

    /**
     * 关闭当前容器
     * 关闭顺序
     *  1、容器本身
     *  2、管道
     *  3、子容器
     *  4、加载器
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws Exception {

        if (!started) throw new LifecycleException("此context容器已处于关闭状态！");

        // 关闭context容器之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);
        // 关闭context容器
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;

        try {
            // 关闭管道
            if (pipeline instanceof Lifecycle)
                ((Lifecycle) pipeline).stop();

            // 关闭子容器
            Container[] children = findChildren();
            for (Container child : children) {
                if (child instanceof Lifecycle)
                    ((Lifecycle) child).stop();
            }

            // 关闭加载器
            if (loader != null && loader instanceof Lifecycle)
                ((Lifecycle) loader).stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
