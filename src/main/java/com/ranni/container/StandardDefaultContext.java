package com.ranni.container;

import com.ranni.container.context.StandardContext;
import com.ranni.loader.Loader;
import com.ranni.deploy.*;
import com.ranni.lifecycle.LifecycleEvent;
import com.ranni.lifecycle.LifecycleListener;

import javax.naming.directory.DirContext;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 这是个十分标准的模板类
 * 标准的默认容器实现类同时也是标准容器的生命周期监听器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:02
 */
public class StandardDefaultContext implements DefaultContext, LifecycleListener {
    private NamingResources namingResources = new NamingResources(); // 关联的命名资源
    private boolean cookies = true; // 是否用cookie作为session id
    private boolean crossContext = true; // 是否允许跨容器
    private String mapperClass = "com.ranni.container.context.StandardContextMapper"; // 标准的容器映射类全限定类名
    private String wrapperClass = "com.ranni.container.wrapper.StandardWrapper"; // 标准的wrapper类
    private Map<String, String> parameters = new HashMap<>(); // 参数
    private boolean reloadable; // 是否可重载
    private boolean swallowOutput;
    private String[] wrapperLifecycles = new String[0]; // wrapper生命周期监听器类名
    private String[] wrapperListeners = new String[0]; // wrapper监听器类名
    private String[] applicationListeners = new String[0]; // 应用程序监听器类名，按照在web.xml文件中出现顺序排列
    private String[] instanceListeners = new String[0]; // 将新创建的Wrapper实例监听器加入到该集合
    private ApplicationParameter[] applicationParameters = new ApplicationParameter[0]; // 应用程序参数集
    private boolean useNaming = true; // 是否使用JNDI
    private Set<Context> contexts = Collections.synchronizedSet(new HashSet<>()); // StandardContext容器的生命周期监听器集合

    protected DirContext dirContext; // 条目资源容器
    protected String name = "defaultContext"; // 此容器名
    protected Container parent; // 父容器
    protected Loader loader; // 加载器
//    protected Manager manager; // 管理器

    public StandardDefaultContext() {
        namingResources.setContainer(this);
    }


    public boolean isSwallowOutput() {
        return swallowOutput;
    }

    public void setSwallowOutput(boolean swallowOutput) {
        this.swallowOutput = swallowOutput;
    }

    /**
     * 是否使用JNDI
     *
     * @return
     */
    public boolean isUseNaming() {
        return useNaming;
    }

    /**
     * 返回是否使用cookie来表示session id
     *
     * @return
     */
    @Override
    public boolean getCookies() {
        return this.cookies;
    }


    /**
     * 设置是否使用cookie来表示session id
     *
     * @param cookies
     */
    @Override
    public void setCookies(boolean cookies) {
        this.cookies = cookies;
    }


    /**
     * 是否允许跨servlet容器
     *
     * @return
     */
    @Override
    public boolean getCrossContext() {
        return this.crossContext;
    }

    /**
     * 设置是否允许跨servlet容器
     *
     * @param crossContext
     */
    @Override
    public void setCrossContext(boolean crossContext) {
        this.crossContext = crossContext;
    }

