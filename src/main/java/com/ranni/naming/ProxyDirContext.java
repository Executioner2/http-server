package com.ranni.naming;

import org.apache.commons.collections4.map.LRUMap;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 这是资源文件的代理类，主要是增加缓存表
 * 提高获取资源文件的效率
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 19:00
 */
public class ProxyDirContext implements DirContext {
    public static final String CONTEXT = "context";
    public static final String HOST = "host";

    protected String vPath; // 虚拟路径
    protected Hashtable<String, String> env; // 环境
    protected DirContext dirContext; // 被代理的资源文件
    protected Map cache = null; // 缓存
    protected int cachedSize = 1000; // 缓存大小
    protected int cacheTTL = 5000; // 缓存生存时间 5s
    protected int cacheObjectMaxSize = 32768; // 缓存对象最大大小 32 KB
    protected String hostName; // 主机名称
    protected String contextName; // 容器名字


    /**
     * 创建一个资源文件代理对象
     * 并设置缓存表，缓存生存时间和缓存对象的最大大小
     *
     * @param env 传入的环境变量
     * @param dirContext 被代理的资源文件对象
     */
    public ProxyDirContext(Hashtable<String, String> env, DirContext dirContext) {
        this.env = env;
        this.dirContext = dirContext;
        if (dirContext instanceof BaseDirContext) {
            if (((BaseDirContext)dirContext).isCached()) {
                cache = Collections.synchronizedMap(new LRUMap(cachedSize));
                cacheTTL = ((BaseDirContext)dirContext).getCacheTTL(); // 获取缓存的生存时间
                cacheObjectMaxSize = ((BaseDirContext)dirContext).getCacheObjectMaxSize(); // 获取缓存对象最大大小
            }
        }
        hostName = env.get(HOST);
        contextName = env.get(CONTEXT);
    }

    /**
     * 在已有的资源文件代理对象的基础上再创建一个资源文件代理对象，
     * 继承传入的资源文件代理对象的缓存相关参数
     *
     * @param proxyDirContext 被继承缓存参数的的代理资源文件对象
     * @param dirContext 被代理的资源文件对象
     * @param vPath 虚拟路径
     */
    public ProxyDirContext(ProxyDirContext proxyDirContext,
                           DirContext dirContext, String vPath) {
        this.env = proxyDirContext.env;
        this.dirContext = dirContext;
        this.vPath = vPath;
        this.cache = proxyDirContext.cache;
        this.cachedSize = proxyDirContext.cachedSize;
        this.cacheTTL = proxyDirContext.cacheTTL;
        this.cacheObjectMaxSize = proxyDirContext.cacheObjectMaxSize;
        this.hostName = proxyDirContext.hostName;
        this.contextName = proxyDirContext.contextName;
    }

    /**
     * 返回主机名
     *
     * @return
     */
    public String getHostName() {
        return this.hostName;
    }


    /**
     * 返回容器名
     *
     * @return
     */
    public String getContextName() {
        return this.contextName;
    }


    /**
     * 返回被代理的目录容器
     *
     * @return
     */
    public DirContext getDirContext() {
        return this.dirContext;
    }


    /**
     * 缓存的视图类，为ProxyDirContext的内部类
     */
    protected class CacheEntry {
        long timestamp = -1;
        String name = null;
        ResourceAttributes attributes = null;
        Resource resource = null;
        DirContext context = null;

        public void recycle() {
            timestamp = -1;
            name = null;
            attributes = null;
            resource = null;
            context = null;
        }


        public String toString() {
            return ("Cache entry: " + name + "\n"
                    + "Attributes: " + attributes + "\n"
                    + "Resource: " + resource + "\n"
                    + "Context: " + context);
        }
    }


    /**
     * 解析缓存
     *
     * @return
     */
    protected CacheEntry cacheLookup(String name) {
        if (cache == null)
            return null;

        CacheEntry cacheEntry = (CacheEntry) cache.get(name);

        if (cacheEntry == null) {
            // 缓存中不存在
            cacheEntry = new CacheEntry();
            cacheEntry.name = name;
            if (!cachedLoad(cacheEntry)) {
                // 如果载入到缓存失败，则返回null
                return null;
            }
            return cacheEntry;
        } else {
            // 验证缓存是否过期
            if (!validate(cacheEntry)) {
                // 是否修改过缓存
                if (!revalidate(cacheEntry)) {
                    // 修改过，移除该缓存
                    cacheUnload(cacheEntry.name);
                    return null;
                } else {
                    // 没有修改，可以设置新的时间戳后直接用
                    cacheEntry.timestamp = System.currentTimeMillis() + cacheTTL;
                }
            }

            return cacheEntry;
        }
    }

