package com.ranni.session;

import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的session管理器实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-20 16:34
 */
public class StandardManager extends ManagerBase implements Lifecycle, Runnable {
    protected static String name = "StandardManager"; // 类名

    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具

    private boolean started; // 启动标志位
    private int checkInterval = 60; // 会话检查间隔时间，单位秒
    private int maxActiveSessions = -1; // 允许的活动session最大数量，-1为不限制


    /**
     * 添加监听器
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 返回所有生命周期监听器
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除监听器
     *
     * @param listener
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 启动session管理器
     * 主要做三件事：
     * 1、触发启动事件
     * 2、从存储器中载入session到内存中
     * 3、启动失效session回收线程
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        if (started)
            throw new IllegalStateException("此session管理器已经启动！");
        log("StandardManager.start  启动session管理器中");

        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        try {
            load();
        } catch (Throwable e) {
            log("StandardManager.start  从存储器中载入session失败！");
        }

        // 启动失效session回收线程
        threadStart();
    }


    /**
     * TODO 失效session回收线程
     */
    private void threadStart() {
    }


    @Override
    public void stop() throws Exception {

    }


    /**
     * 回收失效的session
     */
    @Override
    public void run() {

    }


    /**
     * TODO 从存储器载入到内存中
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @Override
    public void load() throws ClassNotFoundException, IOException {

    }


    /**
     * TODO 存储到存储器中
     *
     * @throws IOException
     */
    @Override
    public void unload() throws IOException {

    }


    /**
     * 设置容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        super.setContainer(container);

        // 重设最大生存时间
        if (this.container != null && this.container instanceof Context) {
            setMaxInactiveInterval(((Context) this.container).getSessionTimeout() * 60);
        }
    }


    /**
     * 创建session
     *
     * @return
     */
    @Override
    public Session createSession() {
        if (maxActiveSessions >= 0
            && sessions.size() >= maxActiveSessions)
            throw new IllegalStateException("StandardManager.createSession  超过最大活动session数量！");

        return super.createSession();
    }


    /**
     * 返回类名
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }
}
