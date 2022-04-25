package com.ranni.core;

import com.ranni.util.InstanceSupport;

import javax.servlet.*;
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
    private List<FilterConfig> filterConfigs = new ArrayList(); // 过滤器配置
    private Iterator iterator;
    private Servlet servlet;
    private InstanceSupport support; // 实例监听器工具类

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {

    }


    /**
     * 添加过滤配置
     *
     * @param filterConfig
     */
    void addFilterConfig(ApplicationFilterConfig filterConfig) {
        this.filterConfigs.add(filterConfig);

    }
}
