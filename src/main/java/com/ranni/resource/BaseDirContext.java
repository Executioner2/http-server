package com.ranni.resource;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * Title: HttpServer
 * Description:
 * 抽象文件夹容器，可以把这个DirContext其实现类的容器看作是一个文件夹的封装对象
 * JNDI的抽象目录容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 19:06
 */
public abstract class BaseDirContext implements DirContext {
    protected boolean cached = true; // 是否缓存
    protected int cacheTTL = 5000; // 缓存生存时间，5s
    protected int cacheObjectMaxSize = 32768; // 缓存对象最大大小，32k
    protected Hashtable env; // 环境
    protected String docBase; // 此容器代表的根目录，必须是个文件夹

    public BaseDirContext() {
        this.env = new Hashtable();
    }

    public BaseDirContext(Hashtable env) {
        this.env = env;
    }

    /**
     * 返回此组件根目录
     *
     * @return
     */
    public String getDocBase() {
        return docBase;
    }

    /**
     * 设置此组件根目录
     *
     * @param docBase
     */
    public void setDocBase(String docBase) {
        if (docBase == null) throw new IllegalArgumentException("文件夹路径不能为空！");
        this.docBase = docBase;
    }

    /**
     * 设置缓存标志位
     *
     * @param cached
     */
    public void setCached(boolean cached) {
        this.cached = cached;
    }

    /**
     * 是否缓存
     *
     * @return
     */
    public boolean isCached() {
        return this.cached;
    }


    /**
     * 取得属性值
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name.toString());
    }

    /**
     * 取得属性值
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(name, null);
    }

    /**
     * 取得属性值
     *
     * @param name
     * @param attrIds
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return getAttributes(name.toString(), attrIds);
    }

    /**
     * 修改属性值
     *
     * @param name
     * @param mod_op
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        modifyAttributes(name.toString(), mod_op, attrs);
    }


    /**
     * 修改属性值
     *
     * @param name
     * @param mods
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        modifyAttributes(name.toString(), mods);
    }

    /**
     * 绑定
     *
     * @param name
     * @param obj
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        bind(name.toString(), obj, attrs);
    }

    /**
     * 绑定
     *
     * @param name
     * @param obj
     * @throws NamingException
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {
        bind(name, obj, null);
    }

    /**
     * 重新绑定
     *
     * @param name
     * @param obj
     * @throws NamingException
     */
    @Override
    public void rebind(String name, Object obj) throws NamingException {
        rebind(name, obj, null);
    }

    /**
     * 重新绑定
     *
     * @param name
     * @param obj
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        rebind(name.toString(), obj, attrs);
    }

    /**
     * 创建子容器
     *
     * @param name
     * @param attrs
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return createSubcontext(name.toString(), attrs);
    }

    /**
     * 返回架构？？？
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchema(Name name) throws NamingException {
        return getSchema(name.toString());
    }

    /**
     * 返回类定义的架构
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return getSchemaClassDefinition(name.toString());
    }

    /**
     * 返回迭代器
     *
     * @param name
     * @param matchingAttributes
     * @param attributesToReturn
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return search(name.toString(), matchingAttributes, attributesToReturn);
    }


    /**
     * 返回迭代器
     *
     * @param name
     * @param matchingAttributes
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return search(name.toString(), matchingAttributes);
    }


    /**
     * 返回迭代器
     *
     * @param name
     * @param filter
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return search(name.toString(), filter, cons);
    }

    /**
     * 返回迭代器
     *
     * @param name
     * @param filterExpr
     * @param filterArgs
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return search(name.toString(), filterExpr, filterArgs, cons);
    }

    /**
     * 解析
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    /**
     * 绑定
     *
     * @param name
     * @param obj
     * @throws NamingException
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        bind(name.toString(), obj);
    }


    /**
     * 重新绑定
     *
     * @param name
     * @param obj
     * @throws NamingException
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        rebind(name.toString(), obj);
    }

    /**
     * 解绑
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void unbind(Name name) throws NamingException {
        unbind(name.toString());
    }

    /**
     * 重命名
     *
     * @param oldName
     * @param newName
     * @throws NamingException
     */
    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        rename(oldName.toString(), newName.toString());
    }


    /**
     * 返回name对应的values的迭代器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    /**
     * 返回绑定的参数的迭代器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    /**
     * 销毁子容器
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(name.toString());
    }


    /**
     * 创建子容器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name.toString());
    }

    /**
     * 解析连接
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name.toString());
    }

    /**
     * 获取name解析器
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return new NameParserImpl();
    }

    /**
     * 获取name解析器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return new NameParserImpl();
    }

    /**
     * 名字组合
     *
     * @param name
     * @param prefix
     * @return
     * @throws NamingException
     */
    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        prefix = (Name) name.clone();
        return prefix.addAll(name);
    }

    /**
     * 名字组合
     *
     * @param name
     * @param prefix
     * @return
     * @throws NamingException
     */
    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + "/" + name;
    }

    /**
     * 添加环境参数
     *
     * @param propName
     * @param propVal
     * @return
     * @throws NamingException
     */
    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return env.put(propName, propVal);
    }

    /**
     * 移除环境参数
     *
     * @param propName
     * @return
     * @throws NamingException
     */
    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return env.remove(propName);
    }

    /**
     * 取得环境表
     *
     * @return
     * @throws NamingException
     */
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return env;
    }

    /**
     * 关闭（清空环境）
     *
     * @throws NamingException
     */
    @Override
    public void close() throws NamingException {
        env.clear();
    }

    /**
     * 返回缓存的生存时间
     *
     * @return
     */
    public int getCacheTTL() {
        return this.cacheTTL;
    }

    /**
     * 缓存对象最大大小
     *
     * @return
     */
    public int getCacheObjectMaxSize() {
        return this.cacheObjectMaxSize;
    }

    /**
     * 释放资源
     */
    public abstract void release();
}
