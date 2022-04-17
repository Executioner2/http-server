package com.ranni.container.wrapper;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.ContainerBase;
import com.ranni.container.Wrapper;
import com.ranni.container.loader.Loader;
import com.ranni.exception.LifecycleException;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的wrapper接口实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:44
 */
public class StandardWrapper extends ContainerBase implements Wrapper, Lifecycle {
    private Servlet servlet; // servlet

    protected String servletClass; // servlet类全限定类名
    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具实例


    public StandardWrapper() {
        pipeline.setBasic(new DefaultWrapperValve(this));
    }


    @Override
    public long getAvailable() {
        return 0;
    }

    @Override
    public void setAvailable(long available) {

    }

    @Override
    public String getJspFile() {
        return null;
    }

    @Override
    public void setJspFile(String jspFile) {

    }

    @Override
    public int getLoadOnStartup() {
        return 0;
    }

    @Override
    public void setLoadOnStartup(int value) {

    }

    @Override
    public String getRunAs() {
        return null;
    }

    @Override
    public void setRunAs(String runAs) {

    }

    /**
     * 返回servlet类名
     * @return
     */
    @Override
    public String getServletClass() {
        return this.servletClass;
    }

    /**
     * 设置servlet类名
     * @param servletClass
     */
    @Override
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }

    @Override
    public boolean isUnavailable() {
        return false;
    }

    @Override
    public void addInitParameter(String name, String value) {

    }

    /**
     * 返回一个servlet
     *
     * @return
     * @throws ServletException
     */
    @Override
    public Servlet allocate() throws ServletException {
        if (servlet != null) return servlet;

        Loader loader = getLoader();

        if (loader == null) throw new ServletException("没有类加载器！");

        ClassLoader classLoader = loader.getClassLoader();

        if (classLoader == null) throw new ServletException("没有找到类加载器！");

        Class clazz = null;
        try {
            clazz = classLoader.loadClass(servletClass);
        } catch (ClassNotFoundException e) {
            throw new ServletException("servlet类未找到！");
        }

        Servlet servlet = null;
        try {
            servlet = (Servlet) clazz.getConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            servlet.init(null); // TODO 暂时传入个null
        } catch (Throwable t) {
            throw new ServletException("servlet初始化失败！");
        }

        return servlet;
    }

    @Override
    public void deallocate(Servlet servlet) throws ServletException {

    }

    @Override
    public String findInitParameter(String name) {
        return null;
    }

    @Override
    public String[] findInitParameters() {
        return new String[0];
    }

    /**
     * 同allocate()，只是不返回值
     *
     * @throws ServletException
     */
    @Override
    public void load() throws ServletException {
        this.servlet = allocate();
    }

    @Override
    public void unavailable(UnavailableException unavailable) {

    }

    @Override
    public void unload() throws ServletException {

    }

    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 进入管道依次执行阀
     *
     * @param request
     * @param response
     *
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        pipeline.invoke(request, response);
    }

    /**
     * Wrapper本就是最小容器
     * 若Wrapper对象执行该方法将抛出异常
     *
     * @param child
     *
     * @exception IllegalStateException Wrapper对象执行该方法抛出异常
     */
    @Override
    public void addChild(Container child) {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @param name
     *
     * @return
     *
     * @exception  IllegalStateException Wrapper对象执行该方法抛出异常
     */
    @Override
    public Container findChild(String name) {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @return
     */
    @Override
    public Container[] findChildren() {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @param child
     */
    @Override
    public void removeChild(Container child) {
        throw new IllegalStateException ("标准wrapper没有child");
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
     * wrapper容器启动
     * 启动顺序：
     *  1、加载器
     *  2、管道
     *  3、容器自身
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws Exception {
        if (started) throw new LifecycleException("此wrapper容器实例已经启动！");
        System.out.println("启动wrapper容器：" + this); // TODO sout

        // 此wrapper容器启动之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        started = true;

        // 启动加载器
        if (loader != null && loader instanceof Lifecycle)
            ((Lifecycle) loader).start();

        // 启动管道
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();

        // 启动此wrapper容器自身
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);

        // 此wrapper容器启动之后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }


    /**
     * 关闭当前容器
     * 关闭顺序
     *  1、容器本身
     *  2、管道
     *  3、加载器
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws Exception {
        if (!started) throw new LifecycleException("此wrapper容器实例已经停止！");
        System.out.println("停止wrapper容器：" + this); // TODO sout
        try {
            // 执行servlet的destroy()
            servlet.destroy();
        } catch (Throwable t) {
            ;
        }

        servlet = null;

        // 关闭此wrapper容器之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);

        // 关闭此wrapper容器
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;

        // 关闭管道
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // 关闭加载器
        if (loader != null && loader instanceof Lifecycle) {
            ((Lifecycle) loader).stop();
        }

        // 关闭此wrapper容器之后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);

    }
}
