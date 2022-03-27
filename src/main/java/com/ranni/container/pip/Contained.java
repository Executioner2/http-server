package com.ranni.container.pip;

import com.ranni.container.Container;

/**
 * Title: HttpServer
 * Description:
 * 阀可以选择是否实现此接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:02
 */
public interface Contained {
    /**
     * 返回与此阀相关联的容器
     * @return
     */
    Container getContainer();


    /**
     * 设置与此阀相关联的容器
     * @param container
     */
    void setContainer(Container container);
}
