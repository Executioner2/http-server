package com.ranni.container;

import com.ranni.connector.Constants;
import com.ranni.connector.http.request.HttpRequestBase;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.handler.Loader;

import javax.naming.directory.DirContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 20:36
 */
public class SimpleContainer implements Container {
    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public Loader getLoader() {
        return null;
    }

    @Override
    public void setLoader(Loader loader) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public Container getParent() {
        return null;
    }

    @Override
    public void setParent(Container container) {

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
     * XXX servlet请求执行对应的方法（粗略简单的实现）
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        // 在这里取得要执行的servlet类
        StringBuffer url = ((HttpRequestBase) request).getRequestURL();
        String servletName = url.substring(url.lastIndexOf("/") + 1);
        URL[] urls = new URL[1];
        File path = new File(Constants.WEB_ROOT);
        String repository = (new URL("file", null, path.getAbsolutePath() + File.separator)).toString();

        URLStreamHandler streamHandler = null;
        urls[0] = new URL(null, repository, streamHandler);

        URLClassLoader urlClassLoader = new URLClassLoader(urls);

        Class<?> aClass = null;

        try {
            aClass = urlClassLoader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        try {
            if (aClass != null) {
                Servlet servlet = (Servlet) aClass.getConstructor().newInstance();
                servlet.service(request.getRequest(), response.getResponse());
                response.finishResponse();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
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
