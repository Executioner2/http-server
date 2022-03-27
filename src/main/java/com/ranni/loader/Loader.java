package com.ranni.loader;

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

    // 返回类加载器
    ClassLoader getClassLoader();

    // 返回与此Loader关联的Container
    Container getContainer();

    // 设置容器
    void setContainer(Container container);

    // 返回默认的Context
//    DefaultContext getDefaultContext();

    // 设置默认的Context
//    void setDefaultContext(DefaultContext defaultContext);

    // 返回委托标志
    boolean getDelegate();

    // 设置委托标志
    void setDelegate(boolean delegate);

    // 返回Loader的描述信息和版本号
    String getInfo();

    // 返回是否可重新载入标志
    boolean getReloadable();

    // 设置重新载入标志
    void setReloadable(boolean reloadable);

    // 添加属性更改监听器
    void addPropertyChangeListener(PropertyChangeListener listener);

    // 将此存储库添加到存储库集合中
    void addRepository(String repository);

    // 返回存储库集合
    String[] findRepositories();

    // 是否修改内部存储库，用于重新加载类
    boolean modified();

    // 移除属性更改监听器
    void removePropertyChangeListener(PropertyChangeListener listener);
}
