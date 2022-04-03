package com.ranni.container.context;

import com.ranni.lifecycle.LifecycleEvent;
import com.ranni.lifecycle.LifecycleListener;

/**
 * Title: HttpServer
 * Description:
 * 简单的context容器生命周期监听器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 15:56
 */
public class SimpleContextLifecycleListener implements LifecycleListener {
    /**
     * 触发生命周期事件时调用此方法
     *
     * @param event
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        // TODO 这里仅仅打印事件类型
//        System.out.println("SimpleContextLifecycleListener: " + event.getType());
    }
}
