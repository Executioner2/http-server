package com.ranni.container;

import com.ranni.connector.http.request.HttpRequestBase;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.pip.Pipeline;
import com.ranni.container.pip.SimplePipeline;
import com.ranni.container.pip.SimpleWrapperValve;
import com.ranni.container.pip.Valve;
import com.ranni.loader.Loader;

import javax.naming.directory.DirContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 简单的wrapper接口实现类，待该类足够完整时再晋升为标准的wrapper实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:44
 */
public class SimpleWrapper implements Wrapper, Pipeline {
    private Loader loader; // 类加载器
    private Servlet servlet; // servlet
    private String servletClass; // servlet类全限定类名

    protected Container parent; // 父容器
    protected Pipeline pipeline = new SimplePipeline(this); // 管道，wrapper有各自的管道


    public SimpleWrapper() {
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

    @Override
    public void load() throws ServletException {

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
     * 获取类加载器
     * @return
     */
    @Override
    public Loader getLoader() {
        if (loader != null) {
            return loader;
        } else if (parent != null) {
            return parent.getLoader();
        }
        return null;
    }

    /**
     * 设置类加载器
     * @param loader
     */
    @Override
    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    /**
     * 返回父容器
     * @return
     */
    @Override
    public Container getParent() {
        return this.parent;
    }

    /**
     * 设置父容器
     * @param container
     */
    @Override
    public void setParent(Container container) {
        this.parent = parent;
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return null;
    }

    @Override
    public void setParentClassLoader(ClassLoader parent) {

    }

    @Override
    public DirContext getResources() {
        return null;
    }

    @Override
    public void setResources(DirContext resources) {

    }

    @Override
    public void addChild(Container child) {

    }

    @Override
    public void addContainerListener(ContainerListener listener) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public Container findChild(String name) {
        return null;
    }

    @Override
    public Container[] findChildren() {
        return new Container[0];
    }

    @Override
    public ContainerListener[] findContainerListeners() {
        return new ContainerListener[0];
    }

    /**
     * 返回基础阀
     * @return
     */
    @Override
    public Valve getBasic() {
        return pipeline.getBasic();
    }

    /**
     * 设置基础阀
     * @param valve
     */
    @Override
    public void setBasic(Valve valve) {
        pipeline.setBasic(valve);
    }

    /**
     * 添加阀
     * @param valve
     */
    @Override
    public void addValve(Valve valve) {
        pipeline.addValve(valve);
    }

    /**
     * 返回所有非基础阀
     * @return
     */
    @Override
    public Valve[] getValves() {
        return pipeline.getValves();
    }

    /**
     * 进入管道依次执行阀
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        StringBuffer url = ((HttpRequestBase) request).getRequestURL();
        String servletName = url.substring(url.lastIndexOf("/") + 1);
        setServletClass(servletName);
        pipeline.invoke(request, response);
    }

    /**
     * 移除指定阀
     * @param valve
     */
    @Override
    public void removeValve(Valve valve) {
        pipeline.removeValve(valve);
    }

    @Override
    public Container map(Request request, boolean update) {
        return null;
    }

    @Override
    public void removeChild(Container child) {

    }

    @Override
    public void removeContainerListener(ContainerListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
}
