package com.ranni.annotation.core;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/7/14 15:29
 */
public enum Charset {
    NULL(null),
    
    UTF8("utf-8"),
    ;
    
    private final String value;
    
    
    Charset(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
