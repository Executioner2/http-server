package com.ranni.naming;

import javax.naming.directory.DirContext;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 目录容器URL流处理器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 17:35
 */
@Deprecated // 用到了，但是可能会重新设计
public class DirContextURLStreamHandler extends URLStreamHandler {
    private static Map<ClassLoader, DirContext> clBindings = Collections.synchronizedMap(new HashMap<>()); // 绑定类加载器，key为线程的类加载器
    private static Map<Thread, DirContext> threadBindings = Collections.synchronizedMap(new HashMap<>()); // 与线程绑定的目录容器

    protected DirContext context; // 与此目录容器流处理器关联的目录容器

    public DirContextURLStreamHandler() {
    }
    
    public DirContextURLStreamHandler(DirContext context) {
        this.context = context;
    }


    /**
     * 创建一个目录容器URL连接
     *
     * @param u
     * @return
     * @throws IOException
     */
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        DirContext currentContext = this.context;
        if (currentContext == null)
            currentContext = get();

        return new DirContextURLConnection(currentContext, u);
    }


    /**
     * 获取绑定的容器
     *
     * @return
     */
    public static DirContext get() {
        DirContext res = null;

        Thread currentThread = Thread.currentThread();
        ClassLoader currentCL = currentThread.getContextClassLoader(); // 获取当前线程的类加载器

        res = clBindings.get(currentCL);
        if (res != null)
            return res;

        // 如果有父加载器就取父加载器对应的目录容器
        res = threadBindings.get(currentThread);
        currentCL = currentCL.getParent();
        while (currentCL != null) {
            res = clBindings.get(currentCL);
            if (res != null)
                return res;
            currentCL = currentCL.getParent();
        }

        if (res == null)
            throw new IllegalStateException("类加载器绑定异常！");

        return res;
    }


    /**
     * 当前线程是否绑定容器
     *
     * @return
     */
    public static boolean isBound() {
        return clBindings.containsKey(Thread.currentThread().getContextClassLoader())
                || threadBindings.containsKey(Thread.currentThread());
    }


    /**
     * 给当前线程的类加载器绑定容器
     *
     * @param dirContext
     */
    public static void bind(DirContext dirContext) {
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();

        if (currentCL != null)
            clBindings.put(currentCL, dirContext);
    }


    /**
     * 解绑当前线程类的加载器的容器
     */
    public static void unbind() {
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        if (currentCL != null)
            clBindings.remove(currentCL);
    }


    /**
     * 当前线程与传入的容器绑定
     *
     * @param dirContext 不允许空值
     */
    public static void bindThread(DirContext dirContext) {
        if (dirContext == null)
            throw new IllegalArgumentException("DirContext不能为空！");
        threadBindings.put(Thread.currentThread(), dirContext);
    }


    /**
     * 解绑当前线程的容器
     */
    public static void unbindThread() {
        threadBindings.remove(Thread.currentThread());
    }


    /**
     * 传入指定的类加载器和目录容器进行绑定
     *
     * @param cl 不允许空值
     * @param dirContext 不允许空值
     */
    public static void bind(ClassLoader cl, DirContext dirContext) {
        if (cl == null || dirContext == null)
            throw new IllegalArgumentException("ClassLoader和DirContext不能为空！");
        clBindings.put(cl, dirContext);
    }


    /**
     * 对指定的类加载器的目录容器进行解绑
     *
     * @param cl
     */
    public static void unbind(ClassLoader cl) {
        clBindings.remove(cl);
    }


    /**
     * 返回指定类加载对应的目录容器
     *
     * @param cl
     * @return
     */
    public static DirContext get(ClassLoader cl) {
        return clBindings.get(cl);
    }


    /**
     * 返回指定线程对应的目录容器
     *
     * @param thread
     * @return
     */
    public static DirContext get(Thread thread) {
        return threadBindings.get(thread);
    }
}
