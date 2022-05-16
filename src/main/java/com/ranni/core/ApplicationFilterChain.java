package com.ranni.core;

import com.ranni.container.monitor.InstanceEvent;
import com.ranni.util.InstanceSupport;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 22:44
 */
public final class ApplicationFilterChain implements FilterChain {
    private List<FilterConfig> filters = new ArrayList(); // 过滤器
    private Iterator iterator;
    private Servlet servlet;
    private InstanceSupport support; // 实例监听器工具类
    
    
    
    /**
     * 过滤方法
     * 
     * @param servletRequest
     * @param servletResponse
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        // XXX 应该在这里做安全检查
        internalDoFilter(servletRequest, servletResponse);
    }


    /**
     * 往下执行过滤器
     * 
     * @param request
     * @param response
     */
    private void internalDoFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (this.iterator == null)
            this.iterator = filters.iterator();
        
        if (this.iterator.hasNext()) {
             ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) iterator.next();
             Filter filter = null;
             
             try {
                 filter = filterConfig.getFilter();
                 support.fireInstanceEvent(InstanceEvent.BEFORE_FILTER_EVENT, filter, request, response);
                 filter.doFilter(request, response, this);
                 support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response);
             } catch (IOException e) {
                 if (filter != null) {
                     support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
                 }
                 throw e;
             } catch (ServletException e) {
                 if (filter != null) {
                     support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
                 }
                 throw e;
             } catch (RuntimeException e) {
                 if (filter != null) {
                     support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
                 }
                 throw e;
             } catch (Throwable e) {
                 if (filter != null) {
                     support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
                 }
                 throw new ServletException(e);
             }
        }
        
        // 开始执行servlet的service方法
        try {
            support.fireInstanceEvent(InstanceEvent.BEFORE_SERVICE_EVENT, servlet, request, response);            
            if (request instanceof HttpServletRequest
                && response instanceof HttpServletResponse) {
                servlet.service((HttpServletRequest) request, (HttpServletResponse) response);
            } else {
                servlet.service(request, response);
            }
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response);
        } catch (IOException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
            throw e;
        } catch (ServletException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
            throw e;
        } catch (RuntimeException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
            throw e;
        } catch (Throwable e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
            throw new ServletException(e);
        }
    }


    /**
     * 添加过滤配置
     *
     * @param filterConfig
     */
    public void addFilter(ApplicationFilterConfig filterConfig) {
        this.filters.add(filterConfig);
    }


    /**
     * 释放资源
     */
    public void release() {
        this.filters.clear();
        this.iterator = null;
        this.servlet = null;
    }
    

    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
    }
    

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }
    
    
    public void setSupport(InstanceSupport support) {
        this.support = support;
    }
}
