package com.ranni.connector.coyote.http11;

import com.ranni.connector.coyote.OutputBuffer;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/15 20:00
 */
public interface HttpOutputBuffer extends OutputBuffer {
    
    void end() throws IOException;

    
    void flush() throws IOException;
}
