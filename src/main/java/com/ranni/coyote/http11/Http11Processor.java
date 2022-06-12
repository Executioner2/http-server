package com.ranni.coyote.http11;

import com.ranni.coyote.AbstractProcessor;
import com.ranni.coyote.Adapter;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.SocketWrapperBase;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 20:06
 */
public class Http11Processor extends AbstractProcessor {
    public <S> Http11Processor(AbstractHttp11Protocol<S> sAbstractHttp11Protocol, Adapter adapter) {
    }


    /**
     * 处理请求的方法，会调用适配器然后由适配器交付给对应的容器
     *
     * @param socketWrapper 要处理的socket包装实例
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException {
        return null;
    }
}
