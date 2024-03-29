package com.ranni.annotation.core;

import java.lang.annotation.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 16:39
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Param
public @interface RequestBody {

    String value() default ""; // 别名
}
