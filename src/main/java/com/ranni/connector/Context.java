package com.ranni.connector;

import com.ranni.container.Container;
import com.ranni.util.CharsetMapper;

import javax.servlet.ServletContext;

/**
 * Title: HttpServer
 * Description:
 * ServletContext的处理类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:50
 */
public interface Context extends Container {
    // 获取servlet context
    ServletContext getServletContext();

    // 获取字符编码
    CharsetMapper getCharsetMapper();
}
