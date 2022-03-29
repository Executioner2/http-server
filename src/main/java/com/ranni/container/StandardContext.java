package com.ranni.container;

import com.ranni.container.pip.SimpleContextValve;
import com.ranni.util.CharsetMapper;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Title: HttpServer
 * Description:
 * 简单context容器实现类，等该实现类足够完整再晋升为标准实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-28 17:29
 */
public class StandardContext extends ContainerBase implements Context {

    protected String servletClass; // 要加载的servlet类全限定名
    protected Map<String, String> servletMappings = new HashMap<>(); // 请求servlet与wrapper容器的映射

    public StandardContext() {
        pipeline.setBasic(new SimpleContextValve(this));
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public CharsetMapper getCharsetMapper() {
        return null;
    }

    @Override
    public Object[] getApplicationListeners() {
        return new Object[0];
    }

    @Override
    public void setApplicationListeners(Object[] listeners) {

    }

    @Override
    public boolean getAvailable() {
        return false;
    }

    @Override
    public void setAvailable(boolean available) {

    }

    @Override
    public void setCharsetMapper(CharsetMapper mapper) {

    }

    @Override
    public boolean getConfigured() {
        return false;
    }

    @Override
    public void setConfigured(boolean configured) {

    }

    @Override
    public boolean getCookies() {
        return false;
    }

    @Override
    public void setCookies(boolean cookies) {

    }

    @Override
    public boolean getCrossContext() {
        return false;
    }

    @Override
    public void setCrossContext(boolean crossContext) {

    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void setDisplayName(String displayName) {

    }

    @Override
    public boolean getDistributable() {
        return false;
    }

    @Override
    public void setDistributable(boolean distributable) {

    }

    @Override
    public String getDocBase() {
        return null;
    }

    @Override
    public void setDocBase(String docBase) {

    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void setPath(String path) {

    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public void setPublicId(String publicId) {

    }

    @Override
    public boolean getReloadable() {
        return false;
    }

    @Override
    public void setReloadable(boolean reloadable) {

    }

    @Override
    public boolean getOverride() {
        return false;
    }

    @Override
    public void setOverride(boolean override) {

    }

    @Override
    public boolean getPrivileged() {
        return false;
    }

    @Override
    public void setPrivileged(boolean privileged) {

    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int timeout) {

    }

    @Override
    public String getWrapperClass() {
        return null;
    }

    @Override
    public void setWrapperClass(String wrapperClass) {

    }

    @Override
    public void addApplicationListener(String listener) {

    }

    @Override
    public void addParameter(String name, String value) {

    }

    /**
     * Context的实现类会有个名为servletMappings的map数据结构
     * key存放的是servlet的uri，即你在浏览器上输入正确的url地址
     * http://127.0.0.1/servlet/testServlet，那么/testServlet将是联系具体的wrapper对象的key
     * 所以可知，value存放的就是具体的wrapper名字
     * 添加pattern与wrapper对象的映射关系
     *
     * @param pattern
     * @param name
     */
    @Override
    public void addServletMapping(String pattern, String name) {
        synchronized (servletMappings) {
            servletMappings.put(pattern, name);
        }
    }

    @Override
    public Wrapper createWrapper() {
        return null;
    }

    @Override
    public String findMimeMapping(String extension) {
        return null;
    }

    @Override
    public String[] findMimeMappings() {
        return new String[0];
    }

    @Override
    public String findParameter(String name) {
        return null;
    }

    @Override
    public String[] findParameters() {
        return new String[0];
    }

    /**
     * 返回servletMapping中指定pattern对应的wrapper名
     *
     * @param pattern
     * @return
     */
    @Override
    public String findServletMapping(String pattern) {
        synchronized (servletMappings) {
            return servletMappings.get(pattern);
        }
    }

    /**
     * 返回servletMapping所有的key
     *
     * @return
     */
    @Override
    public String[] findServletMappings() {
        synchronized (servletMappings) {
            Set<String> keys = servletMappings.keySet();
            return keys.toArray(new String[keys.size()]);
        }
    }

    @Override
    public void reload() {

    }

    @Override
    public void removeParameter(String name) {

    }

    /**
     * 移除servletMapping中指定pattern对应的wrapper
     *
     * @param pattern
     */
    @Override
    public void removeServletMapping(String pattern) {
        synchronized (servletMappings) {
            servletMappings.remove(pattern);
        }
    }

    @Override
    public String getInfo() {
        return null;
    }
}
