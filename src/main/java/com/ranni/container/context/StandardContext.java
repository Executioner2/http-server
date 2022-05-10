package com.ranni.container.context;

import com.ranni.common.Globals;
import com.ranni.common.SystemProperty;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.*;
import com.ranni.container.host.StandardHost;
import com.ranni.container.loader.WebappLoader;
import com.ranni.container.scope.ApplicationContext;
import com.ranni.core.ApplicationFilterConfig;
import com.ranni.core.FilterDef;
import com.ranni.deploy.*;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.naming.*;
import com.ranni.session.StandardManager;
import com.ranni.util.CharsetMapper;
import com.ranni.util.RequestUtil;

import javax.naming.directory.DirContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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
public class StandardContext extends ContainerBase implements Context {
    private ApplicationContext context; // servlet的全局作用域
    private String docBase; // web应用程序文档根目录（相对路径）
    private boolean crossContext; // 跨servlet访问
    private String displayName; // 显示的名称
    private boolean distributable; // 可分发标志
    private Map exceptionPages = new HashMap(); // 异常页面，以异常的全限定类名作为key
    private Map<String, FilterConfig> filterConfigs = new HashMap(); // 过滤器配置，以过滤器名作为key
    private Map<String, FilterDef> filterDefs = new HashMap<>(); // 过滤器定义，以过滤器名作为key
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
    private boolean privileged; // 此容器是否具备特权
    private boolean available; // 此WebApp是否可用
    private boolean configured; // TODO 此Context容器的配置标志，在start()通过生命周期事件触发配置监听器对此context容器进行配置，为true时WebApp才能启动
    private FilterMap[] filterMaps = new FilterMap[0]; // 过滤器映射集合
    private CharsetMapper charsetMapper; // 字符集
    private String charsetMapperClass = "com.ranni.util.CharsetMapper"; // 默认的字符集类
    private String mapperClass = "com.ranni.container.context.StandardContextMapper"; // 默认映射器
    private int count; // 正式进行Session回收任务的倒计时
    private int managerChecksFrequency = 15; // Session回收频率，默认15
    private boolean reloadable; // 容器的重载标志位
    private String publicId; // xml公共id
    private String systemId; // xml系统id

    protected boolean cachingAllowed = true; // 是否允许在代理容器对象中缓存目录容器中的资源
    protected String servletClass; // 要加载的servlet类全限定名
    protected Map<String, String> servletMappings = new HashMap<>(); // 请求servlet与wrapper容器的映射
    protected boolean filesystemBased; // 关联的目录容器是否是文件类型的目录容器


    public StandardContext() {
        pipeline.setBasic(new StandardContextValve(this));
        namingResources.setContainer(this);
    }


    /**
     * 返回Session回收频率
     * 
     * @return
     */
    public int getManagerChecksFrequency() {
        return managerChecksFrequency;
    }


    /**
     * 设置Session回收频率
     * 
     * @param managerChecksFrequency
     */
    public void setManagerChecksFrequency(int managerChecksFrequency) {
        this.managerChecksFrequency = managerChecksFrequency;
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
    public boolean getPaused() {
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
     * 接收处理请求
     * 
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        // 容器暂停中
        while (getPaused()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ;
            }
        }
        super.invoke(request, response);
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


    /**
     * 返回字符集
     * 
     * @return
     */
    @Override
    public CharsetMapper getCharsetMapper() {
        if (this.charsetMapper == null) {   
            // 创建字符集实例
            try {
                Class<CharsetMapper> clazz = (Class<CharsetMapper>) Class.forName(charsetMapperClass);
                this.charsetMapper = clazz.getConstructor().newInstance();
            } catch (Throwable e) {
                this.charsetMapper = new CharsetMapper();
            }
        }
        return this.charsetMapper;
    }

    @Override
    public Object[] getApplicationListeners() {
        return new Object[0];
    }

    @Override
    public void setApplicationListeners(Object[] listeners) {

    }

    /**
     * 当前WebApp可用标志位
     *
     * @return
     */
    @Override
    public boolean getAvailable() {
        return this.available;
    }


    /**
     * 设置此WebApp的可用标志位
     *
     * @param available
     */
    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public void setCharsetMapper(CharsetMapper mapper) {

    }
    

    /**
     * 返回此容器是否正确配置的标志位
     * 
     * @return
     */
    @Override
    public boolean getConfigured() {
        return this.configured;
    }
    

    /**
     * 设置此容器是否正确配置的标志位
     * 
     * @param configured 
     */
    @Override
    public void setConfigured(boolean configured) {
        this.configured = configured;
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

    @Override
    public NamingResources getNamingResources() {
        return null;
    }

    @Override
    public void setNamingResources(NamingResources namingResources) {

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


    /**
     * 设置xml解析系统id
     * 
     * @param systemId
     */
    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }


    /**
     * 返回xml解析系统id
     * 
     * @return
     */
    @Override
    public String getSystemId() {
        return this.systemId;
    }


    /**
     * 返回xml解析所产生的公共id
     * 
     * @return
     */
    @Override
    public String getPublicId() {
        return this.publicId;
    }


    /**
     * 设置xml解析所产生的公共id
     * 
     * @param publicId
     */
    @Override
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }


    /**
     * 返回重载标志位
     * 
     * @return
     */
    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }


