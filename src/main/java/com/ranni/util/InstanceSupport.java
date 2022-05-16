package com.ranni.util;

import com.ranni.container.Wrapper;
import com.ranni.container.monitor.InstanceEvent;
import com.ranni.container.monitor.InstanceListener;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Title: HttpServer
 * Description:
 * servlet实例通知工具类
 * 这个类没什么技术点，直接复制的Tomcat的代码
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-25 22:47
 */
public final class InstanceSupport {

    private InstanceListener listeners[] = new InstanceListener[0];
    private Wrapper wrapper = null;


    public InstanceSupport(Wrapper wrapper) {
        this.wrapper = wrapper;
    }



    public Wrapper getWrapper() {

        return (this.wrapper);

    }


    public void addInstanceListener(InstanceListener listener) {

        synchronized (listeners) {
            InstanceListener results[] =
                    new InstanceListener[listeners.length + 1];
            for (int i = 0; i < listeners.length; i++)
                results[i] = listeners[i];
            results[listeners.length] = listener;
            listeners = results;
        }

    }



    public void fireInstanceEvent(String type, Filter filter) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Filter filter,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                exception);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Filter filter,
                                  ServletRequest request,
                                  ServletResponse response) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                request, response);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Filter filter,
                                  ServletRequest request,
                                  ServletResponse response,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                request, response, exception);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Servlet servlet) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Servlet servlet,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                exception);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Servlet servlet,
                                  ServletRequest request,
                                  ServletResponse response) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                request, response);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void fireInstanceEvent(String type, Servlet servlet,
                                  ServletRequest request,
                                  ServletResponse response,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                request, response, exception);
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);

    }



    public void removeInstanceListener(InstanceListener listener) {

        synchronized (listeners) {
            int n = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;
            InstanceListener results[] =
                    new InstanceListener[listeners.length - 1];
            int j = 0;
            for (int i = 0; i < listeners.length; i++) {
                if (i != n)
                    results[j++] = listeners[i];
            }
            listeners = results;
        }

    }
}
