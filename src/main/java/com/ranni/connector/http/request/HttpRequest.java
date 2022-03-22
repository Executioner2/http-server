package com.ranni.connector.http.request;

import javax.servlet.http.Cookie;
import java.security.Principal;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:05
 */
public interface HttpRequest extends Request {
    // 添加cookie
    void addCookie(Cookie cookie);

    // 添加header中的参数和value
    void addHeader(String name, String value);

    // 添加语言环境
    void addLocale(Locale locale);

    // 添加参数
    void addParameter(String name, String values[]);

    // 清空cookie
    void clearCookies();

    // 清空header中的参数
    void clearHeaders();

    // 清空语言环境
    void clearLocales();

    // 清空参数
    void clearParameters();

    // 设置认证类型，"DIGEST" 或 "SSL".
    void setAuthType(String type);

    // 设置请求的上下文路径
    void setContextPath(String path);

    // 设置请求方法
    void setMethod(String method);

    // 设置例如GET请求存在与URI中的查询字符串
    void setQueryString(String query);

    // 设置路径信息
    void setPathInfo(String path);

    // session是否存在与cookie中
    void setRequestedSessionCookie(boolean flag);

    // 设置session id
    void setRequestedSessionId(String id);

    // session是否存在于url中
    void setRequestedSessionURL(boolean flag);

    // 设置请求的uri
    void setRequestURI(String uri);

    // 设置解码后的uri
    void setDecodedRequestURI(String uri);

    // 获取解码后的uri
    String getDecodedRequestURI();

    // 设置servlet的路径
    void setServletPath(String path);

    // 设置已对此请求进行验证的Principal
    void setUserPrincipal(Principal principal);
}
