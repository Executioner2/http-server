package com.ranni.connector.http.response;

/**
 * Title: HttpServer
 * Description:
 * 响应包通用参数
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/19 14:28
 */
public final class DefaultHeaders {
    public static final long HASH = 1610612741L;
    
    public static final long DATE_NAME = hash("date".toCharArray());
    public static final long AUTHORIZATION_NAME = hash("authorization".toCharArray());
    public static final long ACCEPT_LANGUAGE_NAME = hash("accept-language".toCharArray());
    public static final long COOKIE_NAME = hash("cookie".toCharArray());
    public static final long CONTENT_LENGTH_NAME = hash("content-length".toCharArray());
    public static final long CONTENT_TYPE_NAME = hash("content-type".toCharArray());
    public static final long HOST_NAME = hash("host".toCharArray());
    public static final long CONNECTION_NAME = hash("connection".toCharArray());
    public static final long CONNECTION_CLOSE_VALUE = hash("close".toCharArray());
    public static final long EXPECT_NAME = hash("expect".toCharArray());
    public static final long EXPECT_100_VALUE = hash("100-continue".toCharArray());
    public static final long TRANSFER_ENCODING_NAME = hash("transfer-encoding".toCharArray());


    /**
     * hash
     * 加快请求头参数判断
     * 
     * @param chs
     * @return
     */
    private static long hash(char[] chs) {
        long res = 0;

        for (int i = 0; i < chs.length; i++) {
            res = res * HASH + chs[i];
        }
        
        return res;
    }
    
}
