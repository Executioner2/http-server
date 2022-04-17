package com.ranni.naming;

/**
 * Title: HttpServer
 * Description:
 * 文件视图类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-09 17:25
 */
public class NamingEntry {
    public static final int ENTRY = 0;
    public static final int LINK_REF = 1;
    public static final int REFERENCE = 2;
    public static final int CONTEXT = 10;

    public String name; // 资源名
    public Object value; // 资源对象
    public int type; // 资源类型

    public NamingEntry(String name, Object value, int type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof NamingEntry)) {
            return name.equals(((NamingEntry) obj).name);
        } else {
            return false;
        }
    }


    public int hashCode() {
        return name.hashCode();
    }

}
