package com.ranni.processor.stream;

import com.ranni.processor.http.HttpResponse;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 21:35
 */
public class ResponseStream extends ServletOutputStream {
    private HttpResponse response;

    public ResponseStream() {
    }

    public ResponseStream(HttpResponse response) {
        this.response = response;
    }

    @Override
    public void write(int b) throws IOException {

    }

    public void setCommit(boolean b) {
    }
}
