package com.ranni.container.pip;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 20:03
 */
public interface ValveContext {

    /**
     * 返回实现类的信息
     * @return
     */
    String getInfo();

    /**
     * 设置基础阀和非基础阀
     * 并且将阀执行计数清0
     * @param valves
     */
    void setValve(Valve basic, Valve[] valves);


    /**
     * 调用中执行下一个阀
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    void invokeNext(Request request, Response response) throws IOException, ServletException;
}
