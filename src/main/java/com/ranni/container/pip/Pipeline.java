package com.ranni.container.pip;

import com.ranni.connector.Request;
import com.ranni.connector.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 管道的定义接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 19:57
 */
public interface Pipeline {
    /**
     * 返回基础阀，基本阀为最后一个调用的阀
     * @return
     */
    Valve getBasic();


    /**
     * 设置基础阀
     * @param valve
     */
    void setBasic(Valve valve);


    /**
     * 添加非基础阀
     * @param valve
     */
    void addValve(Valve valve);


    /**
     * 返回所有非基础阀
     * @return
     */
    Valve[] getValves();


    /**
     * 调用管道中的阀和基础阀
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    void invoke(Request request, Response response) throws IOException, ServletException;


    /**
     * 移除指定阀
     * @param valve
     */
    void removeValve(Valve valve);
}