    /**
     * 移除指定缓存
     *
     * @param name
     */
    protected boolean cacheUnload(String name) {
        if (cache == null)
            return false;

        return cache.remove(name) != null;
    }

    /**
     * 判断属性对象是否修改过
     *
     * @param cacheEntry
     *
     * @return
     */
    private boolean revalidate(CacheEntry cacheEntry) {
        if (cacheEntry.attributes == null)
            return false;

        long lastModified1 = cacheEntry.attributes.getLastModified();// 最后修改时间
        long contentLength1 = cacheEntry.attributes.getContentLength();

        if (lastModified1 <= 0)
            return false;

        try {
            Attributes ta = dirContext.getAttributes(cacheEntry.name);// 重新获取属性
            ResourceAttributes attributes = null;
            if (!(ta instanceof ResourceAttributes)) {
                attributes = new ResourceAttributes(ta);
            } else {
                attributes = (ResourceAttributes) ta;
            }

            long lastModified2 = attributes.getLastModified();
            long contentLength2 = attributes.getContentLength();

            return (lastModified1 == lastModified2 && contentLength1 == contentLength2);

        } catch (NamingException e) {
            return false;
        }
    }

    /**
     * 验证缓存是否过期
     *
     * @param cacheEntry
     *
     * @return
     */
    private boolean validate(CacheEntry cacheEntry) {
        if (cacheEntry.resource != null
            && cacheEntry.resource.getContext() != null
            && cacheEntry.timestamp > System.currentTimeMillis()) {
            return true;
        }

        return false;
    }

    /**
     * 载入到缓存中
     *
     * @param cacheEntry 要载入到缓存中的缓存视图对象
     *
     * @return
     */
    protected boolean cachedLoad(CacheEntry cacheEntry) {
        if (cache == null)
            return false;

        String name = cacheEntry.name;

        // 填充属性
        if (cacheEntry.attributes == null) {
            try {
                Attributes attributes = dirContext.getAttributes(name);
                if (!(attributes instanceof ResourceAttributes)) {
                    cacheEntry.attributes = new ResourceAttributes(attributes);
                } else {
                    cacheEntry.attributes = (ResourceAttributes) attributes;
                }
            } catch (NamingException e) {
                return false;
            }
        }

        // 填充资源
        if (cacheEntry.resource == null && cacheEntry.context == null) {
            try {
                // 从被代理的资源文件对象中解析出资源对象
                Object object = dirContext.lookup(name);
                if (object instanceof InputStream) {
                    cacheEntry.resource = new Resource((InputStream)object);
                } else if (object instanceof DirContext) {
                    cacheEntry.context = (DirContext) object;
                } else if (object instanceof Resource) {
                    cacheEntry.resource = (Resource) object;
                } else {
                    cacheEntry.resource = new Resource(new ByteArrayInputStream(object.toString().getBytes()));
                }
            } catch (NamingException e) {
                return false;
            }
        }

        // 如果资源填充了，但是没有从资源的输入流中获取数据
        // 那么就从流中取得数据
        if (cacheEntry.resource != null && cacheEntry.resource.getContext() == null
            && cacheEntry.attributes.getContentLength() >= 0
            && cacheEntry.attributes.getContentLength() < cacheObjectMaxSize) {
            int length = (int) cacheEntry.attributes.getContentLength();

            try (InputStream is = cacheEntry.resource.streamContent()) {
                int pos = 0;
                byte[] b = new byte[length];
                while (pos < length) {
                    int read = is.read(b);
                    if (read < 0) break;
                    pos += read;
                }
                cacheEntry.resource.setContext(b);
            } catch (IOException e) {
                ;
            }
        }

        // 设置缓存的过期时间戳
        cacheEntry.timestamp = System.currentTimeMillis() + cacheTTL;

        // 添加到缓存中
        cache.put(name, cacheEntry);

        return true;
    }

