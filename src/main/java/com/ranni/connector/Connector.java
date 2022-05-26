package com.ranni.connector;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.connector.socket.ServerSocketFactory;
import com.ranni.container.Container;
import com.ranni.core.Service;
import com.ranni.lifecycle.LifecycleException;

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
    /**
     * 取得一个容器来处理请求
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

    
    /**
     * 返回dns查询标志
     * 
     * @return
     */
    boolean getEnableLookups();

    
    /**
     * 设置dns查询标志
     * 
     * @param enableLookups
     */
    void setEnableLookups(boolean enableLookups);

    
    /**
     * 返回server socket工厂
     * 
     * @return
     */
    ServerSocketFactory getFactory();

    
    /**
     * 设置server socket工厂
     * 
     * @param factory
     */
    void setFactory(ServerSocketFactory factory);

    
    /**
     * 返回此Connector实现类的信息和版本
     * 
     * @return
     */
    String getInfo();


    /**
     * 返回重定向端口
     * 
     * @return
     */
    int getRedirectPort();


    /**
     * 设置重定向端口
     * 
     * @param redirectPort
     */
    void setRedirectPort(int redirectPort);


    /**
     * 取得协议类型
     * 
     * @return
     */
    String getScheme();


    /**
     * 设置协议类型
     * 
     * @param scheme
     */
    void setScheme(String scheme);

    
    /**
     * 返回安全标志，默认为false
     * 
     * @return
     */
    boolean getSecure();

    
    /**
     * 设置安全标志
     * 
     * @param secure
     */ 
    void setSecure(boolean secure);


    /**
     * 返回服务对象
     * 
     * @return
     */
    Service getService();


    /**
     * 设置服务对象
     * 
     * @param service
     */
    void setService(Service service);

    
    /**
     * 创建请求对象
     * 
     * @return
     */
    Request createRequest();


    /**
     * 创建响应对象
     * 
     * @return
     */
    Response createResponse();
        
    
    /**
     * 初始化连接器
     * 
     * @throws LifecycleException
     */
    void initialize() throws LifecycleException;


    /**
     * 设置日志输出级别
     * 
     * @param debug
     */
    void setDebug(int debug);


    /**
     * @return 是否重置外观对象
     */
    boolean getDiscardFacades();


    /**
     * @return 返回Post请求包的最大大小
     */
    int getMaxPostSize();


    /**
     * @return 返回容器自动解析的参数数量，为0表示无限制
     */
    int getMaxParameterCount();
}
