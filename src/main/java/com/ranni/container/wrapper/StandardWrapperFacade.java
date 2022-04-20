package com.ranni.container.wrapper;

import com.ranni.container.scope.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-20 20:18
 */
public final class StandardWrapperFacade implements ServletConfig {
    private ServletConfig config;

    public StandardWrapperFacade(ServletConfig config) {
        this.config = config;
    }

    @Override
    public String getServletName() {
        return config.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        ServletContext theContext = config.getServletContext();
        if (theContext != null && theContext instanceof ApplicationContext)
            theContext = ((ApplicationContext) theContext).getFacade();
        return theContext;
    }

    @Override
    public String getInitParameter(String s) {
        return config.getInitParameter(s);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return config.getInitParameterNames();
    }
}
