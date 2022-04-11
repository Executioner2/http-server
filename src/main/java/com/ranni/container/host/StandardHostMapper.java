package com.ranni.container.host;

import com.ranni.connector.http.request.HttpRequest;
import com.ranni.connector.http.request.Request;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Mapper;

/**
 * Title: HttpServer
 * Description:
 * Host容器的标准映射器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 16:11
 */
public class StandardHostMapper implements Mapper {
    private StandardHost host;
    private String protocol;

    @Override
    public Container getContainer() {
        return null;
    }

    /**
     * 设置容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        if (!(container instanceof StandardHost))
            throw new IllegalArgumentException("不是标准Host容器！");

        this.host = (StandardHost) container;
    }

    /**
     * 取得协议
     *
     * @return
     */
    @Override
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * 设置协议
     *
     * @param protocol
     */
    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * 取得子容器
     *
     * @param request
     * @param update
     * @return
     */
    @Override
    public Container map(Request request, boolean update) {
        // 不用更新容器且请求对象中绑定了容器就直接返回
        if (!update && request.getContext() != null)
            return request.getContext();

        String uri = ((HttpRequest) request).getDecodedRequestURI();
        Context context = host.map(uri);

        // 是否更新容器，即使context为null
        if (update) {
            request.setContext(context);
            if (context == null) {
                ((HttpRequest) request).setContextPath(null);
            } else {
                ((HttpRequest) request).setContextPath(context.getPath());
            }
        }

        return context;
    }
}
