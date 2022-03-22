package com.ranni.connector.http.request;

import com.ranni.connector.http.Connector;
import com.ranni.connector.http.Context;
import com.ranni.connector.http.Wrapper;
import com.ranni.connector.http.response.Response;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:04
 */
public interface Request {

    /**
     * 获取此请求中的授权认证
     * @return
     */
    String getAuthorization();

    /**
     * 将此请求的授权认证设置到Request对象中
     * @param authorization
     */
    void setAuthorization(String authorization);

    /**
     * 返回此请求的连接器
     * @return
     */
    Connector getConnector();

    /**
     * 设置此请求的连接器
     * @param connector
     */
    void setConnector(Connector connector);

    /**
     * 返回此请求的上下文
     * @return
     */
    Context getContext();

    /**
     * 设置此请求的上下文
     * @param context
     */
    void setContext(Context context);

    /**
     * 返回请求实现的描述信息
     * @return
     */
    String getInfo();

    /**
     * 返回请求对象，不存在就创建
     * 实际上创建了RequestBase对象
     * @exception ServletRequest
     * @return
     */
    ServletRequest getRequest();


    /**
     * 返回响应对象，不存在就创建
     * @exception Response
     * @return
     */
    Response getResponse();

    /**
     * 设置响应对象
     * @param response
     */
    void setResponse(Response response);

    /**
     * 返回此条http连接的socket对象
     * @return
     */
    Socket getSocket();

    /**
     * 设置此条http连接的socket对象
     * @param socket
     */
    void setSocket(Socket socket);

    /**
     * 返回此socket通信中的InputStream
     * @return
     */
    InputStream getStream();

    /**
     * 设置此请求的InputStream
     * @param input
     */
    void setStream(InputStream input);

    /**
     * 设置远程主机完全限定名
     * @param remoteHost
     */
    void setRemoteHost(String host);

    /**
     * 返回Container的包装器
     * @exception Wrapper
     * @return
     */
    Wrapper getWrapper();

    /**
     * 设置包装器
     * @exception Wrapper
     * @param wrapper
     */
    void setWrapper(Wrapper wrapper);

    /**
     * 创建并返回ServletInputStream
     * @exception ServletInputStream
     * @return
     * @throws IOException
     */
    ServletInputStream createInputStream() throws IOException;

    /**
     * 完成请求读取
     * 关闭其它资源和逻辑上关闭（实际关闭会关闭Socket，所以逻辑关闭）InputStream
     * @throws IOException
     */
    void finishRequest() throws IOException;

    /**
     * 释放一些引用，为重用此对象做准备
     */
    void recycle();

    /**
     * 设置请求体长度
     * @param length
     */
    void setContentLength(int length);

    /**
     * 设置请求体类型
     * @param type
     */
    void setContentType(String type);

    /**
     * 设置此请求的协议和版本
     * 如 HTTP/0.9 或 HTTP/1.0 又或者 HTTP/1.1
     * @param protocol
     */
    void setProtocol(String protocol);

    /**
     * 设置远程地址
     * @param remote
     */
    void setRemoteAddr(String remote);

    /**
     * 设置协议类型
     * 如 http 或 https 又或者 ftp
     * @param scheme
     */
    void setScheme(String scheme);

    /**
     * 设置此请求是否安全
     * @param secure
     */
    void setSecure(boolean secure);

    /**
     * 设置服务器名称
     * @param name
     */
    void setServerName(String name);

    /**
     * 设置服务器端口号
     * @param port
     */
    void setServerPort(int port);
}
