package com.ranni.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 过滤器通用定义
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-26 17:30
 */
public final class FilterDef {
    private String description; // 描述信息
    private String displayName; // 显示的名字
    private String filterClass; // 过滤器类名
    private String filterName;  // 过滤器名
    private String largeIcon;
    private String smallIcon;
    private Map<String, String> parameters = new HashMap();

    public String getDescription() {
        return (this.description);
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDisplayName() {
        return (this.displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getFilterClass() {
        return (this.filterClass);
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    public String getFilterName() {
        return (this.filterName);
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }
    
    public String getLargeIcon() {
        return (this.largeIcon);
    }

    public void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }
    
    public Map getParameterMap() { return (this.parameters); }

    public String getSmallIcon() {
        return (this.smallIcon);
    }

    public void setSmallIcon(String smallIcon) {
        this.smallIcon = smallIcon;
    }


    /**
     * 添加过滤器的定义参数
     * 
     * @param name
     * @param value
     */
    public void addInitParameter(String name, String value) {
        parameters.put(name, value);
    }

    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("FilterDef[");
        sb.append("filterName=");
        sb.append(this.filterName);
        sb.append(", filterClass=");
        sb.append(this.filterClass);
        sb.append("]");
        return (sb.toString());
    }
}
