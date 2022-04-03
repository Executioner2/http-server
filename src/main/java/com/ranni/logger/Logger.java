package com.ranni.logger;

import com.ranni.container.Container;

import java.beans.PropertyChangeListener;

/**
 * Title: HttpServer
 * Description:
 * 日志记录器接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 19:53
 */
public interface Logger {
    int FATAL = Integer.MIN_VALUE;
    int ERROR = 1;
    int WARNING = 2;
    int INFORMATION = 3;
    int DEBUG = 4;


    /**
     * 返回与此日志记录器关联的容器
     *
     * @return
     */
    Container getContainer();


    /**
     * 设置与此日志记录器关联的容器
     *
     * @param container
     */
    void setContainer(Container container);


    /**
     * 返回此实现类的信息
     *
     * @return
     */
    String getInfo();


    /**
     * 返回日志级别
     *
     * @return
     */
    int getVerbosity();


    /**
     * 设置日志级别
     *
     * @param verbosity
     */
    void setVerbosity(int verbosity);


    /**
     * 添加属性改变监听器
     *
     * @param listener
     */
    void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 移除指定的属性监听器
     *
     * @param listener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * 将信息写入日志
     *
     * @param msg
     */
    void log(String msg);


    /**
     * 将信息和异常写入日志
     *
     * @param exception
     * @param msg
     */
    void log(Exception exception, String msg);


    /**
     * 将信息和异常写入日志
     *
     * @param msg
     * @param throwable
     */
    void log(String msg, Throwable throwable);


    /**
     * 将信息、异常和级别写入日志
     *
     * @param msg
     * @param throwable
     * @param verbosity
     */
    void log(String msg, Throwable throwable, int verbosity);


    /**
     * 将信息和级别写入日志
     *
     * @param msg
     * @param verbosity
     */
    void log(String msg, int verbosity);
}
