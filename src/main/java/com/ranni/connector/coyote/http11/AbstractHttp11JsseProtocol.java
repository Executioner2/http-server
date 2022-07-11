package com.ranni.connector.coyote.http11;

import com.ranni.util.net.AbstractEndpoint;
import com.ranni.util.net.AbstractJsseEndpoint;

/**
 * Title: HttpServer
 * Description:
 * SSL相关，目前无实现，以后拓展用
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/11 16:00
 */
public abstract class AbstractHttp11JsseProtocol<S> extends AbstractHttp11Protocol<S> {
    public AbstractHttp11JsseProtocol(AbstractEndpoint<S, ?> endpoint) {
        super(endpoint);
    }

    @Override
    protected AbstractJsseEndpoint<S,?> getEndpoint() {
        return (AbstractJsseEndpoint<S,?>) super.getEndpoint();
    }
}
