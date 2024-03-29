package com.ranni.lifecycle;

/**
 * Title: HttpServer
 * Description:
 * 生命周期接口，统一管理服务器中容器的生命周期
 * 此类为被观察目标所要实现的接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-02 19:13
 */
public interface Lifecycle {
    /**
     * 初始化前
     */
    String BEFORE_INIT_EVENT = "before_init";


    /**
     * 初始化后
     */
    String AFTER_INIT_EVENT = "after_init";
    
    
    /**
     * 启动事件
     */
    String START_EVENT = "start";
    

    /**
     * 启动前
     */
    String BEFORE_START_EVENT = "before_start";
    

    /**
     * 启动后
     */
    String AFTER_START_EVENT = "after_start";
    

    /**
     * 停止事件
     */
    String STOP_EVENT = "stop";
    

    /**
     * 停止前
     */
    String BEFORE_STOP_EVENT = "before_stop";
    

    /**
     * 停止后
     */
    String AFTER_STOP_EVENT = "after_stop";


    /**
     * 销毁前
     */
    String BEFORE_DESTROY_EVENT = "before_destroy";
    
    
    /**
     * 销毁后
     */
    String AFTER_DESTROY_EVENT = "after_destroy";
    

    /**--------------------------------------分隔线-----------------------------------------**/

    /**
     * 添加生命周期事件
     *
     * @param listener
     */
    void addLifecycleListener(LifecycleListener listener);


    /**
     * 返回所有生命周期事件
     *
     * @return
     */
    LifecycleListener[] findLifecycleListeners();


    /**
     * 移除生命周期事件
     *
     * @param listener
     */
    void removeLifecycleListener(LifecycleListener listener);


    /**
     * 启动器
     *
     * @throws Exception
     */
    void start() throws LifecycleException;



    /**
     * 停止器
     *
     * @throws Exception
     */
    void stop() throws LifecycleException;


    /**
     * @return 返回当前生命周期状态
     */
    default LifecycleState getState() {
        return LifecycleState.STARTING_PREP; 
    }

}