    /**
     * 设置容器的重载标志位
     * 
     * @param reloadable
     */
    @Override
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    @Override
    public boolean getOverride() {
        return false;
    }

    @Override
    public void setOverride(boolean override) {

    }


    /**
     * 此容器是否有特权
     *
     * @return
     */
    @Override
    public boolean getPrivileged() {
        return this.privileged;
    }


    /**
     * 设置容器是否具备特权
     *
     * @param privileged
     */
    @Override
    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
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
     * 添加过滤器定义实例
     * 
     * @param filterDef
     */
    @Override
    public void addFilterDef(FilterDef filterDef) {
        synchronized (filterDefs) {
            filterDefs.put(filterDef.getFilterName(), filterDef);
        }
    }


    /**
     * 添加过滤器映射
     * 
     * @param filterMap
     */
    @Override
    public void addFilterMap(FilterMap filterMap) {
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getUrlPattern();
        
        // 合法性检查
        if (findFilterDef(filterName) == null) 
            throw new IllegalArgumentException("StandardContext.addFilterMap->name  没有找到这个名字的定义：" + filterName);
        
        if (servletName == null && urlPattern == null)
            throw new IllegalArgumentException("StandardContext.addFilterMap：servletName或urlPattern为null");
        
        if (servletName != null && urlPattern != null)
            throw new IllegalArgumentException("StandardContext.addFilterMap：此FilterMap已经添加");
        
        synchronized (filterMaps) {
            FilterMap[] newArs = new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, newArs, 0, filterMaps.length);
            newArs[filterMaps.length] = filterMap;
            filterMaps = newArs;
        }
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


    /**
     * 这个filterName是否是定义中的
     * 
     * @param filterName
     * @return
     */
    @Override
    public FilterDef findFilterDef(String filterName) {
        synchronized (filterDefs) {
            return filterDefs.get(filterName);
        }
    }

    @Override
    public FilterDef[] findFilterDefs() {
        return new FilterDef[0];
    }


    /**
     * 返回此容器所有过滤器映射
     * 
     * @return
     */
    @Override
    public FilterMap[] findFilterMaps() {
        return filterMaps;
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
     * 容器重载
     * 要做的事情：
     *  停止Session管理器
     *  停止子容器
     *  停止监听器
     *  清空全局作用域属性
     *  停止过滤器
     *  停止加载器
     *  启动加载器
     *  启动监听器
     *  启动过滤器
     *  重新将资源实例添加到全局作用域中
     *  设置欢迎文件
     *  启动子容器
     *  启动Session管理器
     *  
     */
    @Override
    public synchronized void reload() {
        if (!started)
            throw new IllegalStateException("容器还没有启动！");

        log("开始重载容器！");

        // 停止接收请求
        setPaused(true);

        // 停止Session管理器
        if (manager != null && manager instanceof Lifecycle) {
            try {
                ((Lifecycle) manager).stop();
            } catch (LifecycleException e) {
                log("StandardContext.stoppingManager", e);
            }
        }
        
        // 停止子容器
        for (Container wrapper : findChildren()) {
            if (wrapper instanceof Lifecycle) {
                try {
                    ((Lifecycle) wrapper).stop();
                } catch (LifecycleException e) {
                    log("StandardContext.stoppingWrapper", e);
                }
            }
        }
        
        // 停止监听器
        listenerStop();
        
        // 清空全局作用域属性
        if (context != null)
            context.clearAttributes();
        
        // 停止过滤器
        filterStop();
        
        // 停止加载器
        if (loader != null && loader instanceof Lifecycle) {
            try {
                ((Lifecycle) loader).stop();
            } catch (LifecycleException e) {
                log("StandardContext.stoppingLoader", e);
            }
        }
        
        // 启动加载器
        if (loader != null && loader instanceof Lifecycle) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                log("StandardContext.startingLoader", e);
            }
        }
        
