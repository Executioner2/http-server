package com.ranni.session;

import com.ranni.container.Container;
import com.ranni.container.DefaultContext;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * session管理器接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 16:27
 */
public interface Manager {

    /**
     * 返回默认容器
     *
     * @return
     */
    DefaultContext getDefaultContext();


    /**
     * 设置默认容器
     *
     * @param defaultContext
     */
    void setDefaultContext(DefaultContext defaultContext);


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
     * 是否启用序列化（存储到存储器上）
     *
     * @return
     */
    boolean getDistributable();


    /**
     * 设置是否开启序列化
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
    Session findSession(String id) throws IOException;


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
     * 移除这个id的session
     *
     * @param id
     */
    void remove(String id);
}