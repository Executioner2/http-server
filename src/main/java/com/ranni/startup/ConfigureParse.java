package com.ranni.startup;

import com.ranni.deploy.ConfigureMap;

import java.io.File;
import java.io.InputStream;

/**
 * Title: HttpServer
 * Description:
 * 配置文件解析
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 20:41
 */
public interface ConfigureParse<T, E> {

    /**
     * 解析
     *
     * @param file
     * @return
     */
    ConfigureMap<T, E> parse(File file) throws Exception;


    /**
     * 解析
     *
     * @param input
     * @return
     */
    ConfigureMap<T, E> parse(InputStream input) throws Exception;
    
    
    /**
     * 解析
     *
     * @param file
     * @param autoFit 自动填充
     * @return
     */
    ConfigureMap<T, E> parse(File file, boolean autoFit) throws Exception;


    /**
     * 解析
     *
     * @param input
     * @param autoFit 自动填充
     * @return
     */
    ConfigureMap<T, E> parse(InputStream input, boolean autoFit) throws Exception;


    /**
     * 设置要被解析为什么类
     * 
     * @param clazz
     */
    void setClazz(Class clazz);


    /**
     * 返回要被解析的类
     * 
     * @return
     */
    Class getClazz();


    /**
     * 装配。将配置文件中的值填充到指
     * 定的实例中
     * 
     * @param configure 配置信息
     * @return 返回指定类型实现类
     */
    T fit(E configure) throws Exception;
}
