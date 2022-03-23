package com.ranni.connector.http.response;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:27
 */
public class HttpResponseBase extends ResponseBase implements HttpResponse, HttpServletResponse {
    protected List<Cookie> cookies = new ArrayList<>();
    protected Map<String, ArrayList<String>> headers = new HashMap<>();
    protected final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); // 格式化日期格式

    /**
     * 添加cookie
     * @param cookie
     */
    @Override
    public void addCookie(Cookie cookie) {
        synchronized (cookies) {
            cookies.add(cookie);
        }
    }

    /**
     * 响应头中是否包含指定key
     * @param s
     * @return
     */
    @Override
    public boolean containsHeader(String s) {
        synchronized (headers) {
            return headers.containsKey(s);
        }
    }

    /**
     * TODO 对URL进行编码
     * @param s
     * @return
     */
    @Override
    public String encodeURL(String s) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String s) {
        return null;
    }

    @Override
    public String encodeUrl(String s) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return null;
    }

    @Override
    public void sendError(int i, String s) throws IOException {

    }

    @Override
    public void sendError(int i) throws IOException {

    }

    @Override
    public void sendRedirect(String s) throws IOException {

    }

    @Override
    public void setDateHeader(String s, long l) {

    }

    @Override
    public void addDateHeader(String s, long l) {

    }

    @Override
    public void setHeader(String s, String s1) {

    }

    @Override
    public void addHeader(String s, String s1) {

    }

    @Override
    public void setIntHeader(String s, int i) {

    }

    @Override
    public void addIntHeader(String s, int i) {

    }

    @Override
    public void setStatus(int i) {

    }

    @Override
    public void setStatus(int i, String s) {

    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public String[] getHeaderNames() {
        return new String[0];
    }

    @Override
    public String[] getHeaderValues(String name) {
        return new String[0];
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void reset(int status, String message) {

    }

    @Override
    public void sendAcknowledgement() throws IOException {

    }
}
