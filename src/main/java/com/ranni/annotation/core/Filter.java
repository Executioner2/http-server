package com.ranni.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Title: HttpServer
 * Description:
 * 过滤器
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 14:13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Filter {
    String value(); // 需要被过滤的请求路径
}
