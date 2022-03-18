package com.ranni.processor.stream;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-17 23:32
 */
public class ResponseWriter extends PrintWriter {

    public ResponseWriter(OutputStreamWriter osr) {
        super(osr);
    }

    public ResponseWriter(Writer out) {
        super(out);
    }

    @Override
    public void flush() {
        super.flush();
    }

    @Override
    public void close() {
        super.close();
    }
}
