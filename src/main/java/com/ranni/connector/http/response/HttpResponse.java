package com.ranni.connector.http.response;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Collection;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:26
 */
public interface HttpResponse extends Response {
    // 返回cookies
    Collection<Cookie> getCookies();

    // 返回请求头中指定name对应的values[0]
    String getHeader(String name);

    // 返回请求头中指定name对应的values
    Collection<String> getHeaderValues(String name);

    // 返回响应消息
    String getMessage();

    // 返回响应状态
    int getStatus();

    // 重置此响应并指定响应状态和消息
    void reset(int status, String message);

    // 发送静态资源
    void sendStaticResource() throws IOException;
}
