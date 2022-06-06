package com.ranni.util.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.NetworkChannel;

/**
 * Title: HttpServer
 * Description:
 * 实现SSL。
 * TODO:
 * XXX - 几乎没有实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:03
 */
public abstract class AbstractJsseEndpoint<S,U> extends AbstractEndpoint<S,U> {


    protected abstract NetworkChannel getServerSocket();
    
    
    @Override
    protected final InetSocketAddress getLocalAddress() throws IOException {
        NetworkChannel serverSocket = getServerSocket();
        if (serverSocket == null) {
            return null;
        }
        SocketAddress sa = serverSocket.getLocalAddress();
        if (sa instanceof InetSocketAddress) {
            return (InetSocketAddress) sa;
        }
        return null;
    }
}
