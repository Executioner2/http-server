package com.ranni.container.loader;

import com.ranni.container.Container;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * Title: HttpServer
 * Description:
 * 简单的类加载器实现类，后续完善为标准类加载器实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:34
 */
public class SimpleLoader implements Loader, Lifecycle {
    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot"; // WEB根目录
    private ClassLoader classLoader; // 类加载器
    private Container container; // 与此加载器关联的容器
    private boolean stared; // 容器是否启动

    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具实例

    /**
     * 构造对象的时候就创建类加载器
     */
    public SimpleLoader() {
        try {
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(WEB_ROOT);
            String repository = (new URL("file", null, classPath.getAbsolutePath() + File.separator)).toString();
            urls[0] = new URL(null, repository, streamHandler);
            classLoader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回类加载器
     *
     * @return
     */
    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * 返回与该类加载器关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与该类加载器关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public boolean getDelegate() {
        return false;
    }

    @Override
    public void setDelegate(boolean delegate) {

    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public boolean getReloadable() {
        return false;
    }

    @Override
    public void setReloadable(boolean reloadable) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void addRepository(String repository) {

    }

    @Override
    public String[] findRepositories() {
        return new String[0];
    }

    @Override
    public boolean modified() {
        return false;
    }


    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * 添加生命周期监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * 返回所有生命周期监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()} 该方法是线程安全的方法
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * 移除生命周期监听器
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
     * 加载器启动事件
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws Exception {
        System.out.println("启动SimpleLoader"); // TODO sout
    }

    /**
     * 加载器停止事件
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws Exception {
        System.out.println("停止SimpleLoader"); // TODO sout
    }
}
