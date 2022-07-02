package com.ranni.coyote;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/15 20:01
 */
public interface OutputBuffer {

    /**
     * 将数据从服务器的输出缓冲区写入到socket的输出信道
     * 
     * @param chunk 要写入的数据
     * @return 返回实际写入的数据量
     * 
     * @throws IOException 可能抛出I/O异常
     */
    int doWrite(ByteBuffer chunk) throws IOException;


    /**
     * @return 返回写入的字节数
     */
    long getBytesWritten();
}
