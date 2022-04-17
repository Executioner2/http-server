package com.ranni.container.loader;

import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.naming.Resource;
import com.ranni.naming.ResourceAttributes;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 15:45
 */
public class WebappClassLoader extends URLClassLoader implements Reloader, Lifecycle {
    // 不允许载入的类和包
    private static final String[] packageTriggers = {
            "javax",                                     // Java extensions
            "org.xml.sax",                               // SAX 1 & 2
            "org.w3c.dom",                               // DOM 1 & 2
            "org.apache.xerces",                         // Xerces 1 & 2
            "org.apache.xalan"                           // Xalan
    };
    private static final String[] triggers = {
            "javax.servlet.Servlet"                     // Servlet API
    };

    private ClassLoader parent; // 父-类加载器
    private ClassLoader system; // 应用（系统）类加载器
    private Object lock = new Object(); // 锁

    protected DirContext resources; // 容器资源
    protected boolean delegate; // 委托标志
    protected boolean hasExternalRepositories; // 是否有拓展仓库集
    protected boolean started;
    protected String[] repositories = new String[0]; // 类仓库名，与类文件仓库的下标是对应的
    protected File[] files = new File[0]; // 类文件仓库，与类仓库名的的下标是对应的
    protected String jarPath; // JAR包的路径
    protected List<String> jarNames = new ArrayList<>(); // JAR包名集合
    protected List<JarFile> jarFiles = new ArrayList<>(); // JAR包列表
    protected List<File> jarRealFiles = new ArrayList<>(); // 存放JAR包中资源的文件夹集合
    protected List<Long> lastModifiedDates = new ArrayList<>(); // JAR的最后修改日期
    protected List<String> paths = new ArrayList<>(); // JAR包路径列表
    protected Set<String> notFoundResources = new HashSet<>(); // 未找到的资源名
    protected Map<String, ResourceEntry> resourceEntries = new HashMap<>(); // 已经载入的缓存资源

    public WebappClassLoader() {
        super(new URL[0]);
        this.parent = getParent();
        this.system = getSystemClassLoader();
    }

    public WebappClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parent = getParent();
        this.system = getSystemClassLoader();
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
        if (!started)
            throw new ClassNotFoundException(name);

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
     * 查询类
     *
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class clazz = findClassInternal(name);

