package com.ranni.connector.coyote;

import com.ranni.connector.ApplicationBufferHandler;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 23:42
 */
public interface InputBuffer {


    /**
     * 从缓冲处理器中读取数据
     * 
     * @param handler 缓冲处理器
     * @return 返回读取的数据数量
     * @throws IOException 可能抛出I/O异常
     */
    int doRead(ApplicationBufferHandler handler) throws IOException;


    /**
     * @return 返回输入缓冲中可用大小
     */
    int available();
}
