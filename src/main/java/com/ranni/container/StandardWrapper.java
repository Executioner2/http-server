package com.ranni.container;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.pip.SimpleWrapperValve;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.container.loader.Loader;

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
    protected Servlet servlet; // servlet
    protected String servletClass; // servlet类全限定类名


    public StandardWrapper() {
        pipeline.setBasic(new SimpleWrapperValve(this));
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

    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
