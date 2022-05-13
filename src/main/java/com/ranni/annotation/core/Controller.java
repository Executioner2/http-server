package com.ranni.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Title: HttpServer
 * Description:
 * 请求的controller实例
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 15:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {
    String value(); // 映射的path
    
    String alias() default ""; // 别名
}
