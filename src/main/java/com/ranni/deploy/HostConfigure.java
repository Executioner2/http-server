package com.ranni.deploy;

import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * Host配置
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 21:05
 */
public final class HostConfigure {
    private String name; // host的名字
    private String appBase; // host目录
    private String clazz; // host的类名
    private String defaultContextClass; // 默认的Context容器全限定类名
    private List<ContextConfigure> contexts; // Context容器

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppBase() {
        return appBase;
    }

    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getDefaultContextClass() {
        return defaultContextClass;
    }

    public void setDefaultContextClass(String defaultContextClass) {
        this.defaultContextClass = defaultContextClass;
    }

    public List<ContextConfigure> getContexts() {
        return contexts;
    }

    public void setContexts(List<ContextConfigure> contexts) {
        this.contexts = contexts;
    }
}
