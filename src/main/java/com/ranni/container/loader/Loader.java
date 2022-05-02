package com.ranni.container.loader;

import com.ranni.container.Container;

import java.beans.PropertyChangeListener;

/**
 * Title: HttpServer
 * Description:
 * 类加载器的再包装接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 20:48
 */
public interface Loader {

    /**
     * 返回类加载器
     *
     * @return
     */
    ClassLoader getClassLoader();


    /**
     * 返回与此Loader关联的Container
     *
     * @return
     */
    Container getContainer();


    /**
     * 设置容器
     *
     * @param container
     */
    void setContainer(Container container);

    // 返回默认的Context
//    DefaultContext getDefaultContext();

    // 设置默认的Context
//    void setDefaultContext(DefaultContext defaultContext);

    /**
     * 是否委托给父类加载器
     *
     * @return 返回委托标志
     */
    boolean getDelegate();


    /**
     * 是否委托给父类加载器
     *
     * @param delegate 委托标志
     */
    void setDelegate(boolean delegate);


    /**
     * 返回Loader的描述信息和版本号
     *
     * @return
     */
    String getInfo();
    

    /**
     * 添加属性更改监听器
     *
     * @param listener
     */
    void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 将此库添加到库集合中
     *
     * @param repository
     */
    void addRepository(String repository);


    /**
     * 返回所有的库
     *
     * @return
     */
    String[] findRepositories();


    /**
     * 是否修改了servlet类，用于重新加载类
     *
     * @return
     */
    boolean modified();


    /**
     * 移除属性更改监听器
     *
     * @param listener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);
}
