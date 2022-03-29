package com.ranni.container.pip;

import com.ranni.connector.http.request.HttpRequest;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Wrapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * 适用于context的基础阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-29 21:36
 */
public class SimpleContextValve implements Valve, Contained {
    protected Container container;

    public SimpleContextValve(Container container) {
        setContainer(container);
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 在这个方法中寻找最终请求的wrapper包装的servlet对象
     * 并执行wrapper的invoke方法
     *
     * @param request
     * @param response
     * @param valveContext
     *
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) return;

        HttpServletResponse hsrp = (HttpServletResponse)response;
        String requestURI = ((HttpRequest)request).getDecodedRequestURI();
        Context context = (Context) this.container;

        Wrapper wrapper = null;

        try {
            wrapper = (Wrapper) context.map(request, true);
        } catch (IllegalArgumentException e) {
            hsrp.sendError(HttpServletResponse.SC_BAD_REQUEST, requestURI);
        }

        if (wrapper == null) {
            hsrp.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
            return;
        }

        response.setContext(context);
        wrapper.invoke(request, response);
    }

    /**
     * 返回与此阀关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }


    /**
     * 设置与此阀关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }
}