package com.ranni.container.loader;

import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.resource.ResourceAttributes;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    protected DirContext resources; // 容器资源
    protected boolean delegate; // 委托标志
    protected boolean hasExternalRepositories; // 是否有拓展仓库集
    protected boolean started;
    protected String[] repositories = new String[0]; // 类仓库名，与类文件仓库的下标是对应的
    protected File[] files = new File[0]; // 类文件仓库，与类仓库名的的下标是对应的
    protected String jarPath; // JAR包的路径
    protected String[] jarNames = new String[0]; // JAR包名集合
    protected JarFile[] jarFiles = new JarFile[0]; // JAR包列表
    protected File[] jarRealFiles = new File[0]; // 存放JAR包中资源的文件夹集合
    protected long[] lastModifiedDates = new long[0]; // JAR的最后修改日期
    protected String[] paths = new String[0]; // JAR包路径列表
    protected Set<String> notFoundResources = new HashSet<>(); // 缓存未找到的资源名
    protected Map<String, ResourceEntry> resourceEntries = new HashMap<>(); // 已经载入的缓存资源


    public WebappClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
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
     * 批量添加文件到仓库
     *
     * @param repository
     * @param file
     */
    public synchronized void addRepository(String repository, File file) {
        if (repository == null)
            return;

        // XXX 这里将类仓库名和类文件仓库的拷贝当作了一个整体进行，如果抛出下标异常优先检查这里

        String[] res1 = new String[repositories.length + 1];
        File[] res2 = new File[this.files.length + 1];
        int i = 0;

        for (; i < res1.length; i++) {
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

            String[] res1 = new String[jarNames.length + 1];
            System.arraycopy(jarNames, 0, res1, 0, jarNames.length);
            res1[jarNames.length] = jarName;
            jarNames = res1;
        }

        // 此JAR包最后修改时间
        try {
            long lastModified = ((ResourceAttributes) resources.getAttributes(path)).getLastModified();

            String[] res2 = new String[paths.length + 1];
            System.arraycopy(paths, 0, res2, 0, paths.length);
            res2[paths.length] = path;
            paths = res2;

            long[] res3 = new long[lastModifiedDates.length + 1];
            System.arraycopy(lastModifiedDates, 0, res3, 0, lastModifiedDates.length);
            res3[lastModifiedDates.length] = lastModified;
            lastModifiedDates = res3;

        } catch (NamingException e) {
            ;
        }

        // TODO 检查JAR包中有无无效类
//        if (!validateJarFile(file)) return;

        JarFile[] res4 = new JarFile[this.jarFiles.length + 1];
        System.arraycopy(jarFiles, 0, res4, 0, jarFiles.length);
        res4[jarFiles.length] = jarFile;
        jarFiles = res4;

        File[] res5 = new File[this.jarRealFiles.length + 1];
        System.arraycopy(jarRealFiles, 0, res5, 0, jarRealFiles.length);
        res5[jarRealFiles.length] = file;
        jarRealFiles = res5;

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
        files = new File[0];
        jarFiles = new JarFile[0];
        jarRealFiles = new File[0];
        jarPath = null;
        jarNames = new String[0];
        lastModifiedDates = new long[0];
        paths = new String[0];
        hasExternalRepositories = false;
    }
}
