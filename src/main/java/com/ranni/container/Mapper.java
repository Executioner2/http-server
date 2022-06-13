package com.ranni.container;

import com.ranni.connector.http.request.Request;

/**
 * Title: HttpServer
 * Description:
 * 映射器接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-28 16:53
 */
@Deprecated
public interface Mapper {
    /**
     * 返回与此映射器关联的容器
     * @return
     */
    Container getContainer();


    /**
     * 设置与此映射器关联的容器
     * @param container
     */
    void setContainer(Container container);


    /**
     * 返回此映射器负责处理的协议
     * @return
     */
    String getProtocol();


    /**
     * 设置此映射器负责处理的协议
     * @param protocol
     */
    void setProtocol(String protocol);


    /**
     * 返回要处理某个特定请求的子容器的实例
     * @param request
     * @param update
     * @return
     */
    Container map(Request request, boolean update);
}
