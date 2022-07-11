package com.ranni.container.session;

import com.ranni.container.Container;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * session管理器接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 16:27
 * @Ref org.apache.catalina.Manager
 */
public interface Manager {

    /**
     * 设置此manager关联的context容器
     *
     * @param container
     */
    void setContainer(Container container);


    /**
     * 返回容器
     *
     * @return
     */
    Container getContainer();


    /**
     * 是否持久化到存储器上（存储到存储器上）
     *
     * @return
     */
    boolean getDistributable();


    /**
     * 设置是否开持久化
     *
     * @param distributable
     */
    void setDistributable(boolean distributable);


    /**
     * 取得最大生存时间
     *
     * @return
     */
    int getMaxInactiveInterval();


    /**
     * 设置最大生存时间
     *
     * @param interval
     */
    void setMaxInactiveInterval(int interval);


    /**
     * 创建session
     *
     * @return
     */
    Session createSession();


    /**
     * 根据id查找session
     * @param id
     * @return
     * @throws IOException
     */
    Session findSession(String id);


    /**
     * 返回所有session
     *
     * @return
     */
    Session[] findSessions();


    /**
     * 将session从存储设备上载入到内容中
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    void load() throws ClassNotFoundException, IOException;


    /**
     * 将session持久化到存储设备上（如果开启了持久化的话）
     *
     * @throws IOException
     */
    void unload() throws IOException;


    /**
     * 添加session
     *
     * @param session
     */
    void add(Session session);


    /**
     * 移除这个session
     *
     * @param session
     */
    void remove(Session session);


    /**
     * 后台任务
     */
    void backgroundProcess();


    /**
     * 设置Session回收标志位
     * 
     * @param sessionRecycle
     */
    void setSessionRecycle(boolean sessionRecycle);


    /**
     * 返回Session回收标志位
     * @return
     */
    boolean getSessionRecycle();


    /**
     * 生成新的session id
     * 
     * @param session
     * @return
     */
    String rotateSessionId(Session session);
}
