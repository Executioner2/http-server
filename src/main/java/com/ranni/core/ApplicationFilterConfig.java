package com.ranni.core;

import com.ranni.container.Context;
import com.ranni.util.Enumerator;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 过滤器关联类，将容器、过滤器定义实例和过滤器实例三者相关联起来
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 23:03
 */
public final class ApplicationFilterConfig implements FilterConfig {
    private Context context; // 与此App过滤器配置绑定的Context容器
    private Filter filter;
    private FilterDef filterDef; // 过滤器通用定义

    public ApplicationFilterConfig(Context context, FilterDef filterDef)
            throws ClassCastException, ClassNotFoundException,
            IllegalAccessException, InstantiationException,
            ServletException, NoSuchMethodException, InvocationTargetException {

        this.context = context;
        setFilterDef(filterDef);

    }


    /**
     * 设置过滤器定义实例
     * 
     * @param filterDef
     */
    private void setFilterDef(FilterDef filterDef) throws NoSuchMethodException, ServletException, IllegalAccessException, InstantiationException, InvocationTargetException, ClassNotFoundException {
        this.filterDef = filterDef;
        
        if (this.filterDef == null) {
            if (filter != null) {
                filter.destroy();
            }
            filter = null;
        } else {
            filter = getFilter();
        }
    }


    /**
     * 返回一个Filter实例
     * 
     * @return
     */
    Filter getFilter() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ServletException {
        if (filter != null)
            return filter;

        String filterClass = filterDef.getFilterClass();
        ClassLoader classLoader = null;
        if (filterClass.startsWith("org.apache.catalina."))
            classLoader = this.getClass().getClassLoader();
        else
            classLoader = context.getLoader().getClassLoader();

        Class<?> aClass = classLoader.loadClass(filterClass);
        Constructor<?> constructor = aClass.getConstructor();
        if (constructor == null)
            throw new IllegalStateException("ApplicationFilterConfig.getFilter->getConstructor  构造器为null");

        filter = (Filter) constructor.newInstance();
        filter.init(this);
        
        return filter;
    }


    /**
     * 返回过滤器的名字
     * 
     * @return
     */
    @Override
    public String getFilterName() {
        return this.filterDef.getFilterName();
    }


    /**
     * 返回全局作用域
     * 
     * @return
     */
    @Override
    public ServletContext getServletContext() {
        return this.context.getServletContext();
    }


    /**
     * 取得指定的初始化参数
     * 
     * @param s
     * @return
     */
    @Override
    public String getInitParameter(String s) {
        Map<String, String> parameterMap = filterDef.getParameterMap();
        if (parameterMap == null) {
            return null;
        } else {
            return parameterMap.get(s);
        }
    }


    /**
     * 返回所有初始化参数名的迭代器
     * 
     * @return
     */
    @Override
    public Enumeration getInitParameterNames() {
        Map<String, String> parameterMap = filterDef.getParameterMap();
        if (parameterMap == null)
            return new Enumerator(new ArrayList());
        
        return new Enumerator(parameterMap.keySet());
    }


    /**
     * 释放资源
     */
    public void release() {
        if (this.filter != null)
            filter.destroy();
        this.filter = null;
    }
}
