package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * 资源连接视图类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:39
 */
public final class ContextResourceLink {
    private String name; // 此资源连接视图的名字
    private String type; // 类型
    private String global; // 资源的全局名称
    private NamingResources namingResources; // 关联的命名资源

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGlobal() {
        return global;
    }

    public void setGlobal(String global) {
        this.global = global;
    }

    public NamingResources getNamingResources() {
        return namingResources;
    }

    public void setNamingResources(NamingResources namingResources) {
        this.namingResources = namingResources;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ContextResourceLink[");
        sb.append("name=");
        sb.append(name);
        if (type != null) {
            sb.append(", type=");
            sb.append(type);
        }
        if (global != null) {
            sb.append(", global=");
            sb.append(global);
        }
        sb.append("]");
        return (sb.toString());
    }
}
