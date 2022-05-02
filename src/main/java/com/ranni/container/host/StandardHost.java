package com.ranni.container.host;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.*;
import com.ranni.container.loader.Loader;
import com.ranni.logger.Logger;

import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的Host容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 15:01
 */
public class StandardHost extends ContainerBase implements Host {
    private String workDir; // 工作目录
    private DefaultContext defaultContext; // 默认容器配置

    protected int debug; // 日志输出等级
    protected String appBase; // 根路径
    protected boolean autoDeploy; // 自动部署


    /**
     * 取得工作路径
     *
     * @return
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * 设置工作路径
     *
     * @param workDir
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    /**
     * 取得根路径
     *
     * @return
     */
    @Override
    public String getAppBase() {
        return this.appBase;
    }

    /**
     * 设置根路径
     *
     * @param appBase
     */
    @Override
    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    /**
     * 返回自动部署标志
     *
     * @return
     */
    @Override
    public boolean getAutoDeploy() {
        return this.autoDeploy;
    }

    /**
     * 设置自动部署标志
     *
     * @param autoDeploy
     */
    @Override
    public void setAutoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
    }


    /**
     * 设置容器名字
     * 转小写
     *
     * @param name
     */
    @Override
    public void setName(String name) {
        if (name == null)
            throw new IllegalArgumentException("容器名字不能为空！");
        this.name = name.toLowerCase();
    }

    @Override
    public void importDefaultContext(Context context) {

    }

    @Override
    public void addAlias(String alias) {

    }

    @Override
    public String[] findAliases() {
        return new String[0];
    }

    /**
     * 请求取得uri中对应的context容器
     *
     * @param uri
     * @return
     */
    @Override
    public Context map(String uri) {
        if (debug > 0)
            log("请求URI  " + uri);
        if (uri == null)
            return null;

        if (debug > 1)
            log("依次尝试从最长的路径前缀取得Context容器");

        Context context = null;
        String prefixUri = uri;
        int pos;
        while ((pos = prefixUri.lastIndexOf('/')) != -1) {
            context = (Context) findChild(prefixUri);
            if (context != null) break;
            prefixUri.substring(0, pos);
        }

        if (context == null) {
            if (debug > 1)
                log("尝试从默认的context容器中取得");
            context = (Context) findChild("");
        }

        if (context == null) {
            log("未能取得context容器  " + uri);
        } else if (debug > 0) {
            log("成功取得context容器  " + context.getPath());
        }

        return context;
    }

    @Override
    public void removeAlias(String alias) {

    }

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
    public void backgroundProcessor() {
        
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
    public void addChild(Container child) {

    }

    @Override
    public void addContainerListener(ContainerListener listener) {

    }

    @Override
    public void addMapper(Mapper mapper) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * 根据name取得子容器
     *
     * @param name
     * @return
     */
    @Override
    public Container findChild(String name) {
        if (name == null) return null;

        synchronized (children) {
            return children.get(name);
        }
    }

    /**
     * 返回所有子容器
     *
     * @return
     */
    @Override
    public Container[] findChildren() {
        synchronized (children) {
            return children.values().toArray(new Container[children.values().size()]);
        }
    }

    @Override
    public ContainerListener[] findContainerListeners() {
        return new ContainerListener[0];
    }

    /**
     * 根据协议取得映射器
     *
     * @param protocol
     * @return
     */
    @Override
    public Mapper findMapper(String protocol) {
        if (mapper != null) {
            return mapper;
        } else {
            synchronized (mappers) {
                return mappers.get(protocol);
            }
        }
    }

    /**
     * 返回所有映射器
     *
     * @return
     */
    @Override
    public Mapper[] findMappers() {
        synchronized (mappers) {
            return mappers.values().toArray(new Mapper[mappers.values().size()]);
        }
    }

    /**
     * 调用管道
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        pipeline.invoke(request, response);
    }

    @Override
    public void removeChild(Container child) {

    }

    @Override
    public void removeContainerListener(ContainerListener listener) {

    }

    @Override
    public void removeMapper(Mapper mapper) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * 日志输出
     *
     * @param msg
     */
    protected void log(String msg) {
        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + msg);
        else
            System.out.println(logName() + ": " + msg);
    }

    /**
     * 日志输出
     *
     * @param msg
     */
    protected void log(String msg, Throwable t) {
        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + msg + ": " + t);
        else {
            System.out.println(logName() + ": " + msg + ": " + t);
            t.printStackTrace(System.out);
        }

    }
}
