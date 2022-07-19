package com.ranni.loader;

import javax.naming.directory.DirContext;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/7/14 19:44
 */
public abstract class AbstractClassLoader extends URLClassLoader {

    // ==================================== 属性字段 ====================================

    /**
     * 不允许载入的类和包
     */
    protected static final String[] packageTriggers = {
            "javax",                                     // Java extensions
            "org.xml.sax",                               // SAX 1 & 2
            "org.w3c.dom",                               // DOM 1 & 2
            "org.apache.xerces",                         // Xerces 1 & 2
            "org.apache.xalan"                           // Xalan
    };
    protected static final String[] triggers = {
            "javax.servlet.Servlet"                     // Servlet API
    };

    /**
     * 父-类加载器
     */
    protected ClassLoader parent;

    /**
     * 应用（系统）类加载器
     */
    protected ClassLoader system;

    /**
     * 未找到的资源名
     */
    protected Set<String> notFoundResources = new HashSet<>();

    /**
     * 委托标志位
     */
    protected boolean delegate;

    /**
     * 容器资源
     */
    protected DirContext resources;
    
    /**
     * 已经载入的缓存资源
     */
    protected Map<String, ResourceEntry> resourceEntries = Collections.synchronizedMap(new HashMap<>());


    // ==================================== 构造方法 ====================================
    

    public AbstractClassLoader() {
       this(new URL[0]);       
    }
    
    public AbstractClassLoader(URL[] urls) {
        super(urls);
        this.parent = getParent();
        this.system = getSystemClassLoader();
    }

    public AbstractClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parent = getParent();
        this.system = getSystemClassLoader();
    }
    
    
    // ==================================== 核心方法 ====================================
    
    
    /**
     * 返回容器资源
     *
     * @return
     */
    public DirContext getResources() {
        return this.resources;
    }


    /**
     * 设置JNDI的容器资源
     *
     * @param resources
     */
    public void setResources(DirContext resources) {
        this.resources = resources;
    }
    

    /**
     * 资源回收
     */
    public void recycle() {
        notFoundResources.clear(); // 清除未找到的资源文件缓存
        resourceEntries.clear(); // 清除已解析的资源文件
    }
    
    
    /**
     * 查询缓存中是否已经加载此类
     *
     * @param name
     * @return
     */
    protected Class findLoadedClass0(String name) {
        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null)
            return entry.loadedClass;
        return null;
    }
    
    
    /**
     * 设置委托标志
     *
     * @param delegate
     */
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }


    /**
     * 返回委托标志
     *
     * @return
     */
    public boolean getDelegate() {
        return this.delegate;
    }
    
    
    /**
     * 类加载
     *
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }
    
    
    /**
     * 类加载
     *
     * @param name
     * @param resolve
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = null;

        // 尝试从WebappClassLoader的缓存中查询
        clazz = findLoadedClass0(name);
        if (clazz != null) {
            // 是否需要重新解析
            if (resolve)
                resolveClass(clazz);
            return clazz;
        }

        // 尝试从父类的缓存中查询
        clazz = findLoadedClass(name);
        if (clazz != null) {
            // 是否需要重新解析
            if (resolve)
                resolveClass(clazz);
            return clazz;
        }

        // 从系统类加载器中加载（JDK 9之前的双亲委派机制）
        try {
            clazz = system.loadClass(name);
            if (clazz != null) {
                // 是否需要重新加载
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            ;
        }

        // 是否委托加载，如果是被过滤的包也要启用委托
        boolean delegateLoad = delegate || filter(name);

        // 尝试委托加载
        if (delegateLoad) {
            ClassLoader loader = parent == null ? system : parent;

            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    // 是否需要重新加载
                    if (resolve)
                        resolveClass(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        // 尝试从本地加载
        try {
            clazz = findClass(name);
            if (clazz != null) {
                // 是否需要重新加载
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            ;
        }

        // 如果本地加载未加载上，且还没有委托加载，则尝试委托加载
        if (!delegateLoad) {
            ClassLoader loader = parent == null ? system : parent;

            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    // 是否需要重新加载
                    if (resolve)
                        resolveClass(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        // 没有找到这个类
        throw new ClassNotFoundException(name);
    }
    

    /**
     * 是否是被过滤的包
     *
     * @param name
     * @return
     */
    protected boolean filter(String name) {
        if (name == null) return false;

        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1)
            packageName = name.substring(0, pos);
        else
            return false;

        for (int i = 0; i < packageTriggers.length; i++) {
            if (packageName.startsWith(packageTriggers[i])) {
                return true;
            }
        }
        return false;
    }

}
