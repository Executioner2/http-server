package com.ranni.util;

import com.ranni.processor.http.ParameterMap;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
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
     * 解析查询字符串中的key和value
     * 其实也就是解析GET请求包中的参数
     * @param map
     * @param queryString 在uri中的查询字符串。格式为 username=zs&userid=1&sex=m  这种
     * @param encoding 编码方式
     */
    public static void parseParameters(ParameterMap map, String queryString, String encoding) throws UnsupportedEncodingException {
        if (queryString == null || queryString.isBlank()) return;
        byte[] data = queryString.getBytes(); // 转byte
        parseParameters(map, data, encoding);
    }

    /**
     * 解析请求包中的参数
     * XXX 以下代码未经测试，转码可能会有bug
     * @param map
     * @param data
     * @param encoding
     */
    public static void parseParameters(ParameterMap map, byte[] data, String encoding) throws UnsupportedEncodingException {
        if (data == null || data.length == 0) return;

        int pos = 0; // 记录key或value的起始位置
        String key = null;
        String value = null;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            switch ((char)b) {
                case '&':
                    pos = i + 1;
                    if (key != null) {
                        // key不为null，说明前面已经有value可以解析了
                        value = new String(data, pos, i - pos + 1, encoding);
                        putMapEntry(map, key, value);
                        key = null;
                    }
                    break;
                case '=':
                    key = new String(data, pos, i - pos + 1, encoding);
                    pos = i + 1;
                    break;
                case '+': // +号变空格
                    data[i] = (byte)' ';
                    break;
                case '%': // %号在http请求包中当作转义字符
                    data[i] = (byte)((convertHexDigit(data[i + 1]) << 4) + convertHexDigit(data[i + 2]));
                    i += 2;
                    break;
            }
        } // for end

        if (key != null) {
            value = new String(data, pos, data.length - pos, encoding);
            putMapEntry(map, key, value);
        }
    }

    /**
     * 转16进制
     * @param b
     * @return
     */
    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
        return 0;
    }

    /**
     * 放入到map中，以数组作为map的value
     * @param map
     * @param name
     * @param value
     */
    private static void putMapEntry(ParameterMap map, String name, String value) {
        String[] oldValues = (String[]) map.get(name);
        String[] newValues = null;

        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }

        map.put(name, newValues);
    }
}
