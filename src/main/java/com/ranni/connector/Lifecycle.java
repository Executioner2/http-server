package com.ranni.connector;

/**
 * Title: HttpServer
 * Description:
 * 连接器生命周期接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 22:34
 */
public interface Lifecycle {
    // 连接器启动事件
    String START_EVENT = "start";

    // 连接器启动前
    String BEFORE_START_EVENT = "before_start";

    // 连接器启动后
    String AFTER_START_EVENT = "after_start";

    // 连接器停止
    String STOP_EVENT = "stop";

    // 连接器停止前
    String BEFORE_STOP_EVENT = "before_stop";

    // 连接器停止后
    String AFTER_STOP_EVENT = "after_stop";

    // 添加生命周期事件
//    void addLifecycleListener(LifecycleListener listener);

    // 返回所有生命周期事件
//    LifecycleListener[] findLifecycleListeners();

    // 移除生命周期事件
//    void removeLifecycleListener(LifecycleListener listener);

    // 启动连接器
    void start() throws RuntimeException;


    // 停止连接器
    void stop() throws RuntimeException;
}
