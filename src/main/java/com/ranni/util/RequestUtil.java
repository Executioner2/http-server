package com.ranni.util;

import com.ranni.connector.ParameterMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
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
 * @Ref org.apache.catalina.util.RequestUtil
 */
public class RequestUtil {
    /**
     * 从字符串中解析cookie
     *
     * @param cookieStr 格式为：userName=zs; password=123; 这种
     *
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
     *
     * @param map
     * @param queryString 在uri中的查询字符串。格式为 username=zs&userid=1&sex=m  这种
     * @param encoding 编码方式
     */
    @Deprecated
    public static void parseParameters(ParameterMap map, String queryString, String encoding) throws UnsupportedEncodingException {
        if (queryString == null || queryString.isBlank()) return;
        byte[] data = queryString.getBytes(); // 转byte
        parseParameters(map, data, encoding);
    }

    /**
     * 解析请求包中的参数
     *
     * @param map
     * @param data
     * @param encoding
     */
    @Deprecated
    public static void parseParameters(ParameterMap map, byte[] data, String encoding) throws UnsupportedEncodingException {
        if (data == null || data.length == 0) return;

        int pos = 0; // 记录key或value的起始位置
        String key = null;
        String value = null;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            switch ((char)b) {
                case '&':
                    if (key != null) {
                        // key不为null，说明前面已经有value可以解析了
                        value = new String(data, pos, i - pos, encoding);
                        putMapEntry(map, key, value);
                        key = null;
                    }
                    pos = i + 1;
                    break;
                case '=':
                    key = new String(data, pos, i - pos, encoding);
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
     *
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
     *
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

    /**
     * 对uri进行修正（规范化），如 '\' 会被替换为 '/'
     *
     * @param uri
     *
     * @return normalized 规范化后的uri
     */
    public static String normalize(String uri) {
        if (uri == null) return null;

        String normalized = uri;

        // 不符合规范的转义字符，以下是特殊转义字符
        // 一般只有uri中的参数部分才需要用到下述转义字符
        // 在调用该方法之前已经对参数部分进行过处理，当前的uri中不会有参数部分
        // 如果当前的uri中出现了下述转义，则不符合规范
        if ((normalized.indexOf("%25") >= 0) // %
                || (normalized.indexOf("%2F") >= 0) // /
                || (normalized.indexOf("%2E") >= 0) // .
                || (normalized.indexOf("%5C") >= 0) // \
                || (normalized.indexOf("%2f") >= 0) // /
                || (normalized.indexOf("%2e") >= 0) // .
                || (normalized.indexOf("%5c") >= 0) // \
        ){
            return null;
        }

        // 开头为 "/%7E" 和 "/%7e" 转义后为 /~ 这个是合法的，需要转回来
        if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e")) normalized = normalized.substring(4);

        if (!normalized.startsWith("/")) normalized = "/" + normalized;

        // 将 "//" 转换为 '/'
        normalized.replaceAll("//", "/");

        // 将 "\\" 转换为 '/'
        normalized.replaceAll("\\\\", "/");

        // 将 "/./" 转换为 '/'
        normalized.replaceAll("/\\./", "/");

        // 转换 "/.." ，这个是返回上一级
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0) break;
            if (index == 0) return null; // 试图跳出WEB_ROOT

            int index2 = normalized.lastIndexOf("/", index - 1); // 从[0, index - 1]内最后一次出现的 '/'
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);

        }

        if ("/.".equals(normalized)) return "/";

        if (normalized.indexOf("/...") >= 0) return  null;

        return normalized;
    }

    /**
     * 解析响应类型
     *
     * @param contentType
     *
     * @return
     */
    public static String parseCharacterEncoding(String contentType) {
        if (contentType == null) return null;
        int start = contentType.indexOf("charset=");

        if (start < 0) return null;

        int offset = 8; // 字符串 "charset=" 的长度
        String encoding = contentType.substring(start + offset);
        int end = encoding.indexOf(";");

        if (end >= 0)
            encoding = encoding.substring(0, end);

        encoding = encoding.trim();
        if (encoding.length() > 2 && encoding.startsWith("\"") && encoding.startsWith("\"")) {
            encoding = encoding.substring(1, encoding.length() - 1);
        }

        return encoding;
    }

    /**
     * 按默认编码格式对uri解码
     *
     * @param requestURI
     *
     * @return
     */
    public static String URLDecode(String requestURI) {
        return URLDecode(requestURI, null);
    }

    /**
     * 对输入的字符串转byte后再调用该方法的重载方法进行解码
     *
     * @param str
     * @param enc
     *
     * @return
     */
    public static String URLDecode(String str, String enc) {
        if (str == null) return null;

        byte[] bytes = null;

        try {
            bytes = str.getBytes();

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return URLDecode(bytes, enc);
    }

    /**
     * 对传入的字节数组按默认编码格式解码
     *
     * @param bytes
     *
     * @return
     */
    public static String URLDecode(byte[] bytes) {
        return URLDecode(bytes, null);
    }

    /**
     * 对传入的字节数组按指定编码格式解码
     *
     * @param bytes
     * @param enc 如果传入null，则按默认编码格式解码
     *
     * @return
     */
    public static String URLDecode(byte[] bytes, String enc) {
        if (bytes == null) return null;

        int len = bytes.length;
        int ix = 0;
        int ox = 0;

        // 对转义字符进行转义
        while (ix < len) {
            byte b = bytes[ix++];
            if (b == '+') {
                // +号变空格
                b = ' ';
            } else if (b == '%') {
                b = (byte) ((convertHexDigit(bytes[ix++]) << 4) + convertHexDigit(bytes[ix++]));
            }

            bytes[ox++] = b;
        }

        if (enc != null) {
            // 指定了编码方式的
            try {
                return new String(bytes, 0, ox, enc);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new String(bytes, 0, ox);
    }


    /**
     * 拼接出请求包的URL
     * 
     * @param request HttpServletRequest实例
     * @return 返回StringBuffer类型的URL字符串
     */
    public static StringBuffer getRequestURL(HttpServletRequest request) {
        StringBuffer url = new StringBuffer();
        String scheme = request.getScheme();
        int port = request.getServerPort();
        if (port < 0) {
            // Work around java.net.URL bug
            port = 80;
        }

        url.append(scheme);
        url.append("://");
        url.append(request.getServerName());
        if ((scheme.equals("http") && (port != 80))
                || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(request.getRequestURI());

        return url;
    }
}
