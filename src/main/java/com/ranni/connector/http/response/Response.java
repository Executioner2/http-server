package com.ranni.connector.http.response;

import com.ranni.container.Context;
import com.ranni.connector.http.request.Request;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:25
 */
public interface Response {
    // 返回连接器
    Connector getConnector();

    // 设置连接器
    void setConnector(Connector connector);

    // 返回实际写入输出流的字节数
    int getContentCount();

    // 返回Context
    Context getContext();

    // 设置Context
    void setContext(Context context);

    // 设置响应提交标志
    void setAppCommitted(boolean appCommitted);

    // 返回响应是否提交
    boolean isAppCommitted();

    // TODO 不太清楚啥作用
    boolean getIncluded();

    void setIncluded(boolean included);

    // 返回与此实现的描述信息与版本号
    String getInfo();

    // 返回此响应相关连的请求
    Request getRequest();

    // 设置与此响应相关联的请求
    void setRequest(Request request);

    // 返回响应
    ServletResponse getResponse();

    // 返回标准输出流
    OutputStream getStream();

    // 设置标准输出流
    void setStream(OutputStream stream);

    // 设置挂起标志
    void setSuspended(boolean suspended);

    // 返回挂起标志
    boolean isSuspended();

    // 设置错误标志
    void setError();

    // 返回错误标志
    boolean isError();

    // 创建ServletOutputStream
    ServletOutputStream createOutputStream() throws IOException;

    // 完成响应，关闭资源
    void finishResponse() throws IOException;

    // 返回此响应设置或计算的长度
    int getContentLength();

    // 返回此响应体的类型
    String getContentType();

    // 返回输出处理流对象PrintWriter
    PrintWriter getReporter();

    // 初始化部分值，便于二次使用
    void recycle();

    // 重置缓冲区
    void resetBuffer();

    // 发送响应确认
    void sendAcknowledgement() throws IOException;
}
