package com.ranni.container;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:51
 */
public interface Wrapper extends Container {


    /**
     * 返回此servlet在什么时候可用，返回的是毫秒
     * 如果是0则表示当前可用
     * 如果返回Long.MAX_VALUE则表示永久不可用
     * @return
     */
    long getAvailable();


    /**
     * 设置此servlet什么时候可用
     * 和上面的方法是一对的
     * @param available
     */
    void setAvailable(long available);


    /**
     * 返回此servlet的jsp文件路径
     * @return
     */
    String getJspFile();


    /**
     * 设置此servlet jsp文件的路径
     * @param jspFile
     */
    void setJspFile(String jspFile);


    /**
     * 返回启动时的加载顺序值，为负数时表示第一次调用时加载
     * @return
     */
    int getLoadOnStartup();


    /**
     * 设置加载顺序
     * @param value
     */
    void setLoadOnStartup(int value);


    /**
     * 返回此servlet的身份
     * @return
     */
    String getRunAs();


    /**
     * 设置此servlet的身份
     * @param runAs
     */
    void setRunAs(String runAs);


    /**
     * 返回servlet类的完全限定名
     * @return
     */
    String getServletClass();


    /**
     * 设置servlet类的完全限定名
     * @param servletClass
     */
    void setServletClass(String servletClass);


    /**
     * 返回此servlet是否可用的标志
     * @return
     */
    boolean isUnavailable();


    /**
     * 为此servlet添加新的初始化参数
     * @param name
     * @param value
     */
    void addInitParameter(String name, String value);


    /**
     * Add a new listener interested in InstanceEvents.
     *
     * @param listener The new listener
     */
//    public void addInstanceListener(InstanceListener listener);


    /**
     * Add a new security role reference record to the set of records for
     * this servlet.
     *
     * @param name Role name used within this servlet
     * @param link Role name used within the web application
     * @param description Description of this security role reference
     */
//    public void addSecurityReference(String name, String link);


    /**
     * 返回wrapper实例表示的servlet实例
     * 就是说，这个返回一个要被调用service方法的servlet
     * @return
     * @throws ServletException
     */
    Servlet allocate() throws ServletException;


    /**
     * 将之前分配servlet实例返回到可用实例池中
     * 把即拿去用了的servlet实例还回去
     * @param servlet
     * @throws ServletException
     */
    void deallocate(Servlet servlet) throws ServletException;


    /**
     * 返回指定初始化参数的值
     * @param name
     * @return
     */
    String findInitParameter(String name);


    /**
     * 返回所有初始化参数的name
     * @return
     */
    String[] findInitParameters();


    /**
     * Return the security role link for the specified security role
     * reference name, if any; otherwise return <code>null</code>.
     *
     * @param name Security role reference used within this servlet
     */
//    String findSecurityReference(String name);


    /**
     * Return the set of security role reference names associated with
     * this servlet, if any; otherwise return a zero-length array.
     */
//    String[] findSecurityReferences();


    /**
     * 加载并初始化这个servlet
     * @throws ServletException
     */
    void load() throws ServletException;


    /**
     * Remove the specified initialization parameter from this servlet.
     *
     * @param name Name of the initialization parameter to remove
     */
//    public void removeInitParameter(String name);


    /**
     * Remove a listener no longer interested in InstanceEvents.
     *
     * @param listener The listener to remove
     */
//    public void removeInstanceListener(InstanceListener listener);


    /**
     * Remove any security role reference for the specified role name.
     *
     * @param name Security role used within this servlet to be removed
     */
//    public void removeSecurityReference(String name);


    /**
     * 处理不可用异常，标记这个servlet在规定时间为不可用
     * @param unavailable
     */
    void unavailable(UnavailableException unavailable);


    /**
     * 卸载此servlet的所有初始化实例
     * @throws ServletException
     */
    void unload() throws ServletException;
}
