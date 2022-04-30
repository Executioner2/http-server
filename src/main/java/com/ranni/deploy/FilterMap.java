package com.ranni.deploy;

import com.ranni.util.RequestUtil;

/**
 * Title: HttpServer
 * Description:
 * 过滤器映射类，将请求url，servlet名和过滤器名映射在一起
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-26 21:44
 */
public final class FilterMap {
    private String filterName;
    private String urlPattern;
    private String servletName;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = RequestUtil.URLDecode(urlPattern);
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    public String toString() {
        return "FilterMap{" +
                "filterName='" + filterName + '\'' +
                ", urlPattern='" + urlPattern + '\'' +
                ", servletName='" + servletName + '\'' +
                '}';
    }
}
