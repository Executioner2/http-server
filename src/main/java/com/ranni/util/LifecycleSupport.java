package com.ranni.util;

import com.ranni.container.lifecycle.Lifecycle;
import com.ranni.container.lifecycle.LifecycleEvent;
import com.ranni.container.lifecycle.LifecycleListener;

/**
 * Title: HttpServer
 * Description:
 * 生命周期接口的支持类（注意，并不是被观察的目标类）
 * 此类是为了方便其它要作为被观察的目标类使用的工具类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-02 19:42
 */
public final class LifecycleSupport {

    private Lifecycle lifecycle; // 被监听的对象
    private LifecycleListener[] lifecycleListeners = new LifecycleListener[0]; // 监听器（观察者）

    public LifecycleSupport(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * 添加监听器
     *
     * @param listener
     */
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            LifecycleListener[] listeners = new LifecycleListener[lifecycleListeners.length + 1];
            System.arraycopy(lifecycleListeners, 0, listeners, 0, lifecycleListeners.length);
            listeners[lifecycleListeners.length] = listener;
            lifecycleListeners = listeners;
        }
    }

    /**
     * 返回所有监听器
     *
     * @return
     */
    public LifecycleListener[] findLifecycleListeners() {
        synchronized (lifecycleListeners) {
            return this.lifecycleListeners;
        }
    }

    /**
     * 移除指定监听器
     *
     * @param listener
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            int n = lifecycleListeners.length;
            int index = -1;

            // 找到要移除的监听器下标
            for (int i = 0; i < n; i++) {
                if (lifecycleListeners[i] == listener) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            LifecycleListener[] listeners = new LifecycleListener[lifecycleListeners.length - 1];
            for (int i = 0; i < n - 1; i++) {
                if (i < index) {
                    listeners[i] = lifecycleListeners[i];
                } else {
                    listeners[i] = lifecycleListeners[i + 1];
                }
            }

            lifecycleListeners = listeners;
        }
    }

    /**
     * 触发一个生命周期事件
     * 先复制事件监听器数组，接着调用数组中每个元素的lifecycleEvent()并传入要触发的事件
     *
     * @param type
     * @param data
     */
    public void fireLifecycleEvent(String type, Object data) {
        // 创建事件
        LifecycleEvent lifecycleEvent = new LifecycleEvent(lifecycle, type, data);

        // 就克隆要同步进行
        LifecycleListener[] clone = null;
        synchronized (lifecycleListeners) {
            clone = lifecycleListeners.clone();
        }

        for (LifecycleListener ll : clone) {
            // 执行监听器的方法
            ll.lifecycleEvent(lifecycleEvent);
        }
    }
}
