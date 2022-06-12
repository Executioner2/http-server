package com.ranni.coyote.http11;

import com.ranni.util.net.Nio2Channel;
import com.ranni.util.net.Nio2Endpoint;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Title: HttpServer
 * Description:
 * HTTP/1.1协议的NIO2实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/11 16:01
 * @Ref org.apache.coyote.http11.Http11Nio2Protocol
 */
public class Http11Nio2Protocol extends AbstractHttp11JsseProtocol<Nio2Channel> {

    public Http11Nio2Protocol() {
        super(new Nio2Endpoint());
    }
}
