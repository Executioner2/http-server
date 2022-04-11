package com.ranni.deploy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 资源参数类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:24
 */
public final class ResourceParams {
    private NamingResources namingResources; // 与此资源参数视图关联的命名资源
    private String name; // 此资源参数视图的名称
    private Map<String, String> resourceParams = Collections.synchronizedMap(new HashMap<>()); // 资源参数

    public NamingResources getNamingResources() {
        return namingResources;
    }

    public void setNamingResources(NamingResources namingResources) {
        this.namingResources = namingResources;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getParameters() {
        return resourceParams;
    }

    public void addParameters(String name, String value) {
        resourceParams.put(name, value);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ResourceParams[");
        sb.append("name=");
        sb.append(name);
        sb.append(", parameters=");
        sb.append(resourceParams.toString());
        sb.append("]");
        return (sb.toString());
    }
}