    /**
     * 返回属性值，如果缓存当中有，就从缓存中拿
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name.toString());
    }

    /**
     * 返回属性值，如果缓存当中有，就从缓存中拿
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(String name) throws NamingException {
        // 从缓存中拿，且存入缓存，如果成功直接返回
        CacheEntry cacheEntry = cacheLookup(name);
        if (cacheEntry != null)
            return cacheEntry.attributes;

        Attributes attributes = dirContext.getAttributes(name);
        if (!(attributes instanceof ResourceAttributes))
            attributes = new ResourceAttributes(attributes);

        return attributes;
    }

    /**
     * 取得属性
     *
     * @param name
     * @param attrIds 要检索的属性的标识符。null: 表示应该检索所有属性；空数组表示不应检索任何内容
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return getAttributes(name.toString(), attrIds);
    }

    /**
     * 取得属性
     *
     * @param name
     * @param attrIds 要检索的属性的标识符。null: 表示应该检索所有属性；空数组表示不应检索任何内容
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        Attributes attributes = dirContext.getAttributes(name, attrIds);
        if (!(attributes instanceof ResourceAttributes))
            attributes = new ResourceAttributes(attributes);

        return attributes;
    }

    /**
     * 修改属性
     *
     * @param name
     * @param mod_op
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        dirContext.modifyAttributes(name, mod_op, attrs);
    }

    /**
     * 修改属性
     *
     * @param name
     * @param mod_op
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        dirContext.modifyAttributes(name, mod_op, attrs);
    }

    /**
     * 修改属性
     *
     * @param name
     * @param mods
     *
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        dirContext.modifyAttributes(name, mods);
    }

    /**
     * 修改属性
     *
     * @param name
     * @param mods
     *
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        dirContext.modifyAttributes(name, mods);
    }

    /**
     * 绑定
     *
     * @param name
     * @param obj
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirContext.bind(name, obj, attrs);
    }

    /**
     * 绑定
     *
     * @param name
     * @param obj
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        dirContext.bind(name, obj, attrs);
    }

    /**
     * 重新绑定
     *
     * @param name
     * @param obj
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirContext.rebind(name, obj, attrs);
    }

    /**
     * 重新绑定
     *
     * @param name
     * @param obj
     * @param attrs
     *
     * @throws NamingException
     */
    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        dirContext.rebind(name, obj, attrs);
    }

    /**
     * 创建子容器
     *
     * @param name
     * @param attrs
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return dirContext.createSubcontext(name, attrs);
    }

    /**
     * 创建子容器
     *
     * @param name
     * @param attrs
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return dirContext.createSubcontext(name, attrs);
    }

    /**
     * 返回关联结构
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public DirContext getSchema(Name name) throws NamingException {
        return dirContext.getSchema(name);
    }

    /**
     * 返回关联结构
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public DirContext getSchema(String name) throws NamingException {
        return dirContext.getSchema(name);
    }

    /**
     * 获取架构类定义
     *
     * @param name
     *
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return dirContext.getSchemaClassDefinition(name);
    }

    /**
     * 获取架构类定义
     *
     * @param name
     *
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return dirContext.getSchemaClassDefinition(name);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param matchingAttributes
     * @param attributesToReturn
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirContext.search(name, matchingAttributes, attributesToReturn);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param matchingAttributes
     * @param attributesToReturn
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirContext.search(name, matchingAttributes, attributesToReturn);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param matchingAttributes
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return dirContext.search(name, matchingAttributes);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param matchingAttributes
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return dirContext.search(name, matchingAttributes);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param filter
     * @param cons
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return dirContext.search(name, filter, cons);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param filter
     * @param cons
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return dirContext.search(name, filter, cons);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param filterExpr
     * @param filterArgs
     * @param cons
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirContext.search(name, filterExpr, filterArgs, cons);
    }

    /**
     * 返回查询迭代器
     *
     * @param name
     * @param filterExpr
     * @param filterArgs
     * @param cons
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirContext.search(name, filterExpr, filterArgs, cons);
    }

    /**
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        CacheEntry cacheEntry = cacheLookup(name.toString());
        if (cacheEntry != null) {
            if (cacheEntry.resource != null)
                return cacheEntry.resource;
            else
                return cacheEntry.context;
        }

        Object o = dirContext.lookup(name);
        if (o instanceof InputStream)
            return new Resource((InputStream) o);

        return o;
    }

    /**
     * 先从缓存中获取，如果缓存没有就去被代理对象中拿
     * 返回name对应的resource或context
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Object lookup(String name) throws NamingException {
        CacheEntry cacheEntry = cacheLookup(name);
        if (cacheEntry != null) {
            if (cacheEntry.resource != null)
                return cacheEntry.resource;
            else
                return cacheEntry.context;
        }

        Object o = dirContext.lookup(name);
        if (o instanceof InputStream)
            return new Resource((InputStream) o);
        else if (o instanceof DirContext || o instanceof Resource)
            return o;

        return new Resource(new ByteArrayInputStream(o.toString().getBytes()));
    }

    /**
     * 绑定key value
     * 并移除缓存中的key
     *
     * @param name
     * @param obj
     *
     * @throws NamingException
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        dirContext.bind(name, obj);
        cacheUnload(name.toString());
    }

    /**
     * 绑定key value
     * 并移除缓存中的key
     *
     * @param name
     * @param obj
     *
     * @throws NamingException
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {
        dirContext.bind(name, obj);
        cacheUnload(name);
    }

    /**
     * 重新绑定
     * 并移除缓存中的key
     *
     * @param name
     * @param obj
     *
     * @throws NamingException
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        dirContext.rebind(name, obj);
        cacheUnload(name.toString());
    }

    /**
     * 重新绑定
     * 并移除缓存中的key
     *
     * @param name
     * @param obj
     *
     * @throws NamingException
     */
    @Override
    public void rebind(String name, Object obj) throws NamingException {
        dirContext.rebind(name, obj);
        cacheUnload(name);
    }

    /**
     * 解除绑定
     * 并删除缓存中的key
     *
     * @param name
     *
     * @throws NamingException
     */
    @Override
    public void unbind(Name name) throws NamingException {
        dirContext.unbind(name);
        cacheUnload(name.toString());
    }

    /**
     * 解除绑定
     * 并删除缓存中的key
     *
     * @param name
     *
     * @throws NamingException
     */
    @Override
    public void unbind(String name) throws NamingException {
        dirContext.unbind(name);
        cacheUnload(name);
    }

    /**
     * 重命名key
     * 并删除缓存中的key
     *
     * @param oldName
     * @param newName
     *
     * @throws NamingException
     */
    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        dirContext.rename(oldName, newName);
        cacheUnload(oldName.toString());
    }

    /**
     * 重命名key
     * 并删除缓存中的key
     *
     * @param oldName
     * @param newName
     *
     * @throws NamingException
     */
    @Override
    public void rename(String oldName, String newName) throws NamingException {
        dirContext.rename(oldName, newName);
        cacheUnload(oldName);
    }

    /**
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return dirContext.list(name);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return dirContext.list(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return dirContext.listBindings(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return dirContext.listBindings(name);
    }

    /**
     * 销毁子容器
     *
     * @param name
     *
     * @throws NamingException
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        dirContext.destroySubcontext(name);
        cacheUnload(name.toString());
    }

    /**
     * 销毁子容器
     *
     * @param name
     *
     * @throws NamingException
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        dirContext.destroySubcontext(name);
        cacheUnload(name);
    }

    /**
     * 创建子容器
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return dirContext.createSubcontext(name);
    }

    /**
     * 创建子容器
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        return dirContext.createSubcontext(name);
    }

    /**
     * 查询连接
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        return dirContext.lookupLink(name);
    }

    /**
     * 查询连接
     *
     * @param name
     *
     * @return
     *
     * @throws NamingException
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        return dirContext.lookupLink(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return dirContext.getNameParser(name);
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return dirContext.getNameParser(name);
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        prefix = (Name) name.clone();
        return prefix.addAll(name);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + "/" + name;
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return dirContext.addToEnvironment(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return dirContext.removeFromEnvironment(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return null;
    }

    @Override
    public void close() throws NamingException {
        dirContext.close();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return dirContext.getNameInNamespace();
    }
}
