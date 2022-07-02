package com.ranni.container.session;

import com.ranni.container.Context;
import com.ranni.logger.Logger;
import com.ranni.util.Enumerator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 标准的session实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 16:17
 */
public class StandardSession implements Session, HttpSession, Serializable {
    private static HttpSessionContext sessionContext; // 与此session关联的HttpSessionContext

    private transient String authType; // 认证类型
    private transient boolean expiring; // 是否正在销毁此session
    private transient HttpSession facade; // 外观对象引用
    private transient Map<String, Object> notes = new HashMap<>(); // 此session的内部注释

    private Map<String, Object> attributes = new HashMap<>(); // 存入的参数
    private long creationTime; // 创建时间
    private Manager manager; // session管理器
    private long lastAccessedTime; // 上次访问session的时间
    private boolean isNew; // 此session是否未被访问过
    private boolean isValid; // 是否可用
    private long thisAccessedTime; // 此session当前访问时间
    private int maxInactiveInterval = -1; // 最大生存时间，单位秒，-1为永久存活
    private String id; // 此session id
    private int debug = Logger.INFORMATION; // debug级别


    public StandardSession(Manager manager) {
        this.manager = manager;
        if (manager instanceof ManagerBase)
            this.debug = ((ManagerBase) manager).getDebug();
    }


    /**
     * 返回日志信息输出级别
     *
     * @return
     */
    public int getDebug() {
        return debug;
    }


    /**
     * 设置日志信息输出级别
     *
     * @param debug
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回此session的认证类型
     *
     * @return
     */
    @Override
    public String getAuthType() {
        return this.authType;
    }


    /**
     * 设置此session的认证类型
     *
     * @param authType
     */
    @Override
    public void setAuthType(String authType) {
        this.authType = authType;
    }


    /**
     * 返回创建时间
     *
     * @return
     */
    @Override
    public long getCreationTime() {
        if (!isValid)
            throw new IllegalStateException("StandardSession.getCreationTime:  此session还不可用！");
        return this.creationTime;
    }


    /**
     * 设置此session创建时间
     * 会将最后访问时间和当前访问时间也设置上
     *
     * @param time
     */
    @Override
    public void setCreationTime(long time) {
        this.creationTime = time;
        this.lastAccessedTime = time;
        this.thisAccessedTime = time;
    }


    /**
     * 返回此session id
     *
     * @return
     */
    @Override
    public String getId() {
        return this.id;
    }


    /**
     * 设置此session id
     *
     * @param id
     */
    @Override
    public void setId(String id) {
        if (this.id != null && manager != null)
            manager.remove(this);

        this.id = id;

        if (manager != null)
            manager.add(this);

        // TODO 通知容器的监听器，此session id发生改变
    }


