package com.ranni.container.context;

import com.ranni.connector.http.request.HttpRequest;
import com.ranni.connector.http.request.Request;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Mapper;
import com.ranni.container.Wrapper;

import javax.servlet.http.HttpServletRequest;

/**
 * Title: HttpServer
 * Description:
 * 这个是针对默认context而实现的默认映射器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-28 17:13
 */
public class DefaultContextMapper implements Mapper {
    protected Container container; // 此映射器关联的servlet容器（wrapper）
    protected String protocol; // 该映射器负责处理的协议

    /**
     * 返回与此映射器关联的servlet容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置此映射器关联的servlet容器
     *
     * @param container
     *
     * @exception IllegalArgumentException 当传入的参数不是DefaultContext或其子类的实例对象将抛出此异常
     */
    @Override
    public void setContainer(Container container) {
        if (!(container instanceof StandardContext))
            throw new IllegalArgumentException("该容器不是DefaultContext的实例对象！");

        this.container = container;
    }

    /**
     * 返回此映射器负责处理的协议
     *
     * @return
     */
    @Override
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * 设置此映射器负责处理的协议
     *
     * @param protocol
     */
    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * 返回要处理某个特定请求的子容器的实例
     *
     * @param request
     * @param update
     *
     * @return
     */
    @Override
    public Container map(Request request, boolean update) {
        // 获得context的URI
        String contextPath = ((HttpServletRequest) request).getContextPath();

        // 获得request的URI
        String requestURI = ((HttpRequest) request).getDecodedRequestURI();

        // 获得真实的URI，即剪掉context的长度
        String relativeURI = requestURI.substring(contextPath.length());

        // 根据URI取得对应的wrapper
        Wrapper wrapper = null;
        String name = ((Context)container).findServletMapping(relativeURI);

        if (name != null) wrapper = (Wrapper) container.findChild(name);

        return wrapper;
    }
}