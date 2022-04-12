package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * 环境容器视图类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:15
 */
public final class ContextEnvironment {
    private String description; // 描述信息
    private boolean override = true; // 是否允许覆盖此环境视图
    private String name; // 此环境视图的名称
    private String type; // 此环境视图的类型
    private String value; // 此环境视图的值
    protected NamingResources resources; // 与此环境视图关联的命名资源

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public NamingResources getNamingResources() {
        return resources;
    }

    public void setNamingResources(NamingResources resources) {
        this.resources = resources;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ContextEnvironment[");
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
        if (value != null) {
            sb.append(", value=");
            sb.append(value);
        }
        sb.append(", override=");
        sb.append(override);
        sb.append("]");
        return (sb.toString());
    }
}
