package com.ranni.connector.http.response;

import com.ranni.util.CookieTools;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
    protected Map<String, List<String>> headers = new HashMap<>();
    protected final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); // 格式化日期格式
    protected String message = getStatusMessage(HttpServletResponse.SC_OK); // 状态信息
    protected int status; // 响应状态码

    /**
     * 调用此方法发送响应包
     */
    public void finishResponse() throws IOException {
        // 如果响应未提交，且状态码为400及以上就返回错误页面
        if (!isCommitted() && stream == null && writer == null
                && status >= HttpServletResponse.SC_BAD_REQUEST
                && contentType == null && contentCount == 0) {

            setContentType("text/html");
            PrintWriter writer = getWriter();
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<title>Tomcat Error Report</title>");
            writer.println("<br><br>");
            writer.println("<h1>HTTP Status ");
            writer.print(status);
            writer.print(" - ");
            if (message != null)
                writer.print(message);
            else
                writer.print(getStatusMessage(status));
            writer.println("</h1>");
            writer.println("</body>");
            writer.println("</html>");
        }

        sendHeaders();
        super.finishResponse();
    }

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

    /**
     * TODO 对重定向url进行编码
     * @param s
     * @return
     */
    @Override
    public String encodeRedirectURL(String s) {
        return null;
    }

    /**
     * 对URL进行编码
     * @param s
     * @return
     */
    @Override
    public String encodeUrl(String s) {
        return encodeURL(s);
    }

    /**
     * 对重定向url进行编码
     * @param s
     * @return
     */
    @Override
    public String encodeRedirectUrl(String s) {
        return encodeRedirectURL(s);
    }

    public String getProtocol() {
        return request.getRequest().getProtocol();
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

        // 输出content-type 和 content-count
        if (contentType != null) {
            pw.print("Content-Type: ");
            pw.print(contentType);
            pw.print("\r\n");
        }
        if (contentLength != -1) {
            pw.print("Content-Length: ");
            pw.print(contentLength);
            pw.print("\r\n");
        }

        // 输出响应头
        synchronized (headers) {
            for (String key : headers.keySet()) {
                List<String> values = headers.get(key);
                for (String value : values) {
                    // XXX 这段代码有问题，响应头会有多个相同name的不同value。Tomcat源码这样写的，不太理解
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
        committed = true; // 已经提交响应头
    }

    /**
     * 发送错误消息
     * @param i
     * @param s
     * @throws IOException
     */
    @Override
    public void sendError(int i, String s) throws IOException {
        if (isCommitted()) throw new IllegalStateException("http响应已提交！");

        setError();

        this.status = i;
        this.message = s;

        resetBuffer();
        setSuspended(true);
    }

    /**
     * 发送错误消息
     * @param i
     * @throws IOException
     */
    @Override
    public void sendError(int i) throws IOException {
        sendError(i, getStatusMessage(i));
    }

    /**
     * 获取状态码对应的消息
     * @param status
     * @return
     */
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

    /**
     * TODO 重定向
     * @param s
     * @throws IOException
     */
    @Override
    public void sendRedirect(String s) throws IOException {

    }

    /**
     * 向响应头中设置日期
     * @param s
     * @param l
     */
    @Override
    public void setDateHeader(String s, long l) {
        if (isCommitted()) return;
        addHeader(s, format.format(new Date(l)));
    }

    /**
     * 向响应头中设置日期
     * @param s
     * @param l
     */
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

//        // XXX 不合理的代码，注释部分是Tomcat的源码，感觉这样写List是多余的
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

    /**
     * 将数值型添加到响应头
     * @param s
     * @param i
     */
    @Override
    public void setIntHeader(String s, int i) {
        setHeader(s, "" + i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        setIntHeader(s, i);
    }

    /**
     * 设置响应状态码
     * @param i
     */
    @Override
    public void setStatus(int i) {
        if (isCommitted()) return;
        this.status = i;
    }

    /**
     * 设置响应状态码和响应信息
     * @param i
     * @param s
     */
    @Override
    public void setStatus(int i, String s) {
        if (isCommitted()) return;
        this.status = i;
        this.message = s;
    }

    /**
     * 返回cookie
     * @return
     */
    @Override
    public Cookie[] getCookies() {
        synchronized (cookies) {
            return cookies.toArray(new Cookie[cookies.size()]);
        }
    }

    /**
     * 返回headers中指定name的values.get(0)
     * @param name
     * @return
     */
    @Override
    public String getHeader(String name) {
        List<String> values = null;

        synchronized (headers) {
            values = headers.get(name);
        }

        if (values == null || values.isEmpty()) return null;

        return values.get(0);
    }

    /**
     * 返回header中所有name
     * @return
     */
    @Override
    public String[] getHeaderNames() {
        Set<String> set = null;
        synchronized (headers) {
            set = headers.keySet();
        }

        if (set == null) return null;

        return set.toArray(new String[set.size()]);
    }

    /**
     * 返回指定name的values
     * @param name
     * @return
     */
    @Override
    public String[] getHeaderValues(String name) {
        List<String> values = null;

        synchronized (headers) {
            values = headers.get(name);
        }

        if (values == null) return null;

        return values.toArray(new String[values.size()]);
    }

    /**
     * 返回响应行消息
     * @return
     */
    @Override
    public String getMessage() {
        return this.message;
    }

    /**
     * 返回响应行状态码
     * @return
     */
    @Override
    public int getStatus() {
        return this.status;
    }

    /**
     * 重置此响应并设置状态码和消息
     * @param status
     * @param message
     */
    @Override
    public void reset(int status, String message) {
        reset();
        setStatus(status, message);
    }

    /**
     * TODO 发送响应确认，Tomcat也未实现此功能
     * @throws IOException
     */
    @Override
    public void sendAcknowledgement() throws IOException {

    }

    /**
     * 刷新缓冲区，如果第一次调用此方法，
     * 则在用户数据之前发送响应头
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException {
        // XXX Tomcat这里做了些安全工作
        doFlushBuffer();
    }

    private void doFlushBuffer() throws IOException {
        // 第一次提交
        if (!isCommitted())
            sendHeaders();

        super.flushBuffer();
    }
}
