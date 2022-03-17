package com.lani.connector;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-17 20:58
 */
public class ResponseWrite extends PrintWriter {
    public ResponseWrite(Writer out) {
        super(out);
    }

    public ResponseWrite(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWrite(OutputStream out) {
        super(out);
    }

    public ResponseWrite(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWrite(OutputStream out, boolean autoFlush, Charset charset) {
        super(out, autoFlush, charset);
    }

    public ResponseWrite(String fileName) throws FileNotFoundException {
        super(fileName);
    }

    public ResponseWrite(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public ResponseWrite(String fileName, Charset charset) throws IOException {
        super(fileName, charset);
    }

    public ResponseWrite(File file) throws FileNotFoundException {
        super(file);
    }

    public ResponseWrite(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    public ResponseWrite(File file, Charset charset) throws IOException {
        super(file, charset);
    }
}
