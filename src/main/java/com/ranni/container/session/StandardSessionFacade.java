package com.ranni.container.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 * StandardSession的外观类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 16:17
 */
public class StandardSessionFacade implements HttpSession {
    private HttpSession session;

    public StandardSessionFacade(HttpSession session) {
        this.session = session;
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return session.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        session.setMaxInactiveInterval(i);
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return session.getSessionContext();
    }

    @Override
    public Object getAttribute(String s) {
        return session.getAttribute(s);
    }

    @Override
    public Object getValue(String s) {
        return getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        return session.getAttributeNames();
    }

    @Override
    public String[] getValueNames() {
        return session.getValueNames();
    }

    @Override
    public void setAttribute(String s, Object o) {
        session.setAttribute(s, o);
    }

    @Override
    public void putValue(String s, Object o) {
        setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        session.removeAttribute(s);
    }

    @Override
    public void removeValue(String s) {
        removeAttribute(s);
    }

    @Override
    public void invalidate() {
        session.invalidate();
    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }
}
