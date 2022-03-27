package com.ranni.container;

import com.ranni.handler.Loader;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description: 将连接器和某个servlet容器相关联
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:51
 */
public interface Container {
    // 下面的final static常量用于相关的事件
    String ADD_CHIlD_EVENT = "addChild"; // 添加子容器
    String ADD_MAPPER_EVENT = "addMapper"; // 添加mapper
    String ADD_VALVE_EVENT = "addValve"; // 添加valve
    String REMOVE_MAPPER_EVENT = "removeMapper"; // 移除mapper
    String REMOVE_VALVE_EVENT = "removeValve"; // 移除valve

    // 返回此Container的描述信息和版本信息
    String getInfo();

    // 返回与此Container关联的载入器，如果没有就返回父Container的，都没有就返回null
    Loader getLoader();

    // 设置此Container的载入器
    void setLoader(Loader loader);

    // 返回记录器，此Container没有就返回父Container的，都没有就返回null
//    Logger getLogger();

    // 设置记录器
//    void setLogger(Logger logger);

    // 返回管理器，此Container没有就返回父Container的，都没有就返回null
//    Manager getManager();

    // 设置管理器
//    void setManager(Manager manager);

    // 返回Cluster，如果此Container没有就返回父Container的，都没有就返回null
//    Cluster getCluster();

    // 设置Cluster
//    void setCluster(Cluster cluster);

    // 返回此Container的名字（方便人用），该名字必须唯一
    String getName();

    // 设置此Container的名字
    void setName(String name);

    // 返回父Container，没有就返回null
    Container getParent();

    // 设置父container
    void setParent(Container container);

    // 返回web应用程序的父类加载器
    ClassLoader getParentClassLoader();

    // 设置web应用程序的父类加载器
    void setParentClassLoader(ClassLoader parent);

    // 返回领域，如果没有就返回父Container的，都没有就返回null
//    Realm getRealm();

    // 设置领域
//    void setRealm(Realm realm);

    // 返回资源，如果没有就返回父Container的，都没有就返回null
    DirContext getResources();

    // 设置resource
    void setResources(DirContext resources);

    // 给此Container添加孩子
    void addChild(Container child);

    // 给此容器添加容器事件监听器
    void addContainerListener(ContainerListener listener);

    // 添加与此Container关联的mapper
//    void addMapper(Mapper mapper);

    // 添加属性改变监听器
    void addPropertyChangeListener(PropertyChangeListener listener);

    // 根据name查询子容器，没有就返回null
    Container findChild(String name);

    // 返回所有子容器
    Container[] findChildren();

    // 返回此容器的所有容器事件监听器
    ContainerListener[] findContainerListeners();

    // 根据协议类型找出与此容器关联的mapper
//    Mapper findMapper(String protocol);

    // 返回此容器所有mapper
//    Mapper[] findMappers();

    // 执行servlet的service方法
    void invoke(Request request, Response response) throws IOException, ServletException;

    // XXX 取得处理这种请求的容器，这个方法在Tomcat5中被弃用
    Container map(Request request, boolean update);

    // 删除子容器
    void removeChild(Container child);

    // 删除容器事件监听器
    void removeContainerListener(ContainerListener listener);

    // 删除mapper
//    void removeMapper(Mapper mapper);

    // 删除属性改变事件监听器
    void removePropertyChangeListener(PropertyChangeListener listener);
}
