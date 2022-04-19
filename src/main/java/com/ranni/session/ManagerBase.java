package com.ranni.session;

import com.ranni.container.Container;
import com.ranni.container.DefaultContext;
import com.ranni.logger.Logger;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * session的管理器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 19:08
 */
public abstract class ManagerBase implements Manager {
    protected static final String DEFAULT_ALGORITHM = "MD5"; // 默认的session算法

    protected Container container; // 关联的context容器
    protected int debug = Logger.INFORMATION; // 日志级别
    protected DefaultContext defaultContext; // 默认容器
    protected boolean distributable; // 持久化标志
    protected int maxInactiveInterval = 60; // 最大生存时间，单位秒
    protected static String name = "ManagerBase";
    protected Map<String, Session> sessions = new HashMap(); // session集合
    protected String randomClass = "java.security.SecureRandom"; // session id生成器类名
    protected ArrayList recycled = new ArrayList(); // 回收的session对象
    protected String entropy; // 生成session id的熵
    protected MessageDigest digest; // 生存session id所使用的算法



    public abstract void recycle(StandardSession standardSession);


    public abstract int getDebug();


    @Override
    public DefaultContext getDefaultContext() {
        return null;
    }

    @Override
    public void setDefaultContext(DefaultContext defaultContext) {

    }

    @Override
    public void setContainer(Container container) {

    }

    @Override
    public Container getContainer() {
        return null;
    }

    @Override
    public boolean getDistributable() {
        return false;
    }

    @Override
    public void setDistributable(boolean distributable) {

    }

    @Override
    public int getMaxInactiveInterval() {
        return 0;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {

    }

    @Override
    public Session createSession() {
        return null;
    }

    @Override
    public Session findSession(String id) throws IOException {
        return null;
    }

    @Override
    public Session[] findSessions() {
        return new Session[0];
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {

    }

    @Override
    public void unload() throws IOException {

    }

    @Override
    public void add(Session session) {

    }

    @Override
    public void remove(Session session) {

    }

    @Override
    public void remove(String id) {

    }
}
