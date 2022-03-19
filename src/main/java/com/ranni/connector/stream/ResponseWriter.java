package com.ranni.connector.stream;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Title: HttpServer
 * Description:
 * 此类并无新增自己的代码。
 * 这个类只允许写入，且写入方法都仅仅是在写入后再调入父类的flush方法。
 * 注意：这个类不允许关闭流，流应该让servlet来关闭
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-17 23:32
 */
public class ResponseWriter extends PrintWriter {

    public ResponseWriter(OutputStreamWriter osr) {
        super(osr);
    }

    @Override
    public void flush() {
        super.flush();
    }

    @Override
    public void write(int c) {
        super.write(c);
        super.flush();
    }

    @Override
    public void write(char[] buf, int off, int len) {
        super.write(buf, off, len);
        super.flush();
    }

    @Override
    public void write(char[] buf) {
        super.write(buf);
        super.flush();
    }

    @Override
    public void write(String s, int off, int len) {
        super.write(s, off, len);
        super.flush();
    }

    @Override
    public void write(String s) {
        super.write(s);
        super.flush();
    }

    @Override
    public void print(boolean b) {
        super.print(b);
        super.flush();
    }

    @Override
    public void print(char c) {
        super.print(c);
        super.flush();
    }

    @Override
    public void print(int i) {
        super.print(i);
        super.flush();
    }

    @Override
    public void print(long l) {
        super.print(l);
        super.flush();
    }

    @Override
    public void print(float f) {
        super.print(f);
        super.flush();
    }

    @Override
    public void print(double d) {
        super.print(d);
        super.flush();
    }

    @Override
    public void print(char[] s) {
        super.print(s);
        super.flush();
    }

    @Override
    public void print(String s) {
        super.print(s);
        super.flush();
    }

    @Override
    public void print(Object obj) {
        super.print(obj);
        super.flush();
    }

    @Override
    public void println() {
        super.println();
        super.flush();
    }

    @Override
    public void println(boolean x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(char x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(int x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(long x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(float x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(double x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(char[] x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(String x) {
        super.println(x);
        super.flush();
    }

    @Override
    public void println(Object x) {
        super.println(x);
        super.flush();
    }
}
