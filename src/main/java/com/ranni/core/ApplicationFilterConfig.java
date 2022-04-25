package com.ranni.core;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 * 过滤器配置
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 23:03
 */
public final class ApplicationFilterConfig implements FilterConfig {
    @Override
    public String getFilterName() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Enumeration getInitParameterNames() {
        return null;
    }
}
