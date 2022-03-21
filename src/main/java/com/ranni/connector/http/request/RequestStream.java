package com.ranni.connector.http.request;

import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:06
 */
public class RequestStream extends ServletInputStream {
    @Override
    public int read() throws IOException {
        return 0;
    }
}
