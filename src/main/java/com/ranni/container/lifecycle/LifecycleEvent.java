package com.ranni.container.lifecycle;

import java.util.EventObject;

/**
 * Title: HttpServer
 * Description:
 * 生命周期事件类
 *
 * @see java.util.EventObject EventObject是java提供的一个工具类，为了方便程序员使用观察者模式
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-02 19:25
 */
public final class LifecycleEvent extends EventObject {
    private Lifecycle lifecycle; // 生命周期管理器
    private String type; // 事件类型
    private Object data; // 事件对象

    public LifecycleEvent(Lifecycle lifecycle, String type) {
        this(lifecycle, type, null);
    }

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        super(lifecycle);
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
