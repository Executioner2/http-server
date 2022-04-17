package com.ranni.container.wrapper;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.Wrapper;
import com.ranni.container.pip.Contained;
import com.ranni.container.pip.Valve;
import com.ranni.container.pip.ValveContext;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 默认的wrapper阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:47
 */
public class DefaultWrapperValve implements Valve, Contained {
    private Container container;

    public DefaultWrapperValve(Container container) {
        setContainer(container);
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 调用对应的servlet执行相应的service()
     *
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
        System.out.println("在处理servlet方法前进入了SimpleWrapperValve的invoke方法"); // TODO sout
        servlet.service(servletRequest, servletResponse);
        response.finishResponse();
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
