package com.ranni.container;

import com.ranni.loader.Loader;
import com.ranni.deploy.*;

import javax.naming.directory.DirContext;
import java.beans.PropertyChangeListener;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 16:43
 */
public interface DefaultContext {
    /**
     * 是否使用cookie获取session id
     *
     * @return
     */
    boolean getCookies();


    /**
     * 设置是否使用cookie获取session id
     *
     * @param cookies
     */
    void setCookies(boolean cookies);


    /**
     * 是否允许跨servlet容器
     *
     * @return
     */
    boolean getCrossContext();


    /**
     * 设置是否允许跨servlet容器
     *
     * @param crossContext
     */
    void setCrossContext(boolean crossContext);


    /**
     * 实现类信息
     *
     * @return
     */
    String getInfo();


    /**
     * 返回重新加载标志位
     *
     * @return
     */
    boolean getReloadable();


    /**
     * 设置可重新加载标志位
     *
     * @param reloadable
     */
    void setReloadable(boolean reloadable);


    /**
     * 取得Wrapper类名
     *
     * @return
     */
    String getWrapperClass();


    /**
     * 设置取得Wrapper类名
     *
     * @param wrapperClass
     */
    void setWrapperClass(String wrapperClass);


    /**
     * 设置资源
     *
     * @param resources
     */
    void setResources(DirContext resources);


    /**
     * 取得资源
     *
     * @return
     */
    DirContext getResources();


    /**
     * 取得加载器
     *
     * @return
     */
    Loader getLoader();


    /**
     * 设置加载器
     *
     * @param loader
     */
    void setLoader(Loader loader);


    /**
     * 取得管理器
     *
     * @return
     */
//    Manager getManager();


    /**
     * 设置管理器
     *
     * @param manager
     */
//    void setManager(Manager manager);


    /**
     * 取得此web应用关联的资源
     *
     * @return
     */
    NamingResources getNamingResources();


    /**
     * 取得默认容器名
     *
     * @return
     */
    String getName();


    /**
     * 设置默认容器名
     *
     * @param name
     */
    void setName(String name);


    /**
     * 取得此容器的父容器
     *
     * @return
     */
    Container getParent();


    /**
     * 设置父容器
     *
     * @param container
     */
    void setParent(Container container);


    /**
     * 添加应用程序监听器类名
     *
     * @param listener
     */
    void addApplicationListener(String listener);


    /**
     * 添加应用程序参数
     *
     * @param parameter
     */
    void addApplicationParameter(ApplicationParameter parameter);


    /**
     * 添加EJB资源引用
     *
     * @param ejb
     */
//    public void addEjb(ContextEjb ejb);


    /**
     * 添加容器环境
     *
     * @param environment
     */
    void addEnvironment(ContextEnvironment environment);


    /**
     * 添加资源参数
     *
     * @param resourceParameters
     */
    void addResourceParams(ResourceParams resourceParameters);


    /**
     * 添加实例监听器类名
     *
     * @param listener
     */
    void addInstanceListener(String listener);


    /**
     * 添加参数
     *
     * @param name
     * @param value
     */
    void addParameter(String name, String value);


    /**
     * 添加属性改变监听器
     *
     * @param listener
     */
//    void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 添加此web应用程序的资源引用
     *
     * @param resource
     */
    void addResource(ContextResource resource);


    /**
     * 为资源引用添加环境
     *
     * @param name
     * @param type
     */
    void addResourceEnvRef(String name, String type);


    /**
     * 添加资源连接
     *
     * @param resourceLink
     */
    void addResourceLink(ContextResourceLink resourceLink);


    /**
     * 添加wrapper容器生命周期监听器类名
     *
     * @param listener
     */
    void addWrapperLifecycle(String listener);


    /**
     * 添加wrapper上的容器监听类名
     *
     * @param listener
     */
    void addWrapperListener(String listener);


    /**
     * 返回此应用程序的监听类名
     *
     * @return
     */
    String[] findApplicationListeners();


    /**
     * 返回所有应用程序参数
     *
     * @return
     */
    ApplicationParameter[] findApplicationParameters();


    /**
     * Return the EJB resources reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resources reference
     */
//    ContextEjb findEjb(String name);


    /**
     * Return the defined EJB resources references for this application.
     * If there are none, a zero-length array is returned.
     */
//    ContextEjb[] findEjbs();


    /**
     * 查询环境
     *
     * @param name
     * @return
     */
    ContextEnvironment findEnvironment(String name);


    /**
     * 返回所有环境
     *
     * @return
     */
    ContextEnvironment[] findEnvironments();


    /**
     * 返回所有资源参数
     *
     * @return
     */
    ResourceParams[] findResourceParams();


    /**
     * 返回所有实例监听类名
     *
     * @return
     */
    String[] findInstanceListeners();


    /**
     * 查询name对应的参数
     *
     * @param name
     * @return
     */
    String findParameter(String name);


    /**
     * 返回所有参数
     *
     * @return
     */
    String[] findParameters();


    /**
     * 查询容器资源
     *
     * @param name
     * @return
     */
    ContextResource findResource(String name);


    /**
     * 查询环境资源的类型
     *
     * @param name
     * @return
     */
    String findResourceEnvRef(String name);


    /**
     * 返回所有环境资源的类型
     *
     * @return
     */
    String[] findResourceEnvRefs();


    /**
     * 查询容器资源连接
     *
     * @param name
     * @return
     */
    ContextResourceLink findResourceLink(String name);


    /**
     * 返回所有容器资源连接
     *
     * @return
     */
    ContextResourceLink[] findResourceLinks();


    /**
     * 返回所有容器资源
     *
     * @return
     */
    ContextResource[] findResources();


    /**
     * 返回所有wrapper生命周期监听类名
     *
     * @return
     */
    String[] findWrapperLifecycles();


    /**
     * 返回容器监听类名集合
     *
     * @return
     */
    String[] findWrapperListeners();


    /**
     * 移除应用程序监听器
     *
     * @param listener
     */
    void removeApplicationListener(String listener);


    /**
     * 移除应用程序参数
     *
     * @param name
     */
    void removeApplicationParameter(String name);


    /**
     * Remove any EJB resources reference with the specified name.
     *
     * @param name Name of the EJB resources reference to remove
     */
//    void removeEjb(String name);


    /**
     * 移除环境
     *
     * @param name
     */
    void removeEnvironment(String name);


    /**
     * Remove a class name from the set of InstanceListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
//    void removeInstanceListener(String listener);


    /**
     * 移除参数
     *
     * @param name
     */
    void removeParameter(String name);


    /**
     * 移除属性改变监听器
     *
     * @param listener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * 移除资源
     *
     * @param name
     */
    void removeResource(String name);


    /**
     * 移除资源环境的类型
     *
     * @param name
     */
    void removeResourceEnvRef(String name);


    /**
     * 移除资源连接
     *
     * @param name
     */
    void removeResourceLink(String name);


    /**
     * 移除wrapper生命周期监听
     *
     * @param listener
     */
    void removeWrapperLifecycle(String listener);


    /**
     * 移除wrapper监听
     *
     * @param listener
     */
    void removeWrapperListener(String listener);


    /**
     * 导入默认的容器
     *
     * @param context
     */
    void importDefaultContext(Context context);
}
