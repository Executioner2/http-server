package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * 应用参数
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:59
 */
public final class ApplicationParameter {
    private String description; // 描述信息
    private String name; // 名字
    private boolean override = true; // 是否允许重载
    private String value; // value

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

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ApplicationParameter[");
        sb.append("name=");
        sb.append(name);
        if (description != null) {
            sb.append(", description=");
            sb.append(description);
        }
        sb.append(", value=");
        sb.append(value);
        sb.append(", override=");
        sb.append(override);
        sb.append("]");
        return (sb.toString());
    }
}
