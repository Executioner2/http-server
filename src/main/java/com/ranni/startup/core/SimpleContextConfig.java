package com.ranni.startup.core;

import com.ranni.container.Context;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleEvent;
import com.ranni.lifecycle.LifecycleListener;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 15:47
 */
public class SimpleContextConfig implements LifecycleListener {
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            Context context = (Context) event.getLifecycle();
            context.setConfigured(true);
        }
    }
}
