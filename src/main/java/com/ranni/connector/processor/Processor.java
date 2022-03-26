package com.ranni.connector.processor;

import com.ranni.connector.HttpConnector;
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
public interface Processor extends Runnable {

    // 处理任务
    void process();

    // 初始化部分属性值，便于重复使用
    void recycle();

    // 设置连接器
    void setHttpConnector(HttpConnector connector);

    // 设置容器
    void setContainer(Container container);

    // 返回工作状态标志位
    boolean isWorking();

    // 设置工作状态标志位
    void setWorking(boolean working);

    // 启动处理器线程
    void start();

    // 传送套接字，并唤醒处理器线程工作
    void assign(Socket socket);

    // 设置停止状态标志位
    void setStopped(boolean stopped);

    // 返回停止状态标志位
    boolean isStooped();
}
