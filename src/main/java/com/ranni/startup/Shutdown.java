package com.ranni.startup;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Title: HttpServer
 * Description:
 * 关闭测试类
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 15:58
 */
public class Shutdown {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 8085);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            printWriter.print("SHUTDOWN");
            printWriter.flush();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
