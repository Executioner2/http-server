package com.ranni.connector.http.request;

import com.ranni.connector.http.ParameterMap;
import com.ranni.util.Enumerator;
import com.ranni.util.RequestUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
 * @Date 2022-03-21 23:06
 */
public class HttpRequestBase extends RequestBase implements HttpRequest, HttpServletRequest {
    protected static List<String> empty = new ArrayList(); // 统一返回的空数组
    protected Map<String, ArrayList<String>> headers = new HashMap<>(); // 请求头中的信息
    protected List<Cookie> cookies = new ArrayList<>();
    protected ParameterMap<String, String[]> parameters; // 参数
    protected boolean parsed; // 是否已经解析完成
    protected String pathInfo = ""; // 路径信息
    protected String authType = ""; // 认证类型
    protected SimpleDateFormat formats[] = { // 日期解析格式
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };
    protected String method = ""; // 请求方法
    protected String contextPath = ""; // context路径
    protected String queryString = ""; // 存放再请求行中的查询字符串
    protected String requestedSessionId = ""; // 请求中的session id
    protected String requestURI = ""; // 请求中的uri
    protected String requestURL = ""; // 请求中的url
    protected String servletPath = ""; // 此请求对应的servlet路径
    protected boolean requestedSessionCookie; // session id是否来自于cookie
    protected boolean requestedSessionURL; // session id 是否来自于URL
    protected String decodedRequestURI; // 解码后的uri

    /**
     * 返回认证类型
     * @return
     */
    @Override
    public String getAuthType() {
        return this.authType;
    }

    /**
     * 返货所有cookie
     * @return
     */
    @Override
    public Cookie[] getCookies() {
        synchronized (cookies) {
            if (cookies.isEmpty()) return null;
            return cookies.toArray(new Cookie[cookies.size()]);
        }
    }

