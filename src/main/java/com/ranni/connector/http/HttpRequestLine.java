package com.ranni.connector.http;

/**
 * Title: HttpServer
 * Description: 请求行内容
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 21:45
 */
public class HttpRequestLine {

    // 静态参数变量
    public final static int INITIAL_METHOD_SIZE = 8;
    public final static int MAX_METHOD_SIZE = 1024;
    public final static int INITIAL_URI_SIZE = 64;
    public final static int MAX_URI_SIZE = 32768;
    public final static int INITIAL_PROTOCOL_SIZE = 8;
    public final static int MAX_PROTOCOL_SIZE = 1024;

    public char[] method;
    public int methodEnd;
    public char[] uri;
    public int uriEnd;
    public char[] protocol;
    public int protocolEnd;
    private int paramsIndex = -2; // uri中参数的位置

    public HttpRequestLine() {
        this(new char[INITIAL_METHOD_SIZE], 0, new char[INITIAL_URI_SIZE], 0, new char[INITIAL_PROTOCOL_SIZE], 0);
    }

    public HttpRequestLine(char[] method, int methodEnd, char[] uri, int uriEnd, char[] protocol, int protocolEnd) {
        this.method = method;
        this.methodEnd = methodEnd;
        this.uri = uri;
        this.uriEnd = uriEnd;
        this.protocol = protocol;
        this.protocolEnd = protocolEnd;
    }

    /**
     * 获取uri中指定字符第一次出现的下标
     * @param ch
     * @return
     */
    public int indexOf(char ch) {
        if (paramsIndex != -2) return paramsIndex;

        paramsIndex = -1;

        for (int i = 0; i < uriEnd; i++) {
            if (uri[i] == ch) {
                paramsIndex = i;
                break;
            }
        }

        return paramsIndex;
    }

    /**
     * 初始化method、uri以及protocol的结束位置
     */
    public void recycle() {
        this.methodEnd = 0;
        this.uriEnd = 0;
        this.protocolEnd = 0;
    }
}
