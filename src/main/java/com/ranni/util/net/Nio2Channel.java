package com.ranni.util.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

/**
 * Title: HttpServer
 * Description:
 * SocketChannel的包装类。也是相当于是ByteBuffer的包装
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/4 12:21
 * @Ref org.apache.tomcat.util.net.Nio2Channel
 */
public class Nio2Channel implements AsynchronousByteChannel {
    @Override
    public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
        
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        return null;
    }

    @Override
    public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {

    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