    /**
     * 从header中获取日期，返回long
     * @param s
     * @return
     */
    @Override
    public long getDateHeader(String s) {
        String value = getHeader(s);
        if (value.isBlank()) return -1L;

        // 返回被第一个日期格式解析后的time
        for (int i = 0; i < formats.length; i++) {
            try {
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
     * 返回headers对应key的迭代器
     * @param s
     * @return
     */
    @Override
    public Enumeration getHeaders(String s) {
        s = s.toLowerCase();
        synchronized (headers) {
            ArrayList<String> values = headers.get(s);
            if (values == null) return new Enumerator(empty);
            else return new Enumerator(values);
        }
    }

    /**
     * 返回headers的name的迭代器
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
        if (value == null) return -1;
        return Integer.parseInt(value);
    }

    /**
     * 返回请求方法
     * @return
     */
    @Override
    public String getMethod() {
        return this.method;
    }

    /**
     * 返回与此请求相关的路径信息
     * @return
     */
    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    /**
     * 返回此请求的真实路径
     * 此请求的真实路径存储再ServletContext中
     * @return
     */
    @Override
    public String getPathTranslated() {
        if (context == null) return null;
        if (pathInfo == null) return null;
        return context.getServletContext().getRealPath(pathInfo);
    }

    /**
     * 返回context路径
     * @return
     */
    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    /**
     * 返回存放再uri中的查询字符串
     * @return
     */
    @Override
    public String getQueryString() {
        return this.queryString;
    }

    /**
     * TODO 返回已对此请求进行身份验证的远程用户的名称。
     * @return
     */
    @Override
    public String getRemoteUser() {
        return null;
    }

    /**
     * TODO 用户是否有指定角色
     * @param s
     * @return
     */
    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    /**
     * TODO 返回已为请求进行身份验证的主体
     * @return
     */
    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    /**
     * 返回请求中的session id
     * @return
     */
    @Override
    public String getRequestedSessionId() {
        return this.requestedSessionId;
    }

    /**
     * 返回此请求的uri
     * @return
     */
    @Override
    public String getRequestURI() {
        return this.requestURI;
    }

    /**
     * 返回此请求的uri
     * @return
     */
    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(this.requestURL);
    }

    /**
     * 返回servlet的路径
     * @return
     */
    @Override
    public String getServletPath() {
        return this.servletPath;
    }

    /**
     * TODO 返回此请求的session，如有必要就创建一个
     * @param b
     * @return
     */
    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }

    /**
     * 返回此请求的session
     * @return
     */
    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    /**
     * TODO 验证session id是否合法
     * @return
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    /**
     * session id是否来自于cookie
     * @return
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return this.requestedSessionCookie;
    }

    /**
     * session id是否来自于URL
     * @return
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        return this.requestedSessionURL;
    }

    /**
     * session id是否来自于URL
     * @return
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
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

            try (ServletInputStream is = getInputStream()) { // 这里调用is的关闭并没有真正关闭流
                while (len < max) {
                    int next = is.read(buffer, len, max - len);
                    if (next < 0) break;
                    len += max;
                }

                if (len < max) throw new RuntimeException("请求体内容未读完！");
                RequestUtil.parseParameters(result, buffer, encoding); // 解析请求体
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        result.setLocked(true);
        parsed = true;
        parameters = result;
    }

    /**
     * 取得请求中的参数，如果还没有解析就解析
     * @param s
     * @return
     */
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

    /**
     * 返回所有请求参数名的迭代器
     * 如果还没有解析请求参数就解析
     * @return
     */
    @Override
    public Enumeration getParameterNames() {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return new Enumerator(parameters.keySet());
    }

    /**
     * 获取请求参数指定name的所有value
     * 如果还没有解析请求参数就解析
     * @param s
     * @return
     */
    @Override
    public String[] getParameterValues(String s) {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return parameters.get(s);
    }

    /**
     * 获取请求参数Map
     * 如果还没有解析请求参数就解析
     * @return
     */
    @Override
    public Map getParameterMap() {
        try {
            parseParameters();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return parameters;
    }

    /**
     * TODO 返回请求转发器
     * @param s
     * @return
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    /**
     * 添加cookie到cookies中
     * @param cookie
     */
    @Override
    public void addCookie(Cookie cookie) {
        synchronized (cookies) {
            cookies.add(cookie);
        }
    }

    /**
     * 添加参数到headers中
     * @param name
     * @param value
     */
    @Override
    public void addHeader(String name, String value) {
        name = name.toLowerCase();
        synchronized (headers) {
            List<String> values = headers.getOrDefault(name, new ArrayList<>());
            values.add(value);
        }
    }

    /**
     * 添加参数
     * @param name
     * @param values
     */
    @Override
    public void addParameter(String name, String[] values) {
        synchronized (parameters) {
            parameters.put(name, values);
        }
    }

    /**
     * 清空cookie
     */
    @Override
    public void clearCookies() {
        synchronized (cookies) {
            cookies.clear();
        }
    }

    /**
     * 清空header
     */
    @Override
    public void clearHeaders() {
        synchronized (headers) {
            headers.clear();
        }
    }

    /**
     * 清空本地环境信息
     */
    @Override
    public void clearLocales() {
        synchronized (locales) {
            locales.clear();
        }
    }

    /**
     * 清空参数
     */
    @Override
    public void clearParameters() {
        synchronized (parameters) {
            parameters.clear();
        }
    }

    /**
     * 设置认证类型
     * @param type
     */
    @Override
    public void setAuthType(String type) {
        this.authType = type;
    }

    /**
     * 设置context路径
     * @param path
     */
    @Override
    public void setContextPath(String path) {
        this.contextPath = path;
    }

    /**
     * 设置方法
     * @param method
     */
    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 设置请求行中的查询字符串
     * @param query
     */
    @Override
    public void setQueryString(String query) {
        this.queryString = query;
    }

    /**
     * 设置路径信息
     * @param path
     */
    @Override
    public void setPathInfo(String path) {
        this.pathInfo = path;
    }

    /**
     * session是否存在于cookie中
     * @param flag
     */
    @Override
    public void setRequestedSessionCookie(boolean flag) {
        this.requestedSessionCookie = flag;
    }

    /**
     * 设置session id
     * @param id
     */
    @Override
    public void setRequestedSessionId(String id) {
        this.requestedSessionId = id;
    }

    /**
     * session id 是否存在与url中
     * @param flag
     */
    @Override
    public void setRequestedSessionURL(boolean flag) {
        this.requestedSessionURL = flag;
    }

    /**
     * 设置请求中的uri
     * @param uri
     */
    @Override
    public void setRequestURI(String uri) {
        this.requestURI = uri;
    }

    /**
     * 设置解码后的uri
     * @param uri
     */
    @Override
    public void setDecodedRequestURI(String uri) {
        this.decodedRequestURI = uri;
    }

    /**
     * 返回解码后的uri
     * @return
     */
    @Override
    public String getDecodedRequestURI() {
        if (decodedRequestURI == null)
            decodedRequestURI = RequestUtil.URLDecode(getRequestURI());
        return this.decodedRequestURI;
    }

    /**
     * 设置servlet的路径
     * @param path
     */
    @Override
    public void setServletPath(String path) {
        this.servletPath = path;
    }

    /**
     * TODO 设置已对此请求进行认证的Principal
     * @param principal
     */
    @Override
    public void setUserPrincipal(Principal principal) {

    }
}
