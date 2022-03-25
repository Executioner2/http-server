package com.ranni.connector.http.request;

import com.ranni.connector.Connector;
import com.ranni.connector.Context;
import com.ranni.container.Wrapper;
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

    // 获取此请求中的授权认证
    String getAuthorization();

    // 将此请求的授权认证设置到Request对象中
    void setAuthorization(String authorization);

    // 返回此请求的连接器
    Connector getConnector();

    // 设置此请求的连接器
    void setConnector(Connector connector);

    // 返回此请求的上下文
    Context getContext();

    // 设置此请求的上下文
    void setContext(Context context);

    // 返回此请求实现的描述信息和版本信息
    String getInfo();

    // 返回请求对象，不存在就创建
    // 实际上创建了RequestBase对象
    ServletRequest getRequest();


    // 返回响应对象，不存在就创建
    Response getResponse();

    // 设置响应对象
    void setResponse(Response response);

    // 返回此条http连接的socket对象
    Socket getSocket();

    // 设置此条http连接的socket对象
    void setSocket(Socket socket);

    // 返回此socket通信中的InputStream
    InputStream getStream();

    // 设置此请求的InputStream
    void setStream(InputStream input);

    // 设置远程主机完全限定名
    void setRemoteHost(String host);

    // 返回Container的包装器
    Wrapper getWrapper();

    // 设置包装器
    void setWrapper(Wrapper wrapper);

    // 创建并返回ServletInputStream
    ServletInputStream createInputStream() throws IOException;

    // 完成请求读取，释放资源
    void finishRequest() throws IOException;

    // 释放一些引用，为重用此对象做准备
    void recycle();

    // 设置请求体长度
    void setContentLength(int length);

    // 设置请求体类型
    void setContentType(String type);

    // 设置此请求的协议和版本
    // 如 HTTP/0.9 或 HTTP/1.0 又或者 HTTP/1.1
    void setProtocol(String protocol);

    // 设置远程地址
    void setRemoteAddr(String remote);

    // 设置协议类型
    // 如 http 或 https 又或者 ftp
    void setScheme(String scheme);

    // 设置此请求是否安全
    void setSecure(boolean secure);

    // 设置服务器名称
    void setServerName(String name);

    // 设置服务器端口号
    void setServerPort(int port);
}
