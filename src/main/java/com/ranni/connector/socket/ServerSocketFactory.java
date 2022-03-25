package com.ranni.connector.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Title: HttpServer
 * Description:
 * 服务端socket生产工厂
 * 用于创建单例serverSocket
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 21:28
 */
public interface ServerSocketFactory {
    // 创建serverSocket，手动指定port
    ServerSocket createSocket(int port) throws IllegalStateException, IOException;

    // 创建serverSocket，手动指定port和最大等待队列
    ServerSocket createSocket(int port, int backlog) throws IllegalStateException, IOException;

    // 创建serverSocket，手动指定port、最大等待队列和ip地址
    ServerSocket createSocket(int port, int backlog, InetAddress ip) throws IllegalStateException, IOException;
}
