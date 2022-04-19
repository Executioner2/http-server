package com.ranni.session;

import com.ranni.container.Container;

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
     * 移除这个id的session
     *
     * @param id
     */
    void remove(String id);


    /**
     * 关联此session
     *
     * @param session
     */
    void add(Session session);


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
     * 移除这个session
     *
     * @param session
     */
    void remove(Session session);
}
