package com.ranni.connector.http.response;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:31
 */
public class ResponseWriter extends PrintWriter {
    public ResponseWriter(Writer out) {
        super(out);
    }

    public ResponseWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWriter(OutputStream out) {
        super(out);
    }

    public ResponseWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWriter(OutputStream out, boolean autoFlush, Charset charset) {
        super(out, autoFlush, charset);
    }

    public ResponseWriter(String fileName) throws FileNotFoundException {
        super(fileName);
    }

    public ResponseWriter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public ResponseWriter(String fileName, Charset charset) throws IOException {
        super(fileName, charset);
    }

    public ResponseWriter(File file) throws FileNotFoundException {
        super(file);
    }

    public ResponseWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    public ResponseWriter(File file, Charset charset) throws IOException {
        super(file, charset);
    }
}
