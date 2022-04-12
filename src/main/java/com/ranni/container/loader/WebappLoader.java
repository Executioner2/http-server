package com.ranni.container.loader;

import com.ranni.common.Globals;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.exception.LifecycleException;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.resource.DirContextURLStreamHandlerFactory;
import com.ranni.resource.Resource;
import com.ranni.util.LifecycleSupport;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.jar.JarFile;

/**
 * Title: HttpServer
 * Description:
 * 加载器，负责Webapp的加载，与Context容器关联
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 15:45
 */
public class WebappLoader implements Loader, Runnable, Lifecycle {
    private String loaderClass = "com.ranni.container.loader.WebappClassLoader"; // 加载器的全限定类名，默认为com.ranni.container.loader.WebappClassLoader
    private ClassLoader parentLoader; // 类载入器（父）
    private WebappClassLoader classLoader; // 类载入器
    private boolean started; // 启动标志位
    private Container container; // 与此加载器关联的容器
    private boolean delegate; // 委托标志位
    private boolean reloadable; // 重载标志位
    private String[] repositories = new String[0]; // 仓库
    private LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理实例

    public WebappLoader() {
    }

    public WebappLoader(ClassLoader parentLoader) {
        this.parentLoader = parentLoader;
    }

    /**
     * 返回加载器全限定类名
     *
     * @return
     */
    public String getLoaderClass() {
        return this.loaderClass;
    }

    /**
     * 设置加载器全限定类名
     * 支持用户自定义的WebappClassLoader子类
     * 若传入null则直接返回
     *
     * @param loaderClass
     */
    public void setLoaderClass(String loaderClass) {
        if (loaderClass == null) return;

        this.loaderClass = loaderClass;
    }

    /**
     * 返回此容器的类载入器
     *
     * @return
     */
    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * 返回与此加载器关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此加载器关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * 是否委托给父类加载器
     *
     * @return 返回委托标志
     */
    @Override
    public boolean getDelegate() {
        return this.delegate;
    }

