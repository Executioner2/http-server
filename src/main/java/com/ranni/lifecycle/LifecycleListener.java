package com.ranni.lifecycle;

/**
 * Title: HttpServer
 * Description:
 * 生命周期监听器接口（观察者接口）
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-02 19:23
 */
public interface LifecycleListener {
    /**
     * 该监听器实例监听事件被触发后，执行此方法
     * @param event
     */
    void lifecycleEvent(LifecycleEvent event);
}
