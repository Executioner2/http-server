package com.ranni.annotation.core;

import java.lang.annotation.*;

/**
 * Title: HttpServer
 * Description:
 * 请求的具体方法
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 14:08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    
    String value(); // 映射的path
    String method() default ""; // 请求的方法
    ContentType contentType() default ContentType.JSON; // 返回的类型
    
}