    /**
     * 是否委托给父类加载器
     *
     * @param delegate 委托标志
     */
    @Override
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 返回是否可重新载入标志
     *
     * @return
     */
    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }

    /**
     * 设置重新载入标志
     *
     * @param reloadable
     */
    @Override
    public void setReloadable(boolean reloadable) {
        this.delegate = delegate;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * 添加仓库
     * 此方法的调用是在一个线程中进行的
     * 所以无需使用synchronized
     *
     * @param repository
     */
    @Override
    public void addRepository(String repository) {
        // 查询是否是重复添加的
        for (int i = 0; i < repositories.length; i++) {
            if (repositories[i].equals(repository))
                return;
        }

        String[] newRepositories = new String[repositories.length + 1];
        System.arraycopy(repositories, 0, newRepositories, 0, repositories.length);
        newRepositories[repositories.length] = repository;
        repositories = newRepositories;
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

    @Override
    public boolean modified() {
        return false;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void run() {

    }

    /**
     * 添加监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * 返回所有监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()}
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * 移除指定监听器
     *
     * @see {@link LifecycleSupport#removeLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * 1、创建一个类载入器
     * 2、设置仓库
     * 3、设置类路径
     * 4、设置访问权限
     * 5、启动一个新的线程来支持自动重载
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        if (started) throw new LifecycleException("此WebappLoader实例已经启动！");

        log("载入器启动！");
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;

        // 取得要载入的资源（通过资源文件取得servlet类的路径）
        if (container.getResources() == null)
            return;

        // 为JNDI协议注册流处理工厂
        URLStreamHandlerFactory streamHandlerFactory = new DirContextURLStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);

        // 创建类载入器
        classLoader = createClassLoader();
        classLoader.setResources(container.getResources());
        classLoader.setDelegate(this.delegate); // 设置委托标志
        // 导入存储库
        for (int i = 0; i < repositories.length; i++) {
            classLoader.addRepository(repositories[i]);
        }


        // 设置仓库
        setRepositories();

        // 设置类路径

        // 设置访问权限

        // 启动一个新的线程来支持自动重载
    }

    /**
     * 设置仓库，主要完成下面两个操作
     * WEB应用class文件存放的位置
     * WEB应用使用的JAR包
     */
    private void setRepositories() {
        if (!(container instanceof Context))
            return;

        // 取得全局作用域，如果没有就直接返回
        ServletContext servletContext = ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        // 取得工作目录
        File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir == null)
            return;

        log("取得webappLoader工作目录：" + workDir.getAbsolutePath());

        // 取得资源文件
        DirContext resources = container.getResources();

        // 导入WEB应用程序的class文件
        String classesPath = "/WEB-INF/classes"; // 类的路径
        DirContext classes = null;

        try {
            Object o = resources.lookup(classesPath);// 解析这个路径下所有的类
            if (o instanceof DirContext)
                classes = (DirContext) o;
        } catch (NamingException e) {
            e.printStackTrace();
        }

        if (classes != null) {
            File classRepository = null; // 类仓库
            String realPath = servletContext.getRealPath(classesPath);
            if (realPath != null) {
                // 如果能在servlet的全局作用域中找到这个路径则表示这个路径的class已经导入了
                classRepository = new File(realPath);
            } else {
                // 在当前工作目录下创建类仓库文件夹
                classRepository = new File(workDir, classesPath);
                classRepository.mkdirs();
                copyDir(classes, classRepository);
            }

            log("WebappLoader 类部署  classPath: " + classesPath + "   classRepositoryAbPath: " + classRepository.getAbsolutePath());

            classLoader.addRepository(classesPath + "/", classRepository);
        }


        // 导入WEB应用使用的JAR包，需要将JAR包里的类拷贝出来
        String libPath = "/WEB-INF/lib";
        classLoader.setJarPath(libPath);

        DirContext libDir = null;
        try {
            Object object = resources.lookup(libPath);
            if (object instanceof DirContext)
                libDir = (DirContext) object;
        } catch (NamingException e) {

        }

        if (libDir != null) {
            boolean copyJars = false; // 是否需要拷贝JAR包中的文件
            String realPath = servletContext.getRealPath(libPath);
            File destDir = null;

            if (realPath != null) {
                // 如果能在servlet的全局作用域中找到这个路径则表示这个路径的JAR已经导入了
                destDir = new File(realPath);
            } else {
                destDir = new File(workDir, libPath);
                destDir.mkdirs();
                copyJars = true;
            }

            try {
                NamingEnumeration<Binding> it = resources.listBindings(libPath);
                while (it.hasMoreElements()) {
                    Binding binding = it.nextElement();
                    String filename = libPath + "/" + binding.getName();
                    if (!filename.endsWith(".jar"))
                        continue;

                    File destFile = new File(destDir, binding.getName());
                    log("WebappLoader JAR部署  filename: " + filename + "  path: " + destFile.getAbsolutePath());

                    Resource jarResource = (Resource) binding.getObject();
                    if (copyJars) {
                        OutputStream os = new FileOutputStream(destFile);
                        if (!copy(jarResource.streamContent(), os))
                            continue;
                    }

                    JarFile jarFile = new JarFile(destFile);
                    classLoader.addJar(filename, jarFile, destFile);
                }
            } catch (NamingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 将srcDir中的类文件拷贝到desDir对象的目录下
     *
     * @param srcDir
     * @param destDir
     */
    private boolean copyDir(DirContext srcDir, File destDir) {
        try {
            NamingEnumeration<NameClassPair> it = srcDir.list("");
            while (it.hasMoreElements()) {
                NameClassPair ncPair = it.nextElement();
                String name = ncPair.getName();
                Object object = srcDir.lookup(name);
                File currentFile = new File(destDir, name);

                if (object instanceof Resource) {
                    InputStream is = ((Resource) object).streamContent();
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy(is, os))
                        return false;
                } else if (object instanceof InputStream) {
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy((InputStream) object, os))
                        return false;
                } else if (object instanceof DirContext) {
                    // 当前名称对应的是个文件夹
                    // 递归复制里面的文件
                    currentFile.mkdir();
                    copyDir((DirContext) object, currentFile);
                }
            }
        } catch (NamingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }


    /**
     * 把输入流的内容从输出流输出出去
     *
     * @param is
     * @param os
     * @return
     */
    private boolean copy(InputStream is, OutputStream os) {

        try {
            byte[] buffer = new byte[4096];
            int len = -1;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * 停止掉此加载器
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        if (started) throw new LifecycleException("此WebappLoader实例已经停止！");
    }

    /**
     * 记录到日志文件
     *
     * @param msg
     */
    private void log(String msg) {
        Logger logger = null;
        String containerName = "";

        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }

        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "]: " + msg);
        } else {
            System.out.println("WebappLoader[" + containerName + "]: " + msg);
        }
    }

    /**
     * 记录到日志文件
     *
     * @param msg
     * @param throwable
     */
    private void log(String msg, Throwable throwable) {
        Logger logger = null;
        String containerName = "";

        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }

        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "]: " + msg, throwable);
        } else {
            System.out.println("WebappLoader[" + containerName + "]: " + msg);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }

    }

    /**
     * 创建类载入器
     * 可以通过私有变量loaderClass创建用户继承了WebappClassLoader的自定义类加载器
     *
     * @return
     *
     * @throws Exception
     */
    private WebappClassLoader createClassLoader() throws Exception {
        Class clazz = Class.forName(getLoaderClass());
        WebappClassLoader classLoader = null;

        if (parentLoader == null) {
            // 父加载器为空，创建当前classLoader的实例
            classLoader = (WebappClassLoader) clazz.getConstructor().newInstance();
        } else {
            // 实例化时设置classLoader的父加载器
            Constructor constructor = clazz.getConstructor(ClassLoader.class);
            classLoader = (WebappClassLoader) constructor.newInstance(parentLoader);
        }

        return classLoader;
    }

}
