package com.ranni.connector;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.connector.socket.ServerSocketFactory;
import com.ranni.container.Container;

/**
 * Title: HttpServer
 * Description:
 * 连接器标准接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 19:17
 */
public interface Connector {
    // 取得一个容器来处理请求
    Container getContainer();

    // 设置容器
    void setContainer(Container container);

    // 返回dns查询标志
    boolean getEnableLookups();

    // 设置dns查询标志
    void setEnableLookups(boolean enableLookups);

    // 返回server socket工厂
    ServerSocketFactory getFactory();

    // 设置server socket工厂
    void setFactory(ServerSocketFactory factory);

    // 返回此Connector实现类的信息和版本
    String getInfo();

    // 返回重定向端口
    int getRedirectPort();

    // 设置重定向端口
    void setRedirectPort(int redirectPort);

    // 取得协议类型
    String getScheme();

    // 设置协议类型
    void setScheme(String scheme);

    // 返回安全标志，默认为false
    boolean getSecure();

    // 设置安全标志
    void setSecure(boolean secure);


    // 返回服务对象
//    public Service getService();


    // 设置服务对象
//    void setService(Service service);

    // 创建请求对象
    Request createRequest();


    // 创建响应对象
    Response createResponse();

    // 初始化连接器
    void initialize() throws RuntimeException;
}