    /**
     * 取得此实现类的信息
     *
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 返回上次访问此session的时间
     *
     * @return
     */
    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }


    /**
     * 取得关联的servlet全局作用域
     *
     * @return
     */
    @Override
    public ServletContext getServletContext() {
        if (manager == null)
            return null;

        Context context = (Context) manager.getContainer();
        if (context == null)
            return null;

        return context.getServletContext();
    }


    /**
     * 返回此session的管理器
     *
     * @return
     */
    @Override
    public Manager getManager() {
        return this.manager;
    }


    /**
     * 设置此session的管理器
     *
     * @param manager
     */
    @Override
    public void setManager(Manager manager) {
        this.manager = manager;
    }


    /**
     * 取得最大生存时间
     *
     * @return
     */
    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }


    /**
     * 取得session context
     * 提供虚拟实现类的对象
     *
     * @return
     */
    @Override
    public HttpSessionContext getSessionContext() {
        if (sessionContext == null)
            sessionContext = new StandardSessionContext();
        return sessionContext;
    }


    /**
     * 取得name对应的属性值
     *
     * @param name
     * @return
     */
    @Override
    public Object getAttribute(String name) {
        if (!isValid)
            throw new IllegalStateException("StandardSession.getAttribute:  此session还不可用！");
        synchronized (attributes) {
            return attributes.get(name);
        }
    }


    /**
     * 实际上就是调用{@link StandardSession#getAttribute(String)}
     *
     * @param name
     * @return
     */
    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }


    /**
     * 返回此session所有属性名的迭代器
     *
     * @return
     */
    @Override
    public Enumeration getAttributeNames() {
        if (!isValid)
            throw new IllegalStateException("StandardSession.getAttributeNames:  此session还不可用！");
        synchronized (attributes) {
            return new Enumerator(attributes.keySet());
        }
    }


    /**
     * 以数组形式返回此session所有属性名
     *
     * @return
     */
    @Override
    public String[] getValueNames() {
        if (!isValid)
            throw new IllegalStateException("StandardSession.getValueNames:  此session还不可用！");
        return keys();
    }


    /**
     * 以数组形式返回此session所有属性名
     *
     * @return
     */
    private String[] keys() {
        synchronized (attributes) {
            return attributes.keySet().toArray(new String[attributes.size()]);
        }
    }


    /**
     * 添加属性
     *
     * @param name
     * @param value
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (!isValid)
            throw new IllegalStateException("StandardSession.setAttribute:  此session还不可用！");

        if (name == null)
            throw new IllegalArgumentException("StandardSession.setAttribute:  name不能为null！");

        if (value == null)
            throw new IllegalArgumentException("StandardSession.setAttribute:  value不能为null！");

        // 如果此session所属的manager启用了序列化功能，而此value未实现序列化接口，那将抛出异常
        if (manager != null && manager.getDistributable()
            && !(value instanceof Serializable))
            throw new IllegalArgumentException("StandardSession.setAttribute:  value不支持序列化！");

        // 存入
        synchronized (attributes) {
            attributes.put(name, value);
        }

        // TODO 通知监听器session属性发生变化
    }


    /**
     * 实际上是调用{@link StandardSession#setAttribute(String, Object)}
     *
     * @param s
     * @param o
     */
    @Override
    public void putValue(String s, Object o) {
        setAttribute(s, o);
    }


    /**
     * 移除属性
     *
     * @param name
     */
    @Override
    public void removeAttribute(String name) {
        removeAttribute(name, true);
    }


    /**
     * 实际上是调用{@link StandardSession#removeAttribute(String)}
     *
     * @param s
     */
    @Override
    public void removeValue(String s) {
        removeAttribute(s);
    }


    /**
     * 使此session无效
     */
    @Override
    public void invalidate() {
        if (!isValid)
            throw new IllegalStateException("StandardSession.setAttribute:  此session还不可用！");
        expire();
    }


    /**
     * 是否是新创建的session
     *
     * @return
     */
    @Override
    public boolean isNew() {
        if (!isValid)
            throw new IllegalStateException("StandardSession.isNew:  此session还不可用！");
        return this.isNew;
    }


    /**
     * 设置最大生存时间
     *
     * @param interval
     */
    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }


    /**
     * 设置新session标志位
     *
     * @param isNew 是否是新创建的session
     */
    @Override
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }


    /**
     * 返回这个session的外观对象引用
     *
     * @return
     */
    @Override
    public HttpSession getSession() {
        if (this.facade == null)
            this.facade = new StandardSessionFacade(this);
        return this.facade;
    }


    /**
     * 设置此session是否可用
     *
     * @param isValid
     */
    @Override
    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }


    /**
     * 返回这个session的有效标志位
     *
     * @return
     */
    @Override
    public boolean isValid() {
        return this.isValid;
    }


    /**
     * 更新此session本次的访问时间
     */
    @Override
    public void access() {
        this.isNew = false;
        this.lastAccessedTime = this.thisAccessedTime;
        this.thisAccessedTime = System.currentTimeMillis();
    }


    /**
     * 使此session无效
     */
    @Override
    public void expire() {
        expire(true);
    }


    /**
     * 使此session无效
     *
     * @param notify 是否通知监听器
     */
    public void expire(boolean notify) {
        if (expiring) // 是否正在删除
            return;

        expiring = true;
        setValid(false);

        if (manager != null)
            manager.remove(this);

        String[] keys = keys();
        for (int i = 0; i < keys().length; i++) {
            removeAttribute(keys[i], notify);
        }

        // TODO 通知相关的监听器

        expiring = false; // 删除完毕
        if (manager != null && manager instanceof ManagerBase) {
            recycle();
        }
    }

    /**
     * 删除属性
     *
     * @param name 要删除的属性的name
     * @param notify 是否通知相关的监听器
     */
    public void removeAttribute(String name, boolean notify) {
        if (!expiring && !isValid)
            throw new IllegalStateException("StandardSession.setAttribute:  此session还不可用！");

        synchronized (attributes) {
            attributes.remove(name);
        }

        // TODO 通知相关的监听器
    }


    @Override
    public Object getNote(String name) {
        return null;
    }

    @Override
    public Iterator getNoteNames() {
        return null;
    }


    /**
     * 初始化属性
     */
    @Override
    public void recycle() {
        attributes.clear();
        setAuthType(null);
        creationTime = 0L;
        expiring = false;
        id = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        notes.clear();
        isNew = false;
        isValid = false;
        Manager savedManager = manager;
        manager = null;

        if ((savedManager != null) && (savedManager instanceof ManagerBase))
            ((ManagerBase) savedManager).recycle(this);
    }

    @Override
    public void removeNote(String name) {

    }

    @Override
    public void setNote(String name, Object value) {

    }

    @Override
    public void endAccess() {
        
    }

    @Override
    public String getIdInternal() {
        return this.id;
    }
}


/**
 * XXX 这个内部类是为了支持过期的HttpSessionContext，其本身是个虚拟实现
 */
final class StandardSessionContext implements HttpSessionContext {
    private Map dummy = new HashMap();

    @Override
    public HttpSession getSession(String s) {
        return null;
    }

    @Override
    public Enumeration getIds() {
        return new Enumerator(dummy);
    }
}
