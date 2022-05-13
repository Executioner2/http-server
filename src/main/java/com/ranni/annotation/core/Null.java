package com.ranni.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Title: HttpServer
 * Description:
 * 为空的标记
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/12 14:58
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
public @interface Null {
}
