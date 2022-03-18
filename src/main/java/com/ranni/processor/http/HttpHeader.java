package com.ranni.processor.http;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-07 22:07
 */
public class HttpHeader {
    public static final int INITIAL_NAME_SIZE = 32;
    public static final int INITIAL_VALUE_SIZE = 64;
    public static final int MAX_NAME_SIZE = 128;
    public static final int MAX_VALUE_SIZE = 4096;

    public char[] name;
    public int nameEnd;
    public char[] value;
    public int valueEnd;

    public HttpHeader() {
        this(new char[INITIAL_NAME_SIZE], 0, new char[INITIAL_VALUE_SIZE], 0);
    }

    public HttpHeader(char[] name, int nameEnd, char[] value, int valueEnd) {
        this.name = name;
        this.nameEnd = nameEnd;
        this.value = value;
        this.valueEnd = valueEnd;
    }

    /**
     * 初始化参数
     */
    public void recycle() {
        this.nameEnd = 0;
        this.valueEnd = 0;
    }
}
