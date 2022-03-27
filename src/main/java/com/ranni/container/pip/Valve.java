package com.ranni.container.pip;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 阀的定义接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 19:57
 */
public interface Valve {
    /**
     * 返回实现类的信息
     * @return
     */
    String getInfo();


    /**
     * 调用执行阀的方法
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException;
}
