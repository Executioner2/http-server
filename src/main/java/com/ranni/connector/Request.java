package com.ranni.connector;

import com.ranni.connector.http.ParameterMap;
import com.ranni.container.Context;
import com.ranni.container.Host;
import com.ranni.container.MappingData;
import com.ranni.container.session.Session;
import com.ranni.core.ApplicationMapping;
import com.ranni.coyote.CoyoteInputStream;
import com.ranni.util.FastHttpDateFormat;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 21:55
 */
public class Request implements HttpServletRequest {

    private static final String HTTP_UPGRADE_HEADER_NAME = "upgrade";
    
    private Connector connector; // 与此请求关联的连接器

    // 日期格式化
    private static final DateTimeFormatter formats[] = {
            DateTimeFormatter.ofPattern(FastHttpDateFormat.RFC1123_DATE, Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
            DateTimeFormatter.ofPattern("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            DateTimeFormatter.ofPattern("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };

    protected static final Locale defaultLocale = Locale.getDefault();
    protected final ArrayList<Locale> locales = new ArrayList<>(); // 与此请求关联的本地环境
    protected String authType; // 认证类型
    protected DispatcherType internalDispatcherType; // 转发类型
    protected final InputBuffer inputBuffer = new InputBuffer(); // 输入缓冲区
    protected CoyoteInputStream inputStream = new CoyoteInputStream(inputBuffer); // 输入流
    protected CoyoteReader reader = new CoyoteReader(inputBuffer); // 输入缓冲区读取器
    
    protected boolean usingInputStream; // 是使用了输入流
    protected boolean usingReader; // 是否使用了缓冲区读取器
    protected boolean parametersParsed; // 参数是否已经解析过了
    protected boolean cookiesParsed; // cookies是否已经解析过了
    protected boolean cookiesConverted; // 是否已经将cookies进行了转换
    protected boolean sslAttributesParsed; // 在SSL下的属性解析了吗
    protected boolean secure; // 安全标志位
    
    protected Cookie[] cookies;
    protected FilterChain filterChain; // 过滤链
    protected ParameterMap<String, String[]> parameterMap = new ParameterMap<>(); // 请求参数
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final transient HashMap<String, Object> notes = new HashMap<>(); // Catalina 组件和事件侦听器与此请求相关的内部注释。（不可被序列化）

    // post数据缓存
    protected static final int CACHED_POST_LEN = 8192;
    protected byte[] postData = null;    
    protected Collection<Part> parts = null; // 随请求上传的部分
    
    protected Exception partsParseException;  // 解析part抛出的异常（如果有异常的话）
    
    protected Session session; // Session
    protected Object requestDispatcherPath; // 转发路径
    protected boolean requestedSessionCookie; // 是否用cookie作为session的id
    protected boolean requestedSessionURL; // session id是否在URL中 
    protected String requestedSessionId; // 请求中的session id
    protected boolean requestedSessionSSL;

    protected boolean localesParsed; // 是否已经解析了本地环境
    protected int localPort = -1; // 本地端口
    protected String localAddr; // 本地地址
    protected String localName; // 本地名
    protected String remoteAddr; // 远程地址
    protected String remoteHost; // 远程主机
    protected int remotePort = -1; // 远程端口
    protected String peerAddr;

    private HttpServletRequest applicationRequest;
    protected com.ranni.coyote.Request coyoteRequest;
    protected RequestFacade facade; // HttpServletRequest请求的外观类

    // 映射数据
    protected final MappingData mappingData = new MappingData();
    private final ApplicationMapping applicationMapping = new ApplicationMapping(mappingData);


    // ------------------------------ 构造方法 ------------------------------
    
    public Request(Connector connector) {
        this.connector = connector;
    }
    

    // ------------------------------ 通用方法 ------------------------------

    protected void addPathParameter(String name, String value) {
        coyoteRequest.addPathParameter(name, value);
    }
    
    
    protected String getPathParameter(String name) {
        return coyoteRequest.getPathParameter(name);
    }


    /**
     * 释放所有对象引用并初始化值
     */
    public void recycle() {
        internalDispatcherType = null;
        requestDispatcherPath = null;

        authType = null;
        inputBuffer.recycle();
        usingInputStream = false;
        usingReader = false;
//        userPrincipal = null;
//        subject = null;
        parametersParsed = false;
        if (parts != null) {
            for (Part part: parts) {
                try {
                    part.delete();
                } catch (IOException ignored) {
                    // ApplicationPart.delete() never throws an IOEx
                }
            }
            parts = null;
        }
        partsParseException = null;
        locales.clear();
        localesParsed = false;
        secure = false;
        remoteAddr = null;
        peerAddr = null;
        remoteHost = null;
        remotePort = -1;
        localPort = -1;
        localAddr = null;
        localName = null;

        attributes.clear();
        sslAttributesParsed = false;
        notes.clear();

        recycleSessionInfo();
        recycleCookieInfo(false);

        if (getDiscardFacades()) {
            parameterMap = new ParameterMap<>();
        } else {
            parameterMap.setLocked(false);
            parameterMap.clear();
        }

        mappingData.recycle();
        applicationMapping.recycle();

        applicationRequest = null;
        if (getDiscardFacades()) {
            if (facade != null) {
                facade.clear();
                facade = null;
            }
            if (inputStream != null) {
                inputStream.clear();
                inputStream = null;
            }
            if (reader != null) {
                reader.clear();
                reader = null;
            }
        }

//        asyncSupported = null;
//        if (asyncContext!=null) {
//            asyncContext.recycle();
//        }
//        asyncContext = null;
    }


    /**
     * @return 是否重置外观对象
     */
    public boolean getDiscardFacades() {
        return (connector == null) ? true : connector.getDiscardFacades();
    }
    

    /**
     * 重置关于cookie操作的属性信息
     * 
     * @param recycleCoyote 是否重置cookie内的信息
     */
    protected void recycleCookieInfo(boolean recycleCoyote) {
        cookiesParsed = false;
        cookiesConverted = false;
        cookies = null;
        if (recycleCoyote) {
            getCoyoteRequest().getCookies().recycle();
        }
    }


    /**
     * 重置session信息
     */
    protected void recycleSessionInfo() {
        if (session != null) {
            try {
                session.endAccess();
            } catch (Throwable t) {
                throw t;
            }
        }

        session = null;
        requestedSessionCookie = false;
        requestedSessionId = null;
        requestedSessionURL = false;
        requestedSessionSSL = false;
    }


    
    public com.ranni.coyote.Request getCoyoteRequest() {
        return coyoteRequest;
    }
    
    
    public Connector getConnector() {
        return connector;
    }
    
    
    public Context getContext() {
        return mappingData.context;
    }
    
    
    public FilterChain getFilterChain() {
        return filterChain;    
    }
    
    
    public void setFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
    }
    
    
    public Host getHost() {
        return mappingData.host;
    }
    
    
    public MappingData getMappingData() {
        return mappingData;
    }
    
    
    public HttpServletRequest getRequest() {
        if (facade == null) {
            facade = new RequestFacade(this);
        }
        
        if (applicationRequest == null) {
            applicationRequest = facade;
        }
        
        return applicationRequest;
    }
    
    
    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        return 0;
    }

    @Override
    public String getMethod() {
        return null;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
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
    public void setAttribute(String name, Object o) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
