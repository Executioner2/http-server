package com.ranni.loader;

import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.naming.Resource;
import com.ranni.naming.ResourceAttributes;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
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
public class WebappClassLoader extends AbstractClassLoader implements Reloader, Lifecycle {

    // ==================================== 属性字段 ====================================\

    /**
     * 锁
     */
    private Object lock = new Object();

    /**
     * 日志输出等级
     */
    private int debug = Logger.ERROR;

    /**
     * 是否已经启动
     */
    protected boolean started;

    /**
     * 类仓库名，与类文件仓库的下标是对应的
     */
    protected String[] repositories = new String[0];

    /**
     * 类文件仓库，与类仓库名的的下标是对应的
     */
    protected File[] files = new File[0];

    /**
     * JAR包的路径
     */
    protected String jarPath;

    /**
     * JAR包名集合
     */
    protected List<String> jarNames = new ArrayList<>();

    /**
     * JAR包列表
     */
    protected List<JarFile> jarFiles = new ArrayList<>();

    /**
     * 存放JAR包中资源的文件夹集合
     */
    protected List<File> jarRealFiles = new ArrayList<>();

    /**
     * JAR的最后修改日期
     */
    protected List<Long> lastModifiedDates = new ArrayList<>();

    /**
     * JAR包路径列表
     */
    protected List<String> paths = new ArrayList<>();

    /**
     * 是否有拓展仓库集
     */
    protected boolean hasExternalRepositories;


    // ==================================== 核心方法 ====================================

    /**
     * 根据类名找到这个类文件并加载到JVM虚拟机中
     *
     * @param name 全限定类名
     * @return 返回找到并加载的类
     * @throws ClassNotFoundException 如果没有找到这个类将抛出此异常
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
        
        return super.loadClass(name, resolve);
    }
    

    /**
     * 从本地存储中查找类
     *
     * @param name 类名或全限定类名
     * @return
     */
    protected Class findClassInternal(String name) throws ClassNotFoundException {
        if (!validate(name))
            throw new ClassNotFoundException(name);

        String tempPath = name.replace('.', '/');
        String classPath = tempPath + ".class";

        ResourceEntry entry = findResourceInternal(name, classPath);

        if (entry == null || entry.binaryContent == null)
            throw new ClassNotFoundException(name);

        Class clazz = entry.loadedClass;
        if (clazz != null)
            return clazz;

        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1)
            packageName = name.substring(0, pos);

        // 取得类所在的包
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
            synchronized (this) { // 同类型但不同实例的加载器可加载相同的两个类（类的相等是指类加载器实例相同然后再是被加载的类相同）
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
                ;
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
                    n = binaryStream.read(binaryContent, pos, contentLength - pos);
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
        
        // 找到并解析了这个资源，现在把解析好的资源对象放到集合中然后返回
        resourceEntries.putIfAbsent(name, entry);
        return resourceEntries.get(name);
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


    /**
     * 返回所有仓库
     * 
     * @return
     */
    @Override
    public String[] findRepositories() {
        return repositories;
    }


    /**
     * TODO 是否有文件修改了
     * XXX 此方法有可能会因为paths和lastModifiedDates的长度不一致引发重载失败或者数组下标异常问题
     * 
     * @return
     */
    @Override
    public boolean modified() {
        if (debug >= 3)
            log("WebappClassLoader.modified  开始检查文件是否被修改或删除");
        
        // 检查普通文件目录
        for (int i = 0; i < paths.size(); i++) {
            try {
                long lastModified = ((ResourceAttributes) resources.getAttributes(paths.get(i))).getLastModified();
                if (lastModified != lastModifiedDates.get(i)) {
                    log("有文件被修改了！ " + paths.get(i));
                    return true;
                }
                
            } catch (NamingException e) {
                log("资源["+ paths.get(i) +"]未找到");
                return true;
            }
        }

        // 检查Jar包
        if (getJarPath() != null) {
            try {
                NamingEnumeration<Binding> it = resources.listBindings(getJarPath());
                int i = 0;
                while (it.hasMore() && i < jarNames.size()) {
                    NameClassPair ncPair = it.next();
                    String name = ncPair.getName();
                    if (!name.endsWith(".jar"))
                        continue;
                    if (!name.equals(jarNames.get(i))) {
                        log("没有名为[ "+ name +" ]的JAR包");
                        return true;
                    }
                    i++;
                }

                // 新添加了JAR包
                if (i < jarNames.size()) {
                    log("新添加了JAR包");
                    return true;
                }

                // 还有JAR包，但是没有载入到WebappClassLoader的jarNames中
                while (it.hasMore()) {
                    NameClassPair ncPair = it.next();
                    String name = ncPair.getName();
                    if (name.endsWith(".jar")) {
                        log("新添加了JAR包");
                        return true;
                    }
                }
            } catch (NamingException e) {
                if (debug >= 2)
                    log("Jar路径[ " + getJarPath() + " ]不存在");
            }
        }        

        return false; 
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
    public void start() throws LifecycleException {
        started = true;
    }

    
    /**
     * 释放资源
     *
     * @throws Exception
     */
    @Override
    public void stop() throws LifecycleException {
        started = false;

        for (JarFile jf : jarFiles) {
            try {
                jf.close();
            } catch (IOException e) {
                log("WebappClassLoader.closeJarFile", e);
            }
        }

        repositories = new String[0];
        jarPath = null;
        files = new File[0];
        jarFiles.clear();
        jarRealFiles.clear();
        jarNames.clear();
        paths.clear();
        lastModifiedDates.clear();
        hasExternalRepositories = false;

        super.recycle();
    }


    /**
     * 打印到控制台
     * 
     * @param message
     */
    private void log(String message) {
        System.out.println("WebappClassLoader: " + message);
    }

    /**
     * 打印到控制台
     *
     * @param message
     * @param t
     */
    private void log(String message, Throwable t) {
        System.out.println("WebappClassLoader: " + message);
        t.printStackTrace(System.out);
    }
}
