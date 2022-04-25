package com.ranni.monitor;

import com.ranni.container.Wrapper;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.EventObject;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 22:50
 */
public final class InstanceEvent extends EventObject {
    // init()方法执行前
    public static final String BEFORE_INIT_EVENT = "beforeInit";

    // init()方法执行后
    public static final String AFTER_INIT_EVENT = "afterInit";

    // service()方法执行前
    public static final String BEFORE_SERVICE_EVENT = "beforeService";

    // service()方法执行后
    public static final String AFTER_SERVICE_EVENT = "afterService";

    // destroy()方法执行前
    public static final String BEFORE_DESTROY_EVENT = "beforeDestroy";

    // destroy()方法执行后
    public static final String AFTER_DESTROY_EVENT = "afterDestroy";

    // 转发前
    public static final String BEFORE_DISPATCH_EVENT = "beforeDispatch";

    // 转发后
    public static final String AFTER_DISPATCH_EVENT = "afterDispatch";

    // 过滤器执行前
    public static final String BEFORE_FILTER_EVENT = "beforeFilter";

    // 过滤器执行后
    public static final String AFTER_FILTER_EVENT = "afterFilter";


    private Throwable exception;
    private Filter filter;
    private ServletRequest request;
    private ServletResponse response;
    private Servlet servlet;
    private String type;
    private Wrapper wrapper;


    public InstanceEvent(Wrapper wrapper, Filter filter, String type) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
    }

    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         Throwable exception) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.exception = exception;
    }

    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         ServletRequest request, ServletResponse response) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.request = request;
        this.response = response;
    }

    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         ServletRequest request, ServletResponse response,
                         Throwable exception) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }

    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
    }


    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         Throwable exception) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.exception = exception;
    }

    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         ServletRequest request, ServletResponse response) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.request = request;
        this.response = response;
    }

    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         ServletRequest request, ServletResponse response,
                         Throwable exception) {

        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public ServletRequest getRequest() {
        return request;
    }

    public void setRequest(ServletRequest request) {
        this.request = request;
    }

    public ServletResponse getResponse() {
        return response;
    }

    public void setResponse(ServletResponse response) {
        this.response = response;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Wrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }
}
