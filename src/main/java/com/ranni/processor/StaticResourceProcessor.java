package com.ranni.processor;

import com.ranni.connector.http.request.HttpRequest;
import com.ranni.connector.http.response.HttpResponse;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 20:52
 */
public class StaticResourceProcessor {
    public StaticResourceProcessor() {
    }

    /**
     * 静态资源处理
     * @param request
     * @param response
     */
    public void process(HttpRequest request, HttpResponse response) {
        try {
            response.sendStaticResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