        // 如果从主库中找不到资源而刚好有扩展库，那就尝试从扩展库中去找
        if (clazz == null && hasExternalRepositories) {
            clazz = super.findClass(name);
        }

        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }

        return clazz;
    }

    /**
     * 从本地存储中查找类
     *
     * @param name
     * @return
     */
    protected Class findClassInternal(String name) throws ClassNotFoundException {
        if (!validate(name))
            throw new ClassNotFoundException(name);

        String tempPath = name.replace('.', '/');
        String classPath = tempPath + ".class";

        ResourceEntry entry = null;
        entry = findResourceInternal(name, classPath);

        if (entry == null || entry.binaryContent == null)
            throw new ClassNotFoundException(name);

        Class clazz = entry.loadedClass;
        if (clazz != null)
            return clazz;

        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1)
            packageName = name.substring(0, pos);

        Package pkg = null;
        if (packageName != null) {
            pkg = getDefinedPackage(packageName);

            if (pkg == null) {
                if (entry.manifest == null) {
                    definePackage(packageName,null, null, null, null, null,
                            null, null);
                } else {
                    definePackage(packageName, entry.manifest, entry.codeBase);
                }
            }
        }

        CodeSource codeSource = new CodeSource(entry.codeBase, entry.certificates);

        if (entry.loadedClass == null) {
            synchronized (this) {
                // 双重(entry.loadedClass == null)判断，避免重复创建类加载器
                if (entry.loadedClass == null) {
                    clazz = defineClass(name, entry.binaryContent, 0,
                                        entry.binaryContent.length,
                                        codeSource);
                    entry.loadedClass = clazz;
                } else {
                    clazz = entry.loadedClass;
                }
            }
        } else {
            clazz = entry.loadedClass;
        }

        return clazz;
    }

    /**
     * 从本地资源中查询并解析资源
     *
     * @param name
     * @param path 相对路径
     * @return
     */
    private ResourceEntry findResourceInternal(String name, String path) {
        if (!started)
            return null;

        if (name == null || path == null)
            return null;

        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null)
            return entry;

        int contentLength = -1;
        InputStream binaryStream = null;
        Resource resource = null;

        // 尝试从repositories中找到需要被加载的类
        for (int i = 0; resource == null
            && i < repositories.length; i++) {

            try {
                String fullPath = repositories[i] + path;
                Object res = resources.lookup(fullPath);
                if (res instanceof Resource)
                    resource = (Resource) res;

                entry = new ResourceEntry();
                try {
                    entry.source = getURL(new File(files[i], path));
                    entry.codeBase = entry.source;
                } catch (MalformedURLException e) {
                    return null;
                }

                ResourceAttributes attributes = (ResourceAttributes) resources.getAttributes(fullPath);
                entry.lastModified = attributes.getLastModified();
                contentLength = (int) attributes.getContentLength();

                if (resource != null) {
                    try {
                        binaryStream = resource.streamContent();
                    } catch (IOException e) {
                        return null;
                    }

                    synchronized (lock) {
                        lastModifiedDates.add(entry.lastModified);
                        paths.add(fullPath);
                    }
                }

            } catch (NamingException e) {
                e.printStackTrace();
            }
        } // for end

        if (entry == null && notFoundResources.contains(name))
            return null;

        // 尝试从jarFiles中寻找
        JarEntry jarEntry = null;
        for (int i = 0; entry == null
            && i < jarFiles.size(); i++) {

            jarEntry = jarFiles.get(i).getJarEntry(path);

            if (jarEntry != null) {
                entry = new ResourceEntry();
                try {
                    entry.codeBase = getURL(jarRealFiles.get(i)); // jar包的绝对路径的URL
                    String jarFakeUrl = entry.codeBase.toString(); // jar包的绝对路径
                    jarFakeUrl = "jar:" + jarFakeUrl + "!/" + path; // emm，jar中资源的定位
                    entry.source = new URL(jarFakeUrl); // 资源地址
                } catch (MalformedURLException e) {
                    return null;
                }

                contentLength = (int) jarEntry.getSize();
                try {
                    entry.manifest = jarFiles.get(i).getManifest();
                    binaryStream = jarFiles.get(i).getInputStream(jarEntry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } // for end

        // 没有找到资源文件，加入未找到资源文件清单并返回null
        if (entry == null) {
            synchronized (notFoundResources) {
                notFoundResources.add(name);
            }
            return null;
        }

        if (binaryStream != null) {
            byte[] binaryContent = new byte[contentLength];
            try {
                int pos = 0;
                int n = 0;
                do {
                    n = binaryStream.read(binaryContent, pos, contentLength - n);
                    pos += n;
                } while (n > 0);
                binaryStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            entry.binaryContent = binaryContent;

            if (jarEntry != null) {
                entry.certificates = jarEntry.getCertificates();
            }
        }

        // 找到并解析了这个资源，现在把解析好的资源对象放到集合中
        synchronized (resourceEntries) {
            // 如果已经存在了就返回存在的（多线程问题）
            ResourceEntry entry2 = resourceEntries.get(name);
            if (entry2 == null) {
                resourceEntries.put(name, entry);
            } else {
                entry = entry2;
            }
        }

        return entry;
    }


    /**
     * 取得文件的URL
     *
     * @param file
     * @return
     */
    protected URL getURL(File file) throws MalformedURLException {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            ;
        }
        return file.toURI().toURL();
    }


    /**
     * 合法性验证
     *
     * @param name
     * @return
     */
    protected boolean validate(String name) {
        if (name == null)
            return false;
        if (name.startsWith("java."))
            return false;

        return true;
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

    /**
     * 添加拓展仓库
     * 这个就是web项目的拓展类库文件夹
     *
     * @param repository
     */
    @Override
    public void addRepository(String repository) {
        // 忽略标准仓库
        if (repository.startsWith("/WEB-INF/lib")
            || repository.startsWith("/WEB-INF/classes"))
            return;

        try {
            URL url = new URL(repository);
            super.addURL(url);
            hasExternalRepositories = true;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    @Override
    public String[] findRepositories() {
        return new String[0];
    }

    @Override
    public boolean modified() {
        return false;
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
     * 返回容器资源
     *
     * @return
     */
    public DirContext getResources() {
        return this.resources;
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
     * 将class仓库名和具体的File对象加入到集合
     *
     * @param repository
     * @param file
     */
    public synchronized void addRepository(String repository, File file) {
        if (repository == null)
            return;

        // XXX 这里将类仓库名和类文件仓库的拷贝当作了一个整体进行，如果抛出下标异常优先检查这里

        String[] res1 = new String[this.repositories.length + 1];
        File[] res2 = new File[this.files.length + 1];
        int i = 0;

        for (; i < files.length; i++) {
            res1[i] = repositories[i];
            res2[i] = files[i];
        }

        res1[i] = repository;
        res2[i] = file;

        repositories = res1;
        files = res2;
    }


    /**
     * 设置导入的JAR包路径
     *
     * @param jarPath
     */
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }


    /**
     * 返回JAR包的路径
     *
     * @return
     */
    public String getJarPath() {
        return this.jarPath;
    }


    /**
     * 导入JAR包
     * XXX 导入JAR包 后面可以改进
     *
     * @param path JAR包的相对路径
     * @param jarFile JAR包
     * @param file 存放此JAR包中资源的文件夹
     */
    public synchronized void addJar(String path, JarFile jarFile, File file) {
        if (path == null || jarFile == null || file == null) return;

        if (jarPath != null && path.startsWith(jarPath)) {
            String jarName = path.substring(jarPath.length());
            // 把开头的/剪掉
            while (jarName.startsWith("/"))
                jarName = jarName.substring(1);

            jarNames.add(jarName);
        }

        // 此JAR包最后修改时间
        try {
            long lastModified = ((ResourceAttributes) resources.getAttributes(path)).getLastModified();
            paths.add(path);
            lastModifiedDates.add(lastModified);

        } catch (NamingException e) {
            ;
        }

        // TODO 检查JAR包中有无无效类
//        if (!validateJarFile(file)) return;

        jarFiles.add(jarFile);
        jarRealFiles.add(file);

        // TODO 加载清单
    }

    /**
     * 不支持哦哦哦
     *
     * @return
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    /**
     * 不支持哦哦哦
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }

    /**
     * 不支持哦哦哦
     *
     * @return
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    /**
     * 仅设置启动标志位为true
     *
     * @return
     */
    @Override
    public void start() throws Exception {
        started = true;
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
     * 释放资源
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        started = false;

        for (JarFile jf : jarFiles)
            jf.close();

        notFoundResources.clear(); // 清除未找到的资源文件缓存
        resourceEntries.clear(); // 清除已解析的资源文件

        repositories = new String[0];
        jarPath = null;
        files = new File[0];
        jarFiles.clear();
        jarRealFiles.clear();
        jarNames.clear();
        paths.clear();
        lastModifiedDates.clear();
        hasExternalRepositories = false;
    }
}
