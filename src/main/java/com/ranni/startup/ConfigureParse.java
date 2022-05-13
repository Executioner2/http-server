package com.ranni.startup;

import com.ranni.deploy.ServerMap;

/**
 * Title: HttpServer
 * Description:
 * 配置文件解析
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 20:41
 */
public interface ConfigureParse {

    /**
     * 解析
     * 
     * @return
     */
    ServerMap parse() throws Exception;
}
