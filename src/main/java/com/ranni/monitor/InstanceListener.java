package com.ranni.monitor;

/**
 * Title: HttpServer
 * Description:
 * 实例监听器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 22:49
 */
public interface InstanceListener {

    /**
     * 当触发时间后，通知给观察者
     *
     * @param event
     */
    void instanceEvent(InstanceEvent event);
}
