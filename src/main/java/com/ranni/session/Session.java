package com.ranni.session;

import javax.servlet.http.HttpSession;
import java.util.Iterator;

/**
 * Title: HttpServer
 * Description:
 * Session接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-18 18:48
 */
public interface Session {
    /**
     * Session创建事件
     */
    String SESSION_CREATED_EVENT = "createSession";


    /**
     * Session销毁事件
     */
    String SESSION_DESTROYED_EVENT = "destroySession";



    /**
     * 取得认证类型
     *
     * @return
     */
    String getAuthType();


    /**
     * 设置认证类型
     *
     * @param authType
     */
    void setAuthType(String authType);


    /**
     * 返回这个session创建时间
     *
     * @return
     */
    long getCreationTime();


    /**
     * 设置这个session的创建时间
     *
     * @param time
     */
    void setCreationTime(long time);


    /**
     * 返回这个session的id
     *
     * @return
     */
    String getId();


    /**
     * 设置这个session的id
     *
     * @param id
     */
    void setId(String id);


    /**
     * 返回实现类的信息
     *
     * @return
     */
    String getInfo();


    /**
     * 返回此session上次被请求的时间
     *
     * @return
     */
    long getLastAccessedTime();


    /**
     * Return the Manager within which this Session is valid.
     */
//    public Manager getManager();


    /**
     * Set the Manager within which this Session is valid.
     *
     * @param manager The new Manager
     */
//    public void setManager(Manager manager);


    /**
     * 返回此session的最大生存时间。-1为永不过期。每次被请求会刷新到期时间
     *
     * @return
     */
    int getMaxInactiveInterval();


    /**
     * 设置session的最大生存时间。-1为永不过期。
     *
     * @param interval
     */
    void setMaxInactiveInterval(int interval);


    /**
     * 这个session是否是新创建的
     *
     * @param isNew
     */
    void setNew(boolean isNew);


    /**
     * Return the authenticated Principal that is associated with this Session.
     * This provides an <code>Authenticator</code> with a means to cache a
     * previously authenticated Principal, and avoid potentially expensive
     * <code>Realm.authenticate()</code> calls on every request.  If there
     * is no current associated Principal, return <code>null</code>.
     */
//    Principal getPrincipal();


    /**
     * Set the authenticated Principal that is associated with this Session.
     * This provides an <code>Authenticator</code> with a means to cache a
     * previously authenticated Principal, and avoid potentially expensive
     * <code>Realm.authenticate()</code> calls on every request.
     *
     * @param principal The new Principal, or <code>null</code> if none
     */
//    void setPrincipal(Principal principal);


    /**
     * 返回StandardSessionFacade
     *
     * @return
     */
    HttpSession getSession();


    /**
     * 标志此session还是否有效
     *
     * @param isValid
     */
    void setValid(boolean isValid);


    /**
     * 返回这个session还是否有效
     *
     * @return
     */
    boolean isValid();


    /**
     * 更新此session最后的访问时间
     */
    void access();


    /**
     * Add a session event listener to this component.
     */
//    void addSessionListener(SessionListener listener);


    /**
     * 将此session置为不可用
     */
    void expire();


    /**
     * 返回存储在此session中指定name对应的内部注释
     * 内部注释：服务器组件和事件监听器在此
     * session中写入的注释
     *
     * @param name
     * @return
     */
    Object getNote(String name);


    /**
     * 取得此session中所有内部注释的name
     * 内部注释：服务器组件和事件监听器在此
     * session中写入的注释
     *
     * @return
     */
    Iterator getNoteNames();


    /**
     * 初始化参数
     */
    void recycle();


    /**
     * 移除此session中指定的内部注释
     * 内部注释：服务器组件和事件监听器在此
     * session中写入的注释
     *
     * @param name
     */
    void removeNote(String name);


    /**
     * Remove a session event listener from this component.
     */
//    void removeSessionListener(SessionListener listener);


    /**
     * 往此session中添加内部注释
     * 内部注释：服务器组件和事件监听器在此
     * session中写入的注释
     *
     * @param name 内部注释名
     * @param value 内部注释对象
     */
    void setNote(String name, Object value);
}
