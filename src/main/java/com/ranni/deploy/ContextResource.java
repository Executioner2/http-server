package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * 资源容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:31
 */
public final class ContextResource {
    private String auth; // 认证
    private String description; // 这个资源的描述信息
    private String name; // 此资源的名称
    private String scope = "Shareable"; // 此资源的共享范围
    private String type; // 此资源的类型
    private NamingResources namingResources; // 与此资源关联的命名资源

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NamingResources getNamingResources() {
        return namingResources;
    }

    public void setNamingResources(NamingResources namingResources) {
        this.namingResources = namingResources;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ContextResource[");
        sb.append("name=");
        sb.append(name);
        if (description != null) {
            sb.append(", description=");
            sb.append(description);
        }
        if (type != null) {
            sb.append(", type=");
            sb.append(type);
        }
        if (auth != null) {
            sb.append(", auth=");
            sb.append(auth);
        }
        if (scope != null) {
            sb.append(", scope=");
            sb.append(scope);
        }
        sb.append("]");
        return (sb.toString());
    }
}
