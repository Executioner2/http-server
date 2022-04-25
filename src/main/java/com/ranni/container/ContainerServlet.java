package com.ranni.container;

/**
 * Title: HttpServer
 * Description:
 * 实现此接口就能访问到Ranni内部的容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 19:26
 */
public interface ContainerServlet {
    /**
     * 返回wrapper容器
     *
     * @return
     */
    Wrapper getWrapper();


    /**
     * 设置wrapper容器
     *
     * @param wrapper
     */
    void setWrapper(Wrapper wrapper);
}
