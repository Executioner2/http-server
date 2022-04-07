package com.ranni.container.loader;

import com.ranni.common.Globals;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.exception.LifecycleException;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.util.LifecycleSupport;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Constructor;

/**
 * Title: HttpServer
 * Description:
 * 加载器，负责Webapp的加载，与Context容器关联
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 15:45
 */
public class WebappLoader implements Loader, Runnable, Lifecycle {
    private String loaderClass = "com.ranni.container.loader.WebappClassLoader"; // 加载器的全限定类名，默认为com.ranni.container.loader.WebappClassLoader
    private ClassLoader parentLoader; // 类载入器（父）
    private WebappClassLoader classLoader; // 类载入器
    private boolean started; // 启动标志位
    private Container container; // 与此加载器关联的容器
    private boolean delegate; // 委托标志位
    private boolean reloadable; // 重载标志位
    private String[] repositories = new String[0]; // 仓库
    private LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理实例

    public WebappLoader() {
    }

    public WebappLoader(ClassLoader parentLoader) {
        this.parentLoader = parentLoader;
    }

    /**
     * 返回加载器全限定类名
     *
     * @return
     */
    public String getLoaderClass() {
        return this.loaderClass;
    }

    /**
     * 设置加载器全限定类名
     * 支持用户自定义的WebappClassLoader子类
     * 若传入null则直接返回
     *
     * @param loaderClass
     */
    public void setLoaderClass(String loaderClass) {
        if (loaderClass == null) return;

        this.loaderClass = loaderClass;
    }

    /**
     * 返回此容器的类载入器
     *
     * @return
     */
    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * 返回与此加载器关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此加载器关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * 是否委托给父类加载器
     *
     * @return 返回委托标志
     */
    @Override
    public boolean getDelegate() {
        return this.delegate;
    }

    /**
     * 是否委托给父类加载器
     *
     * @param delegate 委托标志
     */
    @Override
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 返回是否可重新载入标志
     *
     * @return
     */
    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }

    /**
     * 设置重新载入标志
     *
     * @param reloadable
     */
    @Override
    public void setReloadable(boolean reloadable) {
        this.delegate = delegate;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * 添加仓库
     * 此方法的调用是在一个线程中进行的
     * 所以无需使用synchronized
     *
     * @param repository
     */
    @Override
    public void addRepository(String repository) {
        // 查询是否是重复添加的
        for (int i = 0; i < repositories.length; i++) {
            if (repositories[i].equals(repository))
                return;
        }

        String[] newRepositories = new String[repositories.length + 1];
        System.arraycopy(repositories, 0, newRepositories, 0, repositories.length);
        newRepositories[repositories.length] = repository;
        repositories = newRepositories;
    }

    /**
     * 返回所有仓库
     *
     * @return
     */
    @Override
    public String[] findRepositories() {
        return repositories;
    }

    @Override
    public boolean modified() {
        return false;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void run() {

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
     * 1、创建一个类载入器
     * 2、设置仓库
     * 3、设置类路径
     * 4、设置访问权限
     * 5、启动一个新的线程来支持自动重载
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        if (started) throw new LifecycleException("此WebappLoader实例已经启动！");

        log("载入器启动！");
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;

        // 取得要载入的资源（通过资源文件取得servlet类的路径）
        if (container.getResources() == null)
            return;

        // 为JNDI协议注册流处理工厂
//        URLStreamHandlerFactory streamHandlerFactory = new DirContextURLStreamHandlerFactory();
//
//        URL.setURLStreamHandlerFactory(streamHandlerFactory);

        // 创建类载入器
        classLoader = createClassLoader();
//        if (classLoader == null)
//            throw new

        // 设置仓库
        setRepositories();
    }

    /**
     * 设置仓库
     */
    private void setRepositories() {
        if (!(container instanceof Context))
            return;

        // 取得全局作用域，如果没有就直接返回
        ServletContext servletContext = ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        // 取得工作目录
        File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir == null)
            return;

        log("取得webappLoader工作目录：" + workDir.getAbsolutePath());

        // 取得资源文件
        DirContext resources = container.getResources();

        String classPath = "/WEB-INF/classes"; // 类的路径
        DirContext classes = null;

        try {
            Object o = resources.lookup(classPath);// TODO 解析这个路径下所有的类
            if (o instanceof DirContext)
                classes = (DirContext) o;
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止掉此加载器
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        if (started) throw new LifecycleException("此WebappLoader实例已经停止！");
    }

    /**
     * 记录到日志文件
     *
     * @param msg
     */
    private void log(String msg) {
        Logger logger = null;
        String containerName = "";

        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }

        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "]: " + msg);
        } else {
            System.out.println("WebappLoader[" + containerName + "]: " + msg);
        }
    }

    /**
     * 记录到日志文件
     *
     * @param msg
     * @param throwable
     */
    private void log(String msg, Throwable throwable) {
        Logger logger = null;
        String containerName = "";

        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }

        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "]: " + msg, throwable);
        } else {
            System.out.println("WebappLoader[" + containerName + "]: " + msg);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }

    }

    /**
     * 创建类载入器
     * 可以通过私有变量loaderClass创建用户继承了WebappClassLoader的自定义类加载器
     *
     * @return
     *
     * @throws Exception
     */
    private WebappClassLoader createClassLoader() throws Exception {
        Class clazz = Class.forName(getLoaderClass());
        WebappClassLoader classLoader = null;

        if (parentLoader == null) {
            // 父加载器为空，创建当前classLoader的实例
            classLoader = (WebappClassLoader) clazz.getConstructor().newInstance();
        } else {
            // 实例化时设置classLoader的父加载器
            Constructor constructor = clazz.getConstructor(ClassLoader.class);
            classLoader = (WebappClassLoader) constructor.newInstance(parentLoader);
        }

        return classLoader;
    }

}
