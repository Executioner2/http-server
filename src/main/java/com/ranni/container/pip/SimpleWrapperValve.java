package com.ranni.container.pip;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.Wrapper;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 简单的wrapper阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:47
 */
public class SimpleWrapperValve implements Valve, Contained {
    private Container container;

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 调用对应的servlet执行相应的service()
     * @param request
     * @param response
     * @param valveContext
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
        Wrapper wrapper = (Wrapper) getContainer();
        ServletRequest servletRequest = request.getRequest();
        ServletResponse servletResponse = response.getResponse();
        Servlet servlet = wrapper.allocate();
        servlet.service(servletRequest, servletResponse);
    }

    /**
     * 返回与此容器关联的阀
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此阀关联的容器
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }
}