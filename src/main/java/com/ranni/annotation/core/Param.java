package com.ranni.annotation.core;

import java.lang.annotation.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2023/1/30 14:43
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Param {
    String value() default ""; // 别名
}
