package com.ranni.connector.processor;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;

import java.net.Socket;

/**
 * Title: HttpServer
 * Description:
 * 处理器接口
 * TODO 目前就一个处理任务的功能，有需求了再添加方法
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 22:44
 */
public interface Processor {

    // 处理任务
    void process(Socket socket);
}
