package com.lani.util;

import com.lani.processor.http.ParameterMap;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-07 22:16
 */
public class RequestUtil {
    /**
     * 从字符串中解析cookie
     * @param cookieStr 格式为：userName=zs; password=123; 这种
     * @return
     */
    public static Cookie[] parseCookieHeader(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) return new Cookie[0];

        List<Cookie> cookies = new ArrayList<>();

        while (cookieStr.length() > 0) {
            int semicolon = cookieStr.indexOf(";");
            String token = semicolon < 0 ? "" : cookieStr.substring(0, semicolon);
            cookieStr = semicolon < 0 ? "" : cookieStr.substring(semicolon + 1);

            int equals = token.indexOf("=");
            if (equals > 0) {
                String name = token.substring(0, equals).trim();
                String value = token.substring(equals + 1).trim();
                cookies.add(new Cookie(name, value));
            }
        }

        return cookies.toArray(new Cookie[cookies.size()]);
    }

    /**
     * TODO 解析请求包中的参数
     * @param result
     * @param queryString
     * @param encoding
     */
    public static void parseParameters(ParameterMap result, String queryString, String encoding) {
        if (queryString == null || queryString.isBlank()) return;

    }

    /**
     * TODO 解析请求包中的参数
     * @param result
     * @param buffer
     * @param encoding
     */
    public static void parseParameters(ParameterMap result, byte[] buffer, String encoding) {

    }
}
