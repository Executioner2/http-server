package com.ranni.connector.http.response;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-24 16:02
 */
public final class HttpResponseStream extends ResponseStream {
    public HttpResponseStream(Response response) {
        super(response);
    }

    public HttpResponseStream(Response response, int length) {
        super(response, length);
    }
}