        boolean ok = true;
        
        // 启动监听器
        if (ok) {
            if (!(ok = listenerStart())) {
                log("StandardContext.listenerStartFailed");
                ok = false;
            }
        }
        
        // 启动过滤器
        if (ok) {
            if (!(ok = filterStart())) {
                log("StandardContext.filterStartFailed");
                ok = false;
            }
        }
        
        // 重新将资源实例添加到全局作用域中
        postResources();
        
        // 设置欢迎文件
        postWelcomeFiles();
        
        // 启动子容器
        for (Container wrapper : findChildren()) {
            if (!ok) break;
            
            if (wrapper instanceof Lifecycle) {
                try {
                    ((Lifecycle) wrapper).start();
                } catch (LifecycleException e) {
                    log("StandardContext.wrapperStartFailed");
                    ok = false;
                }
            }
        }

        // TODO 初始化时载入并启动的Servlet
        
        // 启动Session管理器
        if (manager != null && manager instanceof Lifecycle) {
            try {
                ((Lifecycle) manager).start();
            } catch (LifecycleException e) {
                log("StandardContext.mangerStartFailed");
                ok = false;
            }
        }
        
        // 重载失败则置为不可用
        if (ok) {
            log("StandardContext.reload  容器重载成功！");
        } else {
            setAvailable(false);
            log("StandardContext.reload  容器重载失败！");
        }
        
        setPaused(false);

