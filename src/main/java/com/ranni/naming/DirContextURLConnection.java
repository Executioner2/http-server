package com.ranni.naming;

import com.ranni.util.Enumerator;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 连接到JNDI目录容器
 * 因为对象属性名是WebDAV而不是HTTP，所以重写URLConnection
 * 主要作用就是从传入的目录容器中再连接子容器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-12 16:15
 */
@Deprecated // 暂时还用不到
public class DirContextURLConnection extends URLConnection {
    protected DirContext context; // 目录容器
    protected Resource resource; // 资源
    protected DirContext collection; // 解析出来的目录容器
    protected Object object; // 执行connect()方法后可能得到的子容器/子资源对象（正因不确定类型才为Object）
    protected Attributes attributes; // 属性
    protected long date; // 日期
    protected Permission permission;


    public DirContextURLConnection(DirContext context, URL u) {
        super(u);
        if (context == null)
            throw new IllegalArgumentException("目录容器不能为空！");

        if (System.getSecurityManager() != null)
            this.permission = new JndiPermission(url.toString());

        if (u != null) {
            try {
                Object lookup = context.lookup(u.getPath());
                if (lookup instanceof Resource)
                    resource = (Resource) lookup;
            } catch (NamingException e) {
                ;
            }
        }
        this.context = context;
    }


    /**
     * 连接到DirContext，并检测绑定的对象和属性
     * 如果没有对象与URL中指定的名称绑定，则抛出异常
     *
     * @throws IOException 没有对象与URL中指定的名称绑定
     */
    @Override
    public void connect() throws IOException {
        if (!connected) {
            date = System.currentTimeMillis();
            String path = getURL().getFile(); // 从URL中取得文件路径

            if (context instanceof ProxyDirContext) {
                ProxyDirContext proxyDirContext = (ProxyDirContext) context;
                String hostName = proxyDirContext.getHostName(); // hostName无"/"开头
                String contextName = proxyDirContext.getContextName(); // contextName以"/"开头

                if (hostName != null) {
                    if (!path.startsWith("/" + hostName + "/"))
                        return;
                    path = path.substring(hostName.length() + 1);
                }

                if (contextName != null) {
                    if (!path.startsWith(contextName + "/"))
                        return;
                    path = path.substring(contextName.length());
                }
            }

            try {

                object = context.lookup(path);
                attributes = context.getAttributes(path);

                // 是资源还是文件目录
                if (object instanceof Resource)
                    resource = (Resource) object;
                else if (object instanceof DirContext)
                    collection = (DirContext) object;

            } catch (NamingException e) {
                ;
            }

            connected = true;
        }
    }


    /**
     * 取得内容长度
     *
     * @return
     */
    public int getContentLength() {
        return getHeaderFieldInt(ResourceAttributes.CONTENT_LENGTH, -1);
    }


    /**
     * 取得类容类型
     *
     * @return
     */
    public String getContentType() {
        return getHeaderField(ResourceAttributes.CONTENT_TYPE);
    }


    /**
     * 取得连接日期
     *
     * @return
     */
    public long getDate() {
        return date;
    }


    /**
     * 取得最后修改时间
     *
     * @return
     */
    public long getLastModified() {
        if (!connected) {
            try {
                connect();
            } catch (IOException e) {

            }
        }

        if (attributes == null)
            return 0;

        Attribute lastModified = attributes.get(ResourceAttributes.LAST_MODIFIED);
        if (lastModified != null) {
            try {
                Date o = (Date) lastModified.get();
                return o.getTime();
            } catch (NamingException e) {
                ;
            }
        }

        return 0;
    }


    /**
     * 取得指定的字段值
     *
     * @param name
     * @return
     */
    public String getHeaderField(String name) {
        if (!connected) {
            try {
                connect();
            } catch (IOException e) {

            }
        }

        if (attributes == null)
            return null;

        Attribute attribute = attributes.get(name);
        if (attribute != null) {
            try {
                return attribute.get().toString();
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * 取得执行过连接后得到的对象
     *
     * @return
     */
    public Object getContent() throws IOException {
        if (!connected)
            connect();

        if (resource != null)
            return getInputStream();
        if (collection != null)
            return collection;
        if (object != null)
            return object;

        throw new FileNotFoundException();
    }


    /**
     * 取得执行过连接后得到的对象
     * 然后进行匹配校验，如果与传入的类型集合中的一个匹配就直接返回该对象
     *
     * @return
     */
    public Object getContent(Class[] classes) throws IOException {
        Object object = getContent();

        for (Class clazz : classes) {
            if (clazz.isInstance(object))
                return object;
        }

        return null;
    }


    /**
     * 取得输入流
     *
     * @return
     */
    public InputStream getInputStream() throws IOException {
        if (!connected)
            connect();

        // 保证连接得到的是个资源
        if (resource == null) {
            throw new FileNotFoundException();
        } else {
            // 重新打开资源
            try {
                resource = (Resource) context.lookup(getURL().getFile());
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        return resource.streamContent();
    }


    /**
     * 取得这个URL的权限
     *
     * @return
     */
    public Permission getPermission() {
        return permission;
    }


    /**
     * 列出此集合（this.collection）的子项
     *
     * @return
     */
    public Enumeration list() throws IOException {
        if (!connected)
            connect();

        if (object == null)
            throw new FileNotFoundException();

        List<String> list = Collections.synchronizedList(new ArrayList<>());
        if (collection != null) {
            try {
                NamingEnumeration<NameClassPair> it = context.list(getURL().getFile());
                while (it.hasMoreElements()) {
                    NameClassPair ncp = it.nextElement();
                    list.add(ncp.getName());
                }
            } catch (NamingException e) {
                throw new FileNotFoundException();
            }
        }

        return new Enumerator(list);
    }
}
