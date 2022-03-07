package com.lani.processor.stream;

import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 21:33
 */
public class RequestStream extends ServletInputStream {
    @Override
    public int read() throws IOException {
        return 0;
    }
}
