package com.ranni.connector.socket;

import com.ranni.connector.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Title: HttpServer
 * Description:
 * 默认实现serverSocket
 * 创建单例DefaultServerSocketFactory对象
 * 以及实现创建单例ServerSocket对象
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 21:29
 */
public class DefaultServerSocketFactory implements ServerSocketFactory {
    private static volatile ServerSocket socket;
    private static volatile DefaultServerSocketFactory factory;

    private DefaultServerSocketFactory() {}

    /**
     * 创建单例DefaultServerSocketFactory对象
     * @return 返回单例DefaultServerSocketFactory对象
     */
    public static DefaultServerSocketFactory getServerSocketFactory() {
        if (factory == null) {
            synchronized (DefaultServerSocketFactory.class) {
                if (factory == null) {
                    factory = new DefaultServerSocketFactory();
                }
            }
        }

        return factory;
    }

    /**
     * 创建serverSocket对象
     * @param port 端口号
     * @return 返回用于服务器的socket
     * @throws IOException
     * @throws IllegalStateException
     */
    @Override
    public ServerSocket createSocket(int port) throws IOException, IllegalStateException {

        return createSocket(port, Constants.DEFAULT_BACKLOG);
    }

    /**
     * 创建serverSocket对象
     * @param port 端口号
     * @param backlog 最大等待队列
     * @return 返回用于服务器的socket
     * @throws IllegalStateException
     * @throws IOException
     */
    @Override
    public ServerSocket createSocket(int port, int backlog) throws IllegalStateException, IOException  {

        return createSocket(port, backlog, InetAddress.getByName(Constants.DEFAULT_SERVER_IPADDRESS));
    }

    /**
     * 创建serverSocket对象
     * @param port 端口号
     * @param backlog 最大等待队列
     * @param ip 服务器ip
     * @return 返回用于服务器的socket
     * @throws IllegalStateException
     * @throws IOException
     */
    @Override
    public ServerSocket createSocket(int port, int backlog, InetAddress ip) throws IllegalStateException, IOException {
        if (socket != null) throw new IllegalStateException("不能重复创建server socket！");

        if (socket == null) {
            synchronized (DefaultServerSocketFactory.class) {
                if (socket == null) {
                    socket = new ServerSocket(port, backlog, ip);
                }
            }
        }

        return socket;
    }
}

