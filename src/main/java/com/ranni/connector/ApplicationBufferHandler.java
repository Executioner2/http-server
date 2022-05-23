package com.ranni.connector;

import java.nio.ByteBuffer;

/**
 * Title: HttpServer
 * Description:
 * 应用缓冲处理接口
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 21:05
 */
public interface ApplicationBufferHandler {
    ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0); // 空的缓冲区

    ApplicationBufferHandler EMPTY = new ApplicationBufferHandler() { // 空的缓冲处理器
        @Override
        public void setByteBuffer(ByteBuffer buffer) {  }

        @Override
        public ByteBuffer getByteBuffer() {
            return EMPTY_BUFFER;
        }

        @Override
        public void expand(int size) {  }
    };

    /**
     * 设置字节缓冲
     * 
     * @param buffer 字节缓冲
     */
    void setByteBuffer(ByteBuffer buffer);


    /**
     * @return 返回字节缓冲 
     */
    ByteBuffer getByteBuffer();


    /**
     * 缓冲区扩展
     * 
     * @param size 扩展的大小 
     */
    void expand(int size);
}
