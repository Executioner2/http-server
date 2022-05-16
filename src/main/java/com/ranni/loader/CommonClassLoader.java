package com.ranni.loader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Title: HttpServer
 * Description:
 * 通用类加载，webapp和server都可以使用的类加载器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/15 22:21
 */
public class CommonClassLoader extends URLClassLoader {
    private ClassLoader parent; // 父加载器
    private ClassLoader system; // 系统类加载器
    
    public CommonClassLoader() {
        this(new URL[0]);
    }
    
    public CommonClassLoader(URL[] urls) {
        super(urls);
        this.parent = getParent();
        this.system = getSystemClassLoader();
    }
    
    public CommonClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public CommonClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.parent = getParent();
        this.system = getSystemClassLoader();
    }


    /**
     * 加载类
     * 
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }


    /**
     * 加载类
     * 
     * @param name
     * @param resolve
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        
        return super.loadClass(name, resolve);
    }
}
