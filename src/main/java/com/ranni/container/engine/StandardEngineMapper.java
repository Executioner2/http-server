package com.ranni.container.engine;

import com.ranni.connector.http.request.Request;
import com.ranni.container.Container;
import com.ranni.container.Mapper;
import com.ranni.container.host.StandardHost;

/**
 * Title: HttpServer
 * Description:
 * 标准的Engine映射器
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 15:06
 */
public class StandardEngineMapper implements Mapper {
    protected Container container; // 此映射器关联的Context容器
    protected String protocol; // 该映射器负责处理的协议


    /**
     * 返回关联的容器
     * 
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }


    /**
     * 设置关联的容器
     * 
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        if (!(container instanceof StandardHost))
            throw new IllegalArgumentException("不是标准Engine容器！");
        
        this.container = container;
    }


    /**
     * 返回关联的协议
     * 
     * @return
     */
    @Override
    public String getProtocol() {
        return this.protocol;
    }


    /**
     * 设置关联的协议
     * 
     * @param protocol
     */
    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * TODO 返回请求中指向的Context容器
     * 
     * @param request
     * @param update
     * @return
     */
    @Override
    public Container map(Request request, boolean update) {
        return null;
    }
}
