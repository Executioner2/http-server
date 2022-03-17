package com.lani.processor.http;

import com.lani.processor.stream.ResponseStream;
import com.lani.processor.stream.ResponseWriter;
import com.lani.util.CookieTools;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 20:08
 */
public class HttpResponse implements HttpServletResponse {
    private static final int BUFFER_SIZE = 1024;

    private HttpRequest request;
    private OutputStream output;
    private PrintWriter writer;

    protected byte[] buffer = new byte[BUFFER_SIZE];
    protected int bufferCount = 0;
    protected boolean committed = false; // 是否已提交
    protected int contentCount = 0; // 写入响应包的实际字节数
    protected int contentLength = -1; // 响应包内容的长度
    protected String contentType = null; // 响应内容类型
    protected String encoding = null; // 编码格式
    protected Map<String, List<String>> headers = new HashMap(); // http响应头
    protected List<Cookie> cookies = new ArrayList(); // cookies
    protected int status = HttpServletResponse.SC_OK; // 返回状态
    protected String message = getStatusMessage(HttpServletResponse.SC_OK); // 设置消息
    protected final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);

    public HttpResponse() {
    }

    public HttpResponse(OutputStream output) {
        this.output = output;
    }

    /**
     * 调用此方法发送响应包
     */
    public void finishResponse() {
        sendHeaders();
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    public OutputStream getStream() {
        return this.output;
    }

    public String getProtocol() {
        return this.request.getProtocol();
    }

    /**
     * 没有发送就发送请求头
     */
    protected void sendHeaders() {
        if (isCommitted()) return;

        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(getStream(), getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            // 因为编码发生错误，那么就采用默认编码
            osw = new OutputStreamWriter(getStream());
        }

        // 输出状态行
        PrintWriter pw = new PrintWriter(osw);
        pw.print(getProtocol());
        pw.print(" "); // 分开写，减少拼串带来的性能损失
        pw.print(status);
        if (message != null) {
            pw.print(" ");  // 分开写，减少拼串带来的性能损失
            pw.print(message);
        }
        pw.print("\r\n");

        // 输出响应头
        synchronized (headers) {
            for (String key : headers.keySet()) {
                List<String> values = headers.get(key);
                for (String value : values) {
                    // XXX 这段代码有问题，响应头会有多个相同的name的不同value。HowTomcatWorks源码这样写的，不太理解
                    pw.print(key);
                    pw.print(": ");
                    pw.print(value);
                    pw.print("\r\n");
                }
            }
        }

        // TODO 添加session id

        // 写入cookies信息
        synchronized (cookies) {
            for (Cookie cookie : cookies) {
                // TODO 这里这个CookieTools懒得写了，后续再自己写
                pw.print(CookieTools.getCookieHeaderName(cookie));
                pw.print(": ");
                pw.print(CookieTools.getCookieHeaderValue(cookie));
                pw.print("\r\n");
            }
        }

        pw.print("\r\n"); // 输出空白行分割响应实体段和响应头
        pw.flush(); // 刷新流
        committed = true; // 已经提交响应
    }

    /**
     * 添加cookie
     * @param cookie
     */
    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted()) return;

        synchronized (cookies) {
            cookies.add(cookie);
        }
    }

    @Override
    public boolean containsHeader(String s) {
        synchronized (headers) {
            return headers.containsKey(s);
        }
    }

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
        return encodeURL(s);
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return encodeRedirectURL(s);
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
        setHeader(s, format.format(new Date(l)));
    }

    @Override
    public void addDateHeader(String s, long l) {
        setDateHeader(s, l);
    }

    /**
     * 向响应头中添加信息
     * @param name
     * @param value
     */
    @Override
    public void setHeader(String name, String value) {
        if (isCommitted()) return;

//        // XXX 不合理的代码，注释部分是HowTomcatWorks的源码，感觉这样写List是多余的
//        List<String> values = new ArrayList<>() {{
//            add(value);
//        }};
//        synchronized (headers) {
//            headers.put(name, values);
//        }

        // 自己的改动 start
        synchronized (headers) {
            List<String> values = headers.getOrDefault(name, new ArrayList<>());
            values.add(value);
            headers.put(name, values);
        } // 自己的改动 end

        String match = name.toLowerCase();
        if ("content-length".equals(match)) {
            int contentLength = -1;
            try {
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {}
            setContentLength(contentLength);
        } else if ("content-type".equals(match)) {
            setContentType(value);
        }
    }

    @Override
    public void addHeader(String s, String s1) {
        setHeader(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        setHeader(s, "" + i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        setIntHeader(s, i);
    }

    @Override
    public void setStatus(int i) {
        if (isCommitted()) return;
        this.status = i;
    }

    @Override
    public void setStatus(int i, String s) {
        if (isCommitted()) return;
        this.status = i;
        this.message = s;
    }

    @Override
    public String getCharacterEncoding() {
        return encoding == null ? "ISO-8859-1" : encoding;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    /**
     * 获得ResponseWriter(继承于PrintWriter)对象，用于向对端发送消息
     * @return
     * @throws IOException
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        ResponseStream newStream = new ResponseStream(this); // TODO 待实现
        newStream.setCommit(false); // TODO 待实现
        OutputStreamWriter osw = new OutputStreamWriter(newStream, getCharacterEncoding());
        writer = new ResponseWriter(osw);  // TODO 待实现
        return writer;
    }

    @Override
    public void setContentLength(int i) {
        if (isCommitted()) return;
        this.contentLength = i;
    }

    @Override
    public void setContentType(String s) {
        if (isCommitted()) return;
        this.contentType = s;
    }

    @Override
    public void setBufferSize(int i) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (bufferCount > 0) {
            try {
                output.write(buffer, 0, bufferCount);
            } finally {
                bufferCount = 0;
            }
        }
    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {

    }

    /**
     * 设置语言为本地语言
     * @param locale
     */
    @Override
    public void setLocale(Locale locale) {
        if (isCommitted()) return;
        String language = locale.getLanguage();
        if (language != null && !language.isBlank()) {
            String country = locale.getCountry();
            StringBuffer sb = new StringBuffer(language);
            if (country != null && !country.isBlank()) {
                sb.append("-");
                sb.append(country);
            }
            setHeader("Content-Language", sb.toString());
        }
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    protected String getStatusMessage(int status) {
        switch (status) {
            case SC_OK:
                return ("OK");
            case SC_ACCEPTED:
                return ("Accepted");
            case SC_BAD_GATEWAY:
                return ("Bad Gateway");
            case SC_BAD_REQUEST:
                return ("Bad Request");
            case SC_CONFLICT:
                return ("Conflict");
            case SC_CONTINUE:
                return ("Continue");
            case SC_CREATED:
                return ("Created");
            case SC_EXPECTATION_FAILED:
                return ("Expectation Failed");
            case SC_FORBIDDEN:
                return ("Forbidden");
            case SC_GATEWAY_TIMEOUT:
                return ("Gateway Timeout");
            case SC_GONE:
                return ("Gone");
            case SC_HTTP_VERSION_NOT_SUPPORTED:
                return ("HTTP Version Not Supported");
            case SC_INTERNAL_SERVER_ERROR:
                return ("Internal Server Error");
            case SC_LENGTH_REQUIRED:
                return ("Length Required");
            case SC_METHOD_NOT_ALLOWED:
                return ("Method Not Allowed");
            case SC_MOVED_PERMANENTLY:
                return ("Moved Permanently");
            case SC_MOVED_TEMPORARILY:
                return ("Moved Temporarily");
            case SC_MULTIPLE_CHOICES:
                return ("Multiple Choices");
            case SC_NO_CONTENT:
                return ("No Content");
            case SC_NON_AUTHORITATIVE_INFORMATION:
                return ("Non-Authoritative Information");
            case SC_NOT_ACCEPTABLE:
                return ("Not Acceptable");
            case SC_NOT_FOUND:
                return ("Not Found");
            case SC_NOT_IMPLEMENTED:
                return ("Not Implemented");
            case SC_NOT_MODIFIED:
                return ("Not Modified");
            case SC_PARTIAL_CONTENT:
                return ("Partial Content");
            case SC_PAYMENT_REQUIRED:
                return ("Payment Required");
            case SC_PRECONDITION_FAILED:
                return ("Precondition Failed");
            case SC_PROXY_AUTHENTICATION_REQUIRED:
                return ("Proxy Authentication Required");
            case SC_REQUEST_ENTITY_TOO_LARGE:
                return ("Request Entity Too Large");
            case SC_REQUEST_TIMEOUT:
                return ("Request Timeout");
            case SC_REQUEST_URI_TOO_LONG:
                return ("Request URI Too Long");
            case SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                return ("Requested Range Not Satisfiable");
            case SC_RESET_CONTENT:
                return ("Reset Content");
            case SC_SEE_OTHER:
                return ("See Other");
            case SC_SERVICE_UNAVAILABLE:
                return ("Service Unavailable");
            case SC_SWITCHING_PROTOCOLS:
                return ("Switching Protocols");
            case SC_UNAUTHORIZED:
                return ("Unauthorized");
            case SC_UNSUPPORTED_MEDIA_TYPE:
                return ("Unsupported Media Type");
            case SC_USE_PROXY:
                return ("Use Proxy");
            case 207:       // WebDAV
                return ("Multi-Status");
            case 422:       // WebDAV
                return ("Unprocessable Entity");
            case 423:       // WebDAV
                return ("Locked");
            case 507:       // WebDAV
                return ("Insufficient Storage");
            default:
                return ("HTTP Response Status " + status);
        }
    }
}
