package com.ranni.container.pip;

import com.ranni.container.Container;

/**
 * Title: HttpServer
 * Description:
 * 就是用来约束基础阀必须实现Contained和Valve这两个接口的抽象类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-17 16:09
 */
public abstract class ValveBase implements Contained, Valve {
    protected Container container = null;


    /**
     * 返回与此阀关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }


    /**
     * 设置与此阀关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }
}