        lifecycle.fireLifecycleEvent(Context.RELOAD_EVENT, null);
    }

    @Override
    public void removeApplicationListener(String listener) {

    }

    @Override
    public void removeApplicationParameter(String name) {

    }

    @Override
    public void removeFilterDef(FilterDef filterDef) {

    }

    @Override
    public void removeFilterMap(FilterMap filterMap) {

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
     * 后台任务
     */
    @Override
    public void backgroundProcessor() {
        if (!started)
            return;
        
        count = (count + 1) % managerChecksFrequency;
        if (getManager() != null && count == 0) {
            try {
                getManager().backgroundProcess();
            } catch (Exception e) {
                log("Session回收异常！ " + e);
            }
        }
        
        // 重载检测
        if (getLoader() != null) {
            if (reloadable && getLoader().modified()) {
                try {
                    Thread.currentThread().setContextClassLoader(StandardContext.class.getClassLoader());
                    reload();
                } finally {
                    if (getLoader() != null) {
                        Thread.currentThread().setContextClassLoader(getLoader().getClassLoader());
                    }
                }
            }
        }
        
        // XXX 释放JAR资源
    }
    

    /**
     * context容器启动
     * 要做的操作：
     *  1、触发BEFORE_START事件
     *  2、将availability设置false
     *  3、将configured设置为false
     *  4、配置资源，设置资源对象的根目录
     *  5、设置载入器
     *  6、设置Session管理器
     *  7、初始化字符集映射器
     *  8、启动与该Context容器相关联的组件
     *  9、启动子容器
     *  10、启动管道对象
     *  11、启动Session管理器
     *  12、开启后台线程
     *  13、触发START事件，在此监听器（ContextConfig实例）会进行一些配置操作，将配置结果返回给configured
     *  14、检查configured属性的值：
     *      如果为true则调用postWelcomePages()方法，并载入需要载入的子容器。将availability设置为true；
     *      若为false则调用stop()方法
     *  15、触发AFTER_START事件
     *  
     *  XXX 后续将Spring融入进来，Spring会有监听器会在此方法中监听START事件
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (started) throw new LifecycleException("此context容器实例已经启动！");
        
        if (debug >= 1)
            log("启动中");

        // 触发BEFORE_START事件
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);

        if (debug >= 1)
            log("Context启动前准备，当前容器可用状态：" + getAvailable());
        
        boolean ok = true;
        setConfigured(false);
        setAvailable(false);
        
        // 配置资源
        if (getResources() == null) {
            if (debug >= 1)
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

        // 设置加载器
        if (getLoader() == null) {
            log("开始配置加载器");
            if (getPrivileged()) {
                if (debug > 1)
                    log("此容器为特殊容器，可用此容器的加载器加载web应用程序的类");
                
                setLoader(new WebappLoader(this.getClass().getClassLoader()));
            } else {
                if (debug > 1)
                    log("此容器为普通容器，要用此容器的父容器的加载器作为web应用程序类的加载类");
                
                setLoader(new WebappLoader(getParentClassLoader()));
            }
        }

        // 设置session管理器
        if (getManager() == null) {
            if (debug >= 1)
                log("开始配置session管理器");
            
            setManager(new StandardManager());
        }
        
        // 初始化字符集
        getCharsetMapper();

        // 设置工作目录
        postWorkDirectory();
        
        // 给此线程设置新的上下文类加载器
        ClassLoader oldCCL = bindThread();

        if (debug >= 1)
            log("Context容器正式启动");
        
        if (ok) {
            try {
                // 添加默认的映射器
                addDefaultMapper(this.mapperClass);
                started = true;

                // 启动加载器
                if (this.loader != null && this.loader instanceof Lifecycle)
                    ((Lifecycle) this.loader).start();
                
                // 启动记录器日志
                if (logger != null && logger instanceof Lifecycle)
                    ((Lifecycle) logger).start();


                // 重新绑定上下文类加载器
                unbindThread(oldCCL);
                oldCCL = bindThread();
                
                // 启动相关组件 （领域，簇，JNDI资源等）
                if (resources != null && resources instanceof Lifecycle)
                    ((Lifecycle) resources).start();

                // 启动映射器
                Mapper mappers[] = findMappers();
                for (Mapper mapper : mappers) {
                    if (mapper instanceof Lifecycle)
                        ((Lifecycle) mapper).start();
                }
                
                // 启动子容器
                Container[] children = findChildren();
                for (Container child : children) {
                    if (child instanceof Lifecycle)
                        ((Lifecycle) child).start();
                }

                // 启动管道
                if (pipeline instanceof Lifecycle)
                    ((Lifecycle) pipeline).start();

                // 启动事件
                lifecycle.fireLifecycleEvent(START_EVENT, null);

                // 启动Session管理器
                if (manager != null && manager instanceof Lifecycle)
                    ((Lifecycle) manager).start();
                
                // 启动后台线程
                threadStart();

            } finally {
                unbindThread(oldCCL);
            }
        }
        
        // 检查配置有无问题
        if (!getConfigured())
            ok = false;

        // 设置JNDI资源到全局作用域中
        if (ok) {
            getServletContext().setAttribute(Globals.RESOURCES_ATTR, getResources());
        }
        
        // 设置欢迎文件
        if (ok) {
            if (debug >= 1)
                log("设置欢迎文件");

            postWelcomeFiles(); // TODO 未实现
        }
        
        // TODO 启动监听器
        if (ok) {
            ok = listenerStart();
        }

        // 启动过滤器
        if (ok) {
            ok = filterStart();
        }

        // TODO 初始化所有加载时启动的servlet
        
        if (ok) {
            if (debug >= 1)
                log("Context容器启动成功！");
            
            // 设置WebApp可用
            setAvailable(true);
        } else {
            log("Context容器启动失败！");
            try {
                stop();
            } catch (Throwable t) {
                log("StandardContext.StartCleanup" + t);
            }
            setAvailable(false);
        }

        // 容器启动后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }


    /**
     * TODO 启动监听器
     */
    private boolean listenerStart() {
        return true;
    }


    /**
     * TODO 设置欢迎文件
     */
    private void postWelcomeFiles() {
    }


    /**
     * 解绑上下文类加载器
     * 
     * @param oldCCL
     */
    private void unbindThread(ClassLoader oldCCL) {
        Thread.currentThread().setContextClassLoader(oldCCL);
        
        DirContextURLStreamHandler.unbind();
    }


    /**
     * 取得与线程绑定的类加载器
     * 如果JNDI资源为null则直接返回与线程绑定的上下文类加载器
     * 否则设置上下文类加载器为从当前context容器取得的类加载器
     * 
     * @return 返回的始终都是此线程原来的上下文类加载器
     */
    private ClassLoader bindThread() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        
        if (getResources() == null) 
            return contextClassLoader;
        
        Thread.currentThread().setContextClassLoader(getLoader().getClassLoader());

        DirContextURLStreamHandler.bind(getResources());
                
        return contextClassLoader;
    }
    

    /**
     * 启动过滤器
     * 实际上过滤器对象并未创建，只是将过滤器的定义信息载入了进来（其实就是创建了将过滤器定义、过滤器以及容器三者关联的对象）
     * 
     * @return
     */
    public boolean filterStart() {
        boolean ok = true;
        
        // 从xml中载入过滤器配置
        synchronized (filterConfigs) {
            filterConfigs.clear();
            Iterator<String> iterator = filterDefs.keySet().iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig(this, filterDefs.get(name));
                    filterConfigs.put(name, filterConfig);
                } catch (Throwable t) {
                    log("StandardContext.filterStart  实例化应用程序过滤器配置失败！ " + filterConfig);
                    ok = false;
                }
            }
        }
        
        return ok;
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
     *  1、后台线程
     *  2、session管理器
     *  3、管道
     *  4、子容器
     *  5、加载器
     *  6、映射器
     *  7、释放JNDI资源
     *  8、监听器
     *  9、记录器
     *  10、加载器
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws LifecycleException {

        if (!started) throw new LifecycleException("此context容器已处于关闭状态！");
        
        if (debug >= 1)
            log("容器开始关闭！");

        // 关闭context容器之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);

        ClassLoader oldCCL = bindThread();
        
        // 设置此WebApp不可用
        setAvailable(false);
        
        // 设置字符集为null
        setCharsetMapper(null);

        // 关闭后台线程
        threadStop();
        
        // 停止Session管理器
        if (manager != null && manager instanceof Lifecycle)
            ((Lifecycle) manager).stop();

        if (debug >= 1)
            log("容器正式关闭！");
        
        // 关闭事件
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
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
            
            // 关闭mapper
            Mapper[] mappers = findMappers();
            for (Mapper mapper : mappers) {
                if (mapper instanceof Lifecycle)
                    ((Lifecycle) manager).stop();
            }
            
            // 停止监听器
            listenerStop(); // TODO 未实现

            // 关闭JNDI资源 
            if (resources != null) {
                if (resources instanceof Lifecycle)
                    ((Lifecycle) resources).stop();
            } else if (resources instanceof ProxyDirContext) {
                DirContext dirContext = ((ProxyDirContext) resources).getDirContext();
                if (dirContext != null) {
                    if (debug >= 1)
                        log("开启清除资源根路径：" + docBase);
                    
                    if (dirContext instanceof BaseDirContext) {
                        ((BaseDirContext) dirContext).release();
                        if ((dirContext instanceof WARDirContext) 
                           || (dirContext instanceof FileDirContext)) {
                            resources = null;
                        }
                    } else {
                        log("无法释放资源：" + resources);
                    }
                }
            }
            
            // TODO 关闭领域、簇
            
            // 关闭记录器
            if (logger != null && logger instanceof Lifecycle) 
                ((Lifecycle) logger).stop();
            
            // 关闭加载器
            if (loader != null && loader instanceof Lifecycle)
                ((Lifecycle) loader).stop();

        } finally {
            unbindThread(oldCCL);
        }
        
        context = null;
        
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
        if (debug >= 1)
            log("容器已经关闭！");
    }


    /**
     * 停止监听器
     */
    private void listenerStop() {
    }


    /**
     * 停止过滤器
     */
    private void filterStop() {
        if (debug >= 1)
            log("停止过滤器！");
        
        synchronized (filterConfigs) {
            Iterator<String> iterator = filterConfigs.keySet().iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                if (debug >= 1)
                    log("停止过滤器：" + name);
                
                ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) filterConfigs.get(name);
                filterConfig.release();
            }
            
            filterConfigs.clear();
        }
    }


    /**
     * 根据传入的过滤器名称找到对应的过滤器配置项
     * 
     * @param filterName
     * @return
     */
    public FilterConfig findFilterConfig(String filterName) {
        synchronized (filterConfigs) {
            return filterConfigs.get(filterName);
        }
    }

    /**
     * 返回字符集类
     * 
     * @return
     */
    public String getCharsetMapperClass() {
        return charsetMapperClass;
    }


    /**
     * 设置字符集类
     * 
     * @param charsetMapperClass
     */
    public void setCharsetMapperClass(String charsetMapperClass) {
        this.charsetMapperClass = charsetMapperClass;
    }


    /**
     * 设置servlet的类名
     * 
     * @return
     */
    public String getServletClass() {
        return servletClass;
    }


    /**
     * 返回servlet的类名
     * 
     * @param servletClass
     */
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }
}
