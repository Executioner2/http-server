package com.lani.processor.http;
import com.lani.processor.stream.SocketInputStream;
import com.lani.util.Enumerator;
import com.lani.util.RequestUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Principal;
import java.text.ParseException;
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
public class HttpRequest implements HttpServletRequest{
    protected static List<String> empty = new ArrayList(); // 统一返回的空数组

    protected Map<String, ArrayList<String>> headers = new HashMap<>();
    protected List<Cookie> cookies = new ArrayList<>();
    protected ParameterMap<String, String[]> parameters = null;
    protected ServletInputStream stream = null;
    protected BufferedReader reader = null;
    protected boolean parsed = false; // 是否已经解析完成
    protected String pathInfo = null;
    protected Map attributes = new HashMap();

    protected SimpleDateFormat formats[] = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };


    private InetAddress inetAddress;
    private InputStream input = null;
    private String method;
    private String queryString;
    private String requestURI;
    private String serverName;
    private int serverPort;
    private Socket socket;
    private boolean requestedSessionURL; // session是否在url中
    private boolean requestedSessionCookie; // 请求的session id 来自于cookie
    private String requestedSessionId; // session id
    private String contextPath;
    private String contentType; // 请求体类型
    private int contentLength;

    public HttpRequest() {
    }

    public HttpRequest(SocketInputStream input) {
        this.input = input;
    }


    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        synchronized (cookies) {
            if (cookies.size() < 1) {
                return null;
            } else {
                return (Cookie[])(cookies.toArray());
            }
        }
    }

    /**
     * 从header中获取日期
     * @param s
     * @return
     */
    @Override
    public long getDateHeader(String s) {
        String value = getHeader(s);

        for (int i = 0; i < formats.length; i++) {
            try {
                // 第一个格式能转就转了
                Date date = formats[i].parse(value);
                return date.getTime();
            } catch (ParseException e) {
                ;
            }
        }

        throw new IllegalArgumentException(value);
    }

    /**
     * 获取headers中指定name的第一个值
     * @param s
     * @return
     */
    @Override
    public String getHeader(String s) {
        s = s.toLowerCase();
        synchronized (headers) {
            List<String> values = headers.get(s);
            return values == null ? null : values.get(0);
        }
    }

    /**
     * 获取请求头中指定数据的迭代器
     * @param s
     * @return 返回一个迭代器，就是Iterator的包装
     */
    @Override
    public Enumeration getHeaders(String s) {
        s = s.toLowerCase();
        synchronized (headers) {
            List<String> values = headers.get(s);
            if (values == null) {
                return new Enumerator(empty);
            } else {
                return new Enumerator(values);
            }
        }
    }

    /**
     * 获取请求头中的所有键
     * @return
     */
    @Override
    public Enumeration getHeaderNames() {
        synchronized (headers) {
            return new Enumerator(headers.keySet());
        }
    }

    /**
     * 获取headers中指定name的第一个值并转为数值型
     * @param s
     * @return
     */
    @Override
    public int getIntHeader(String s) {
        String value = getHeader(s);
        if (value == null) {
            return -1;
        } else {
            return Integer.parseInt(value);
        }
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return this.requestedSessionId;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(this.requestURI);
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return this.requestedSessionCookie;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return this.requestedSessionURL;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return this.requestedSessionURL;
    }

    @Override
    public Object getAttribute(String s) {
        synchronized (attributes) {
            return attributes.get(s);
        }
    }

    @Override
    public Enumeration getAttributeNames() {
        synchronized (attributes) {
            return (new Enumerator(attributes));
        }
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return this.stream;
    }

    /**
     * 内部调用，解析参数
     * 在调用 getParameter、getParameterNames、getParameterValues 和 getParameterMap时会先调用该方法
     */
    private void parseParameters() throws UnsupportedEncodingException {
        if (parsed) return; // 已经解析了

        ParameterMap result = new ParameterMap(); // 这里新建一个引用是为了保证中途的操作能够全部正常完成后再交给parameters引用，避免数据不一致。
        result.setLocked(false);

        String encoding = getCharacterEncoding() == null ? "ISO-8859-1" : getCharacterEncoding(); // TODO getCharacterEncoding 待实现

        if ("GET".equals(getMethod())) {
            RequestUtil.parseParameters(result, getQueryString(), encoding); // 这里是解析查询字符串的参数，但按规范只对GET请求生效
        }

        String contentType = getContentType();
        if (contentType == null) {
            contentType = "";
        } else {
            int semicolon = contentType.indexOf(";");
            if (semicolon >= 0) contentType = contentType.substring(0, semicolon).trim();
            else contentType = getContentType().trim();
        }

        if ("POST".equals(getMethod()) && "application/x-www-form-urlencoded".equals(contentType)
                && getContentLength() > 0) {
            // 解析请求体
            int max = getContentLength();
            int len = 0;
            byte buffer[] = new byte[getContentLength()]; // 把剩余的数据全部读出来

            try {
                ServletInputStream is = getInputStream();
                while (len < max) {
                    int next = is.read(buffer, len, max - len);
                    if (next < 0) break;
                    len += max;
                }
                is.close();
                if (len < max) throw new RuntimeException("请求体内容未读完！");
                RequestUtil.parseParameters(result, buffer, encoding); // 解析请求体待实现
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        result.setLocked(true);
        parsed = true;
        parameters = result;
    }

    @Override
    public String getParameter(String s) {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String[] values = parameters.get(s);

        return values == null ? null : values[0];
    }

    @Override
    public Enumeration getParameterNames() {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return new Enumerator(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String s) {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return parameters.get(s);
    }

    @Override
    public Map getParameterMap() {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        return parameters;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    public ParameterMap getParameters() {
        return parameters;
    }

    public void setParameters(ParameterMap parameters) {
        this.parameters = parameters;
    }

    public ServletInputStream getStream() {
        return stream;
    }

    public void setStream(ServletInputStream stream) {
        this.stream = stream;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setPathInfo(String path) {
        this.pathInfo = path;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isRequestedSessionURL() {
        return requestedSessionURL;
    }

    public void setRequestedSessionURL(boolean requestedSessionURL) {
        this.requestedSessionURL = requestedSessionURL;
    }

    public void setRequestedSessionId(String requestedSessionId) {
        this.requestedSessionId = requestedSessionId;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContentLength(int n) {
        this.contentLength = n;
    }

    /**
     * 添加参数到headers中
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        name = name.toLowerCase();
        synchronized (headers) {
            List<String> values = headers.getOrDefault(name, new ArrayList<>());
            values.add(value);
        }
    }

    public boolean isRequestedSessionCookie() {
        return requestedSessionCookie;
    }

    public void setRequestedSessionCookie(boolean requestedSessionCookie) {
        this.requestedSessionCookie = requestedSessionCookie;
    }

    /**
     * 添加cookie到cookies中
     * @param cookie
     */
    public void addCookie(Cookie cookie) {
        synchronized (cookies) {
            cookies.add(cookie);
        }
    }
}