    /**
     * 实现类信息
     *
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 返回重新加载标志位
     *
     * @return
     */
    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }


    /**
     * 设置可重新加载标志位
     *
     * @param reloadable
     */
    @Override
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }


    /**
     * 取得Wrapper类名
     *
     * @return
     */
    @Override
    public String getWrapperClass() {
        return this.wrapperClass;
    }


    /**
     * 设置取得Wrapper类名
     *
     * @param wrapperClass
     */
    @Override
    public void setWrapperClass(String wrapperClass) {
        this.wrapperClass = wrapperClass;
    }


    /**
     * 设置资源
     *
     * @param resources
     */
    @Override
    public void setResources(DirContext resources) {
        this.dirContext = resources;
    }


    /**
     * 取得资源
     *
     * @return
     */
    @Override
    public DirContext getResources() {
        return this.dirContext;
    }


    /**
     * 返回加载器
     *
     * @return
     */
    @Override
    public Loader getLoader() {
        return this.loader;
    }


    /**
     * 设置加载器
     *
     * @param loader
     */
    @Override
    public void setLoader(Loader loader) {
        this.loader = loader;
    }


    /**
     * 返回命名资源
     *
     * @return
     */
    @Override
    public NamingResources getNamingResources() {
        return this.namingResources;
    }


    /**
     * 返回容器名称
     *
     * @return
     */
    @Override
    public String getName() {
        return this.name;
    }


    /**
     * 设置此容器的名称
     *
     * @param name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }


    /**
     * 取得父容器
     *
     * @return
     */
    @Override
    public Container getParent() {
        return this.parent;
    }


    /**
     * 设置父容器
     *
     * @param container
     */
    @Override
    public void setParent(Container container) {
        this.parent = container;
    }


    /**
     * 设置应用监听器
     *
     * @param listener
     */
    @Override
    public void addApplicationListener(String listener) {
        synchronized (applicationListeners) {
            String[] strings = new String[applicationListeners.length + 1];
            System.arraycopy(applicationListeners, 0, strings, 0, applicationListeners.length);
            strings[applicationListeners.length] = listener;
            applicationListeners = strings;
        }
    }

    /**
     * 添加应用程序参数
     *
     * @param parameter
     */
    @Override
    public void addApplicationParameter(ApplicationParameter parameter) {
        synchronized (applicationParameters) {
            ApplicationParameter[] parameters = new ApplicationParameter[this.applicationParameters.length + 1];
            System.arraycopy(applicationParameters, 0, parameter, 0, applicationParameters.length);
            parameters[applicationParameters.length] = parameter;
            applicationParameters = parameters;
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
     * 添加资源参数
     *
     * @param resourceParameters
     */
    @Override
    public void addResourceParams(ResourceParams resourceParameters) {
        namingResources.addResourceParams(resourceParameters);
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
     * 为资源引用添加环境
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
     * 返回应用程序所有监听器
     *
     * @return
     */
    @Override
    public String[] findApplicationListeners() {
        return this.applicationListeners;
    }


    /**
     * 返回应用程序所有参数
     *
     * @return
     */
    @Override
    public ApplicationParameter[] findApplicationParameters() {
        return this.applicationParameters;
    }


    /**
     * 查询name对应的容器环境
     *
     * @param name
     * @return
     */
    @Override
    public ContextEnvironment findEnvironment(String name) {
        return namingResources.findEnvironment(name);
    }


    /**
     * 返回所有环境
     *
     * @return
     */
    @Override
    public ContextEnvironment[] findEnvironments() {
        return namingResources.findEnvironments();
    }


    /**
     * 返回所有资源参数
     *
     * @return
     */
    @Override
    public ResourceParams[] findResourceParams() {
        return namingResources.findResourceParams();
    }


    /**
     * 返回所有wrapper实例监听器
     *
     * @return
     */
    @Override
    public String[] findInstanceListeners() {
        return this.instanceListeners;
    }


    /**
     * 返回name对应的参数
     *
     * @param name
     * @return
     */
    @Override
    public String findParameter(String name) {
        synchronized (parameters) {
            return parameters.get(name);
        }
    }


    /**
     * 返回所有key
     *
     * @return
     */
    @Override
    public String[] findParameters() {
        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return parameters.keySet().toArray(results);
        }
    }


    /**
     * 返回name对应的资源
     *
     * @param name
     * @return
     */
    @Override
    public ContextResource findResource(String name) {
        return namingResources.findResource(name);
    }


    /**
     * 返回name对应的环境资源类型
     *
     * @param name
     * @return
     */
    @Override
    public String findResourceEnvRef(String name) {
        return namingResources.findResourceEnvRef(name);
    }


    /**
     * 返回所有环境资源类型
     *
     * @return
     */
    @Override
    public String[] findResourceEnvRefs() {
        return namingResources.findResourceEnvRefs();
    }


    /**
     * 返回name对应的资源连接
     *
     * @param name
     * @return
     */
    @Override
    public ContextResourceLink findResourceLink(String name) {
        return namingResources.findResourceLink(name);
    }


    /**
     * 返回所有资源连接
     *
     * @return
     */
    @Override
    public ContextResourceLink[] findResourceLinks() {
        return namingResources.findResourceLinks();
    }


    /**
     * 返回所有资源
     *
     * @return
     */
    @Override
    public ContextResource[] findResources() {
        return namingResources.findResources();
    }


    /**
     * 返回所有wrapper生命周期监听器类名
     *
     * @return
     */
    @Override
    public String[] findWrapperLifecycles() {
        return this.wrapperLifecycles;
    }


    /**
     * 返回所有wrapper监听器
     *
     * @return
     */
    @Override
    public String[] findWrapperListeners() {
        return this.wrapperListeners;
    }


    /**
     * 移除指定的应用程序监听器
     *
     * @param listener
     */
    @Override
    public void removeApplicationListener(String listener) {
        synchronized (applicationListeners) {
            int index = -1;
            for (int i = 0; i < applicationListeners.length; i++) {
                if (applicationListeners[i].equals(listener)) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            String[] strings = new String[applicationListeners.length - 1];
            for (int i = 0; i < strings.length; i++) {
                if (i < index) {
                    strings[i] = applicationListeners[i];
                } else {
                    strings[i] = applicationListeners[i + 1];
                }
            }

            applicationListeners = strings;
        }
    }


    /**
     * 移除指定的应用程序参数
     *
     * @param name
     */
    @Override
    public void removeApplicationParameter(String name) {
        synchronized (applicationParameters) {
            int index = -1;
            for (int i = 0; i < applicationParameters.length; i++) {
                if (applicationParameters[i].getName().equals(name)) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            ApplicationParameter[] apps = new ApplicationParameter[applicationParameters.length - 1];
            for (int i = 0; i < apps.length; i++) {
                if (i < index) {
                    apps[i] = applicationParameters[i];
                } else {
                    apps[i] = applicationParameters[i + 1];
                }
            }

            applicationParameters = apps;
        }
    }


    /**
     * 移除指定环境
     *
     * @param name
     */
    @Override
    public void removeEnvironment(String name) {
        namingResources.removeEnvironment(name);
    }


    /**
     * 移除指定参数
     *
     * @param name
     */
    @Override
    public void removeParameter(String name) {
        synchronized (parameters) {
            parameters.remove(name);
        }
    }


    /**
     * 移除属性改变监听器
     *
     * @param listener
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }


    /**
     * 移除指定资源
     *
     * @param name
     */
    @Override
    public void removeResource(String name) {
        namingResources.removeResource(name);
    }


    /**
     * 移除指定环境类型
     *
     * @param name
     */
    @Override
    public void removeResourceEnvRef(String name) {
        namingResources.removeResourceEnvRef(name);
    }


    /**
     * 移除指定资源连接
     *
     * @param name
     */
    @Override
    public void removeResourceLink(String name) {
        namingResources.removeResourceLink(name);
    }


    /**
     * 移除wrapper指定的生命周期监听器
     *
     * @param listener
     */
    @Override
    public void removeWrapperLifecycle(String listener) {
        synchronized (wrapperLifecycles) {
            int index = -1;
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (wrapperLifecycles[i].equals(listener)) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            String[] strings = new String[wrapperLifecycles.length - 1];
            for (int i = 0; i < strings.length; i++) {
                if (i < index) {
                    strings[i] = wrapperLifecycles[i];
                } else {
                    strings[i] = wrapperLifecycles[i + 1];
                }
            }

            wrapperLifecycles = strings;
        }
    }


    /**
     * 移除指定的wrapper监听器
     *
     * @param listener
     */
    @Override
    public void removeWrapperListener(String listener) {
        synchronized (wrapperListeners) {
            int index = -1;
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (wrapperListeners[i].equals(listener)) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            String[] strings = new String[wrapperListeners.length - 1];
            for (int i = 0; i < strings.length; i++) {
                if (i < index) {
                    strings[i] = wrapperListeners[i];
                } else {
                    strings[i] = wrapperListeners[i + 1];
                }
            }

            wrapperListeners = strings;
        }
    }


    /**
     * 将此StandardDefaultContext中的配置
     * 导入到context中去
     *
     * @param context
     */
    @Override
    public void importDefaultContext(Context context) {
        if (context instanceof StandardContext) {
            ((StandardContext) context).setUseNaming(isUseNaming());
            ((StandardContext) context).setSwallowOutput(isSwallowOutput());
            // 如果传入的StandardContext未将此StandardDefaultContext作为生命周期管理器
            // 加入到自身的生命周期管理器集合中，那就把此StandardDefaultContext作为生命周期管理器加入进去
            if (!contexts.contains(context))
                ((StandardContext) context).addLifecycleListener(this);
        }

        // 导入通用属性
        context.setCookies(getCookies());
        context.setCrossContext(getCrossContext());
        context.setReloadable(getReloadable());
        context.addApplicationListener(findApplicationListeners());
        context.addInstanceListener(findInstanceListeners());
        context.addWrapperLifecycle(findWrapperLifecycles());
        context.addWrapperListener(findInstanceListeners());
        context.addApplicationParameter(findApplicationParameters());
        String[] parameters = findParameters();
        for (int i = 0; i < parameters.length; i++) {
            context.addParameter(parameters[i], findParameter(parameters[i]));
        }

        if (!(context instanceof StandardContext)) {
            // TODO EJB也是在这里导入


            for (ContextEnvironment ce : findEnvironments())
                context.addEnvironment(ce);

            for (ContextResource cr : findResources())
                context.addResource(cr);

            String[] names = findResourceEnvRefs();
            for (int i = 0; i < names.length; i++) {
                context.addResourceEnvRef(names[i], findResourceEnvRef(name));
            }
        }
    }


    /**
     * TODO 处理所有生命周期事件
     *
     * @param event
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {


    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("DefaultContext[");
        sb.append("]");
        return (sb.toString());
    }
}
