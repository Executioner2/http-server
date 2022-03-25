package com.ranni.connector.http.response;

/**
 * Title: HttpServer
 * Description: TODO HttpResponseImpl 未实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:28
 */
public final class HttpResponseImpl extends HttpResponseBase {
    protected boolean allowChunking; // 是否允许分块
    protected HttpResponseStream responseStream; // 响应流
}
