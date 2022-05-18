package com.ranni.container.wrapper;

import com.ranni.container.Context;
import com.ranni.container.monitor.InstanceEvent;
import com.ranni.container.monitor.InstanceListener;

import javax.servlet.Servlet;

/**
 * Title: HttpServer
 * Description:
 * Controller类配置监听类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/18 11:54
 */
public class ControllerConfig implements InstanceListener {

    /**
     * 监听事件
     * 
     * @param event
     */
    @Override
    public void instanceEvent(InstanceEvent event) {
        if (event.getType().equals(InstanceEvent.INSTANCE_EVENT)) {
            Context context = (Context) event.getWrapper().getParent();
            Class controller = context.findController(event.getWrapper().getName());
            StandardServlet servlet = (StandardServlet) event.getServlet();
            servlet.setClazz(controller);
        }
    }
}
