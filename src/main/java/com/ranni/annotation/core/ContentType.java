package com.ranni.annotation.core;

/**
 * Title: HttpServer
 * Description:
 * 数据类型
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/7/12 20:11
 */
public enum ContentType {

    /**
     * 通用txt类型
     */
    TEXT("text"),

    /**
     * html
     */
    HTML("text/html"),

    /**
     * xml
     */
    XML("text/xml"),

    /**
     * gif
     */
    GIF("image/gif"),

    /**
     * jpeg
     */
    JPEG("image/jpeg"),

    /**
     * png
     */
    PNG("image/png"),
    
    /**
     * json格式
     */
    JSON("application/json"),


    /**
     * 二进制数据
     */
    OCTET_STREAM("application/octet-stream"),

    /**
     * 表单类型
     */
    X_WWW_FROM_URLENCODED("application/x-www-form-urlencoded"),

    /**
     * multipart。例如文件传输会用到
     */
    FORM_DATA("multipart/form-data")

    ;
    

    /**
     * Content-Type的值
     */
    private final String value;
    
    
    ContentType(String value) {
        this.value = value;
    }
    
    
    public String getValue() {
        return value;
    }
}
