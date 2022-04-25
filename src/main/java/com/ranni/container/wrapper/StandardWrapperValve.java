package com.ranni.container.wrapper;

import com.ranni.common.Globals;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;
import com.ranni.core.ApplicationFilterChain;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的的wrapper基础阀
 * 此基础阀要完成的任务：
 * 1、执行与该servlet实例关联的全部过滤器
 * 2、调用servlet实例的service()方法
 *
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:47
 */
public class StandardWrapperValve extends ValveBase {
    public StandardWrapperValve(Container container) {
        setContainer(container);
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 调用对应的servlet执行相应的service()
     * 必要的操作：
     * 1、调用wrapper的allocate()获取servlet实例 {@link StandardWrapper#allocate()}
     * 2、调用私有方法createFilterChain()，创建过滤链
     * 3、调用过滤链的doFilter()方法，doFilter()中有对servlet实例的service()方法调用
     * 4、释放过滤器的链
     * 5、调用wrapper的deallocate()方法归还servlet资源 {@link StandardWrapper#deallocate(Servlet)}
     * 6、若该servlet再也不会被使用到就会调用wrapper的unload()销毁此servlet实例 {@link StandardWrapper#unload()}
     *
     * @param request
     * @param response
     * @param valveContext
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
        boolean unavailable = false; // 是否不可用

        StandardWrapper wrapper = (StandardWrapper) getContainer();
        ServletRequest sreq = request.getRequest();
        ServletResponse sres = response.getResponse();
        Servlet servlet = null;

        HttpServletRequest hreq = null;
        HttpServletResponse hres = null;
        if (sreq instanceof HttpServletRequest)
            hreq = (HttpServletRequest) sreq;
        if (sres instanceof HttpServletResponse)
            hres = (HttpServletResponse) sres;

        // 检查此Web应用程序是否可用
        if (!((Context) wrapper.getParent()).getAvailable()) {
            hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "StandardWrapper.getAvailable  此WebApp不可用，" + wrapper.getName());
            unavailable = true;
        }

        // 检查servlet是否未到可用时间或永久不可用
        if (!unavailable && wrapper.isUnavailable()) {
            if (hres != null) {
                long available = wrapper.getAvailable();
                if (available > 0L && available < Long.MAX_VALUE) {
                    hres.setDateHeader("Retry-After", available);
                }
                hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "StandardWrapper.isUnavailable  此Servlet未到可用时间或永久不可用，" + wrapper.getName());
            }

            unavailable = true;
        }

        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (Throwable e) {
            servlet = null;
            exception(request, response, e); // 设置异常
        }

        // 创建请求链
        ApplicationFilterChain filterChain = createFilterChain(request, servlet);

//        Wrapper wrapper = (Wrapper) getContainer();
//        ServletRequest servletRequest = request.getRequest();
//        ServletResponse servletResponse = response.getResponse();
//        Servlet servlet = wrapper.allocate();
//        servlet.service(servletRequest, servletResponse);
//        response.finishResponse();
    }


    /**
     * TODO 创建请求链
     *
     * @param request
     * @param servlet
     * @return
     */
    private ApplicationFilterChain createFilterChain(Request request, Servlet servlet) {
        return null;
    }


    /**
     * 设置异常状态
     *
     * @param request
     * @param response
     * @param exception
     */
    private void exception(Request request, Response response, Throwable exception) {
        ServletRequest servletRequest = request.getRequest();
        servletRequest.setAttribute(Globals.EXCEPTION_ATTR, exception);

        ServletResponse servletResponse = response.getResponse();
        if (servletResponse instanceof HttpServletResponse)
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
