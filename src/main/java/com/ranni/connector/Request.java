package com.ranni.connector;

import com.ranni.common.Globals;
import com.ranni.container.Context;
import com.ranni.container.Host;
import com.ranni.container.MappingData;
import com.ranni.container.Wrapper;
import com.ranni.container.session.Manager;
import com.ranni.container.session.Session;
import com.ranni.core.ApplicationMapping;
import com.ranni.core.ApplicationSessionCookieConfig;
import com.ranni.coyote.ActionCode;
import com.ranni.coyote.Constants;
import com.ranni.coyote.CoyoteInputStream;
import com.ranni.util.RequestUtil;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.CookieProcessor;
import com.ranni.util.http.FastHttpDateFormat;
import com.ranni.util.http.Parameters;
import com.ranni.util.http.ServerCookies;
import com.ranni.util.http.parse.AcceptLanguage;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;
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
 * @Ref org.apache.catalina.connector.Request
 */
public class Request implements HttpServletRequest {

    private static final String HTTP_UPGRADE_HEADER_NAME = "upgrade";
    

    /**
     * 与此请求关联的连接器
     */
    protected Connector connector;

    /**
     * 日期格式化
     */
    private static final DateTimeFormatter formats[] = {
            DateTimeFormatter.ofPattern(FastHttpDateFormat.RFC1123_DATE, Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
            DateTimeFormatter.ofPattern("EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            DateTimeFormatter.ofPattern("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };

    /**
     * 默认的请求解析语言
     */
    protected static final Locale defaultLocale = Locale.getDefault();

    /**
     * 此请求的解析语言。请求头的Accept-Language参数
     */
    protected final ArrayList<Locale> locales = new ArrayList<>();

    /**
     * 认证类型
     */
    protected String authType;

    /**
     * 转发类型
     */
    protected DispatcherType internalDispatcherType;

    /**
     * 输入缓冲区
     */
    protected final InputBuffer inputBuffer = new InputBuffer();

    /**
     * 输入流
     */
    protected CoyoteInputStream inputStream = new CoyoteInputStream(inputBuffer);

    /**
     * 输入缓冲区读取器
     */
    protected CoyoteReader reader = new CoyoteReader(inputBuffer);

    /**
     * 是使用了输入流
     */
    protected boolean usingInputStream;

    /**
     * 是否使用了缓冲区读取器
     */
    protected boolean usingReader;

    /**
     * 参数是否已经解析过了
     */
    protected boolean parametersParsed;

    /**
     * cookies是否已经解析过了
     */
    protected boolean cookiesParsed;

    /**
     * 是否已经将cookies进行了转换
     */
    protected boolean cookiesConverted;

    /**
     * 在SSL下的字段解析了吗
     */
    protected boolean sslAttributesParsed;

    /**
     * 安全标志位
     */
    protected boolean secure;


    /**
     * cookie集合
     */
    protected Cookie[] cookies;

    /**
     * 过滤链
     */
    protected FilterChain filterChain;

    /**
     * 请求参数
     */
    protected ParameterMap<String, String[]> parameterMap = new ParameterMap<>();

    /**
     * 请求实例的属性集合
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Catalina 组件和事件侦听器与此请求相关的内部注释。（不可被序列化）
     */
    private final transient HashMap<String, Object> notes = new HashMap<>();

    /**
     * post数据（请求体数据）缓存大小
     */
    protected static final int CACHED_POST_LEN = 8192;

    /**
     * post数据（请求体数据）缓存
     */
    protected byte[] postData = null;

    /**
     * 随请求上传的部分
     */
    protected Collection<Part> parts = null;

    /**
     * 解析part抛出的异常（如果有异常的话）
     */
    protected Exception partsParseException;

    /**
     * Session
     */
    protected Session session;

    /**
     * 转发路径
     */
    protected Object requestDispatcherPath;

    /**
     * 是否用cookie作为session的id
     */
    protected boolean requestedSessionCookie;

    /**
     * session id是否在URL中
     */
    protected boolean requestedSessionURL;

    /**
     * 请求中的session id
     */
    protected String requestedSessionId;

    /**
     * 安全套接字中的session id
     */
    protected boolean requestedSessionSSL;

    /**
     * URI字节转URI字符的转换器
     */
    protected B2CConverter URIConverter;

    /**
     * 是否已经解析了处理语言环境（请求头中的accept-language字段）
     */
    protected boolean localesParsed;

    /**
     * 接收这个请求的服务器端口号
     */
    protected int localPort = -1;

    /**
     * 接收这个请求的服务器IP
     */
    protected String localAddr;

    /**
     * 接收这个请求的服务器名
     */
    protected String localName;

    /**
     * 与此请求关联的远程客户端IP地址
     */
    protected String remoteAddr;

    /**
     * 此请求关联的远程客户端端口
     */
    protected int remotePort = -1;

    /**
     * 远程主机域名
     */
    protected String remoteHost;
    
    protected String peerAddr;

    /**
     * webapp的全局上下文作用域
     */
    private HttpServletRequest applicationRequest;

    /**
     * coyoteRequest
     */
    protected com.ranni.coyote.Request coyoteRequest;

    /**
     * 响应对象
     */
    protected Response response;

    /**
     * HttpServletRequest请求的外观类
     */
    protected RequestFacade facade;

    /**
     * 映射数据（取代了tomcat 4中的mapper）
     */
    protected final MappingData mappingData = new MappingData();

    /**
     * 映射数据（取代了tomcat 4中的mapper）
     */
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
     * 重置关于cookie操作的字段信息
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


    /**
     * @return 返回面向应用的请求实例
     */
    public HttpServletRequest getRequest() {
        if (facade == null) {
            facade = new RequestFacade(this);
        }
        
        if (applicationRequest == null) {
            applicationRequest = facade;
        }
        
        return applicationRequest;
    }


    /**
     * 设置面向应用的请求实例。应用请求实例，如果它同时也是
     * HttpServletRequestWrapper实例，则直到取到最内层的请求实例。如果最终
     * 取出来的请求实例不是当前请求实例的外观对象，就抛出异常
     * 
     * @param applicationRequest 传入的面向应用的请求实例
     */
    public void setRequest(HttpServletRequest applicationRequest) {
        ServletRequest r = applicationRequest;
        
        while (r instanceof HttpServletRequestWrapper) {            
            r = ((HttpServletRequestWrapper) r).getRequest();
        }
        
        if (r != facade) {
            throw new IllegalArgumentException("request.illegalWrap");
        }
        this.applicationRequest = applicationRequest;
    } 
    
    
    public Response getResponse() {
        return response;
    }
    
    
    public void setResponse(Response response) {
        this.response = response;
    }
    
    
    public InputStream getStream() {
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }
        return inputStream;
    }
    
    
    protected B2CConverter getURIConverter() {
        return URIConverter;
    }
    
    
    protected void setURIConverter(B2CConverter URIConverter) {
        this.URIConverter = URIConverter; 
    }
    
    
    public Wrapper getWrapper() {
        return mappingData.wrapper;
    }


    // ------------------------------ 请求处理方法 ------------------------------
    
    public ServletInputStream createInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }
        return inputStream;
    }


    /**
     * 只有当响应状态为413时才会调用checkSwallowInput方法
     * 产生作用
     * 
     * @throws IOException 可能抛出I/O异常
     */
    public void finishRequest() throws IOException {
        if (response.getStatus() == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
            checkSwallowInput();
        }
    }


    /**
     * 如果有Context容器且Context容器开启了
     * 请求大小边界限制的，就触发禁止大数据吞吐
     * 的钩子。（例如控制大文件上传）
     */
    protected void checkSwallowInput() {
        Context context = getContext();
        if (context != null && !context.getSwallowAbortedUploads()) {
            coyoteRequest.action(ActionCode.DISABLE_SWALLOW_INPUT, null);
        }
    }


    /**
     * Request的内部便签
     * 
     * @param name 便签名
     * @return 返回便签
     */
    public Object getNode(String name) {
        return notes.get(name);
    }


    /**
     * 移除Request的内部便签
     * 
     * @param name 便签名
     */
    public void removeNote(String name) {
        notes.remove(name);
    }


    /**
     * 添加Request的内部便签
     * 
     * @param name 便签名
     * @param node 便签
     */
    public void setNote(String name, Object node) {
        notes.put(name, node);
    }


    /**
     * 处理这个请求的服务器端口号
     * 
     * @param port 服务器端口号
     */
    public void setLocalPort(int port) {
        localPort = port;
    }
    
    
    /**
     * 设置与此请求关联的远程客户端IP地址
     * 
     * @param remoteAddr 远程客户端IP地址
     */
    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }


    /**
     * 设置与此请求关联的远程客户端主机域名（全限定名）
     * 
     * @param remoteHost 远程客户端主机域名
     */
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }


    /**
     * 设置远程客户端的端口号
     * 
     * @param remotePort 客户端的端口号
     */
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }


    /**
     * 设置安全检查标志
     * 
     * @param secure 安全标志
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    

    /**
     * 设置服务器端口号来处理这个请求
     * 
     * @param port 服务器端口号
     */
    public void setServerPort(int port) {
        coyoteRequest.setServerPort(port);
    }


    // ------------------------------ ServletRequest Methods ------------------------------    


    /**
     * @return 返回此请求的认证类型
     */
    @Override
    public String getAuthType() {
        return authType;
    }


    /**
     * @return 返回所有的cookie
     */
    @Override
    public Cookie[] getCookies() {
        return cookies;
    }


    /**
     * 根据传入的name返回请求头中的日期
     * 
     * @param name 与日期关联的字段名
     * @return 返回解析后的长整型值
     */
    @Override
    public long getDateHeader(String name) {
        String val = getHeader(name);
        if (val == null) {
            return  -1;
        }
        
        long result = FastHttpDateFormat.parseDate(val);
        if (result != -1L) {
            return result;
        }
        
        throw new IllegalArgumentException(val);
    }


    /**
     * 从请求头中取得字段值
     * 
     * @param name 字段名
     * @return 返回字段值
     */
    @Override
    public String getHeader(String name) {
        return coyoteRequest.getHeader(name);
    }


    /**
     * 取得请求头中字段name对应的字段值的迭代器
     * 
     * @param name 字段名
     * @return 返回的字段值的迭代器
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        return coyoteRequest.getMimeHeaders().values(name);
    }


    /**
     * @return 返回请求头中字段值的迭代器
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        return coyoteRequest.getMimeHeaders().names();
    }


    /**
     * 返回指定字段名对应的整型字段值
     * 
     * @param name 字段名
     * @return 返回整型的字段值
     */
    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return 0;
        }
        
        return Integer.parseInt(value);
    }


    @Override
    public HttpServletMapping getHttpServletMapping() {
        return applicationMapping.getHttpServletMapping();
    }
    

    /**
     * @return 返回HTTP请求方法
     */
    @Override
    public String getMethod() {
        return coyoteRequest.method().toString();
    }


    /**
     * @return 返回请求的路径信息
     */
    @Override
    public String getPathInfo() {
        return mappingData.pathInfo.toString();
    }


    /**
     * @return 返回此请求的真实路径
     */
    @Override
    public String getPathTranslated() {
        Context context = getContext();
        
        if (context == null || getPathInfo() == null) {
            return null;
        }
                
        return context.getServletContext().getRealPath(getPathInfo());
    }


    /**
     * @return 返回容器路径
     */
    @Override
    public String getContextPath() {
        return null;
    }


    /**
     * @return 返回查询字符串
     */
    @Override
    public String getQueryString() {
        return coyoteRequest.queryString().toString();
    }
    
    
    /**
     * @return 返回请求路径的缓冲区
     */
    public MessageBytes getRequestPathMB() {
        return mappingData.requestPath;
    }

    
    /**
     * @return 返回请求包中的session id
     */
    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }


    /**
     * @return 返回请求行中的请求URI
     */
    @Override
    public String getRequestURI() {
        return coyoteRequest.requestURI().toString();
    }


    /**
     * @return 返回请求包的URL
     */
    @Override
    public StringBuffer getRequestURL() {
        return RequestUtil.getRequestURL(this);
    }


    /**
     * @return 返回请求的Servlet路径
     */
    @Override
    public String getServletPath() {
        return mappingData.wrapperPath.toString();
    }


    /**
     * 返回session
     * 
     * @param create 如果不存在是否新建一个session
     * @return 返回session
     */
    @Override
    public HttpSession getSession(boolean create) {
        Session session = doGetSession(create);
        if (session == null) {
            return null;
        }
        
        return session.getSession();
    }

    
    public Session getSessionInternal() {
        return doGetSession(true);
    }
    

    /**
     * 取得session，如果不存在，根据create来决定是否创建
     */
    protected Session doGetSession(boolean create) {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        // 如果session存在但失效了就返回null，否则返回session
        if (session != null) {
            if (!session.isValid()) {
                session = null;
            } else {
                return session;
            }
        }

        // 取得session管理器
        Manager manager = context.getManager();

        // 先根据session id查询
        if (requestedSessionId != null) {
            session = manager.findSession(requestedSessionId);
            if (session != null && !session.isValid())
                session = null;
            else if (session != null)
                return session;
        }

        // 是否创建新的session
        if (!create)
            return null;
        if (context != null && response != null && context.getCookies()
                && response.getResponse().isCommitted()) {
            throw new IllegalStateException("HttpRequestBase.doGetSession  响应已经提交！");
        }

        session = manager.createSession();

        if (session == null)
            return null;

        return session;
    }


    /**
     * 取得session，如果不存在就创建一个
     */
    @Override
    public HttpSession getSession() {
        return getSession(true);
    }


    /**
     * 更改session id
     * 
     * @return 返回新的session id
     */
    @Override
    public String changeSessionId() {
        Session session = doGetSession(false);
        if (session == null) {
            throw new IllegalStateException("不存在session");
        }

        Manager manager = this.getContext().getManager();
        String newSessionId = manager.rotateSessionId(session);
        changeSessionId(newSessionId);
        
        return newSessionId;
    }


    /**
     * 修改session id
     * 
     * @param sessionId 新的session id
     */
    public void changeSessionId(String sessionId) {
        if (requestedSessionId != null && requestedSessionId.length() > 0) {
            requestedSessionId = sessionId;
        }

        Context context = getContext();
        if (context != null && !context
                .getServletContext()
                .getEffectiveSessionTrackingModes()
                .contains(SessionTrackingMode.COOKIE)) {
            
            return;
        }
        
        if (response != null) {
            Cookie newCookie = ApplicationSessionCookieConfig.createSessionCookie(context,
                    sessionId, isSecure());
            
            response.addSessionCookieInternal(newCookie);
        }
    }


    /**
     * @return 如果返回true，则表示请求参数已经解析了。否则反之。
     */
    public boolean isParametersParsed() {
        return parametersParsed;
    }


    /**
     * @return 如果返回true，则表示已经读取了所有的请求正文
     */
    public boolean isFinished() {
        return coyoteRequest.isFinished();
    }


    /**
     * @return 如果返回true，则表示session id还有效
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        if (requestedSessionId == null) {
            return false;
        }

        Context context = getContext();
        if (context == null) {
            return false;
        }

        Manager manager = context.getManager();
        if (manager == null) {
            return false;
        }
        
        Session session = null;
        try {
            session = manager.findSession(requestedSessionId);
        } catch (Exception e) {
            ;
        }

        if (session == null || !session.isValid()) {
             if (getMappingData().contexts == null) {
                 return false;
             } else {
                 for (int i = getMappingData().contexts.length; i > 0; i--) {
                     Context ctt = getMappingData().contexts[i - 1];
                     try {
                         if (ctt.getManager().findSession(requestedSessionId) != null) {
                             return true;
                         }
                     } catch (Exception e) {
                         ;
                     }
                     
                     return false;
                 }
             }
        }
        
        return true;
    }

    
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        if (requestedSessionId == null) {
            return false;
        }
        
        return requestedSessionCookie;
    }

    
    @Override
    public boolean isRequestedSessionIdFromURL() {
        if (requestedSessionId == null) {
            return false;
        }
        
        return requestedSessionURL;
    }

    
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }
    
    
    /**
     * 返回此请求中包含的所有上传文件
     * 
     * @return 返回此请求中包含的所有上传文件
     * @throws IOException 可能抛出I/O异常
     * @throws ServletException 可能抛出Servlet异常
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        parseParts(true);

        if (partsParseException != null) {
            if (partsParseException instanceof IOException) {
                throw (IOException) partsParseException;
            } else if (partsParseException instanceof IllegalStateException) {
                throw (IllegalStateException) partsParseException;
            } else if (partsParseException instanceof ServletException) {
                throw (ServletException) partsParseException;
            }
        }
        
        return parts;
    }


    @Override
    public Part getPart(String name) throws IOException, ServletException {
        for (Part part : getParts()) {
            if (name.equals(part.getName())) {
                return part;
            }
        }
        return null;
    }


    /**
     * 根据属性名取得属性值
     * 
     * @param name 属性名
     * @return 返回属性值
     */
    @Override
    public Object getAttribute(String name) {
        // XXX - 特殊属性

        Object attr = attributes.get(name);
        if (attr != null) {
            return attr;
        }

        attr = coyoteRequest.getAttribute(name);
        if (attr != null) {
            return attr;
        }
        
        // XXX - SSL层中拿取
        
        return attr;
    }

    
    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }


    /**
     * @return 返回解码格式
     */
    @Override
    public String getCharacterEncoding() {
        String ce = coyoteRequest.getCharacterEncoding();
        if (ce != null) {
            return ce;
        }

        Context context = getContext();
        if (context != null) {
            return context.getRequestCharacterEncoding();
        }

        return null;
    }


    /**
     * @return 返回解码器
     */
    private Charset getCharset() {
        Charset charset = null;
        try {
            charset = coyoteRequest.getCharset();
        } catch (UnsupportedEncodingException e) {
            ;
        }

        if (charset != null) {
            return charset;
        }

        Context context = getContext();
        if (context != null) {
            String encoding = context.getRequestCharacterEncoding();
            if (encoding != null) {
                try {
                    return B2CConverter.getCharset(encoding);
                } catch (UnsupportedEncodingException e) {
                    ;
                }
            }
        }

        return Constants.DEFAULT_BODY_CHARSET;
    }


    /**
     * 设置字符解码格式
     * 
     * @param env 解码格式
     * @throws UnsupportedEncodingException
     */    
    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        if (usingReader) {
            return;
        }

        Charset charset = B2CConverter.getCharset(env);
        coyoteRequest.setCharset(charset);
    }


    /**
     * @return 返回请求体长度
     */
    @Override
    public int getContentLength() {
        return coyoteRequest.getContentLength();
    }

    
    /**
     * @return 返回请求体长度（长整型）
     */
    @Override
    public long getContentLengthLong() {
        return coyoteRequest.getContentLengthLong();
    }


    /**
     * @return 返回请求体类型
     */
    @Override
    public String getContentType() {
        return coyoteRequest.getContentType();
    }


    /**
     * 返回此请求的输入流（实际上是一个输入缓冲区转换为的输入流）
     * 
     * @return 返回的输入流
     * 
     * @throws IOException 可能抛出I/O异常
     * @throws IllegalStateException 如果已经使用了读取器（这个缓冲区通过
     *         读取器进行数据处理），那么将抛出此状态异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (usingReader) {
            throw new IllegalStateException("coyoteRequest.getInputStream.ise");
        }
        
        usingInputStream = true;
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }
        
        return inputStream;
    }


    /**
     * 返回参数名对应的参数值
     * 
     * @param name 参数名
     * @return 返回参数值
     */
    @Override
    public String getParameter(String name) {
        if (!parametersParsed) {
            parseParameters();
        }
        
        return coyoteRequest.getParameters().getParameter(name);
    }


    /**
     * 参数解析
     */
    protected void parseParameters() {
        parametersParsed = true;

        Parameters parameters = coyoteRequest.getParameters();
        boolean success = false;
        
        try {
            parameters.setLimit(getConnector().getMaxParameterCount());

            Charset charset = getCharset();

            boolean useBodyEncodingForURI = connector.getUseBodyEncodingForURI();
            parameters.setCharset(charset);
            if (useBodyEncodingForURI) {
                parameters.setQueryStringCharset(charset);
            }

            // 解析查询参数
            parameters.handleQueryParameters();

            if (usingInputStream || usingReader) {
                success = true;
                return;
            }

            // 没有使用输入流或者读取器就要在这里将解析出来的查询参数进行处理
            String contentType = getContentType();
            if (contentType == null) {
                contentType = "";
            }

            int semicolon = contentType.indexOf(";");
            if (semicolon >= 0) {
                contentType = contentType.substring(0, semicolon).trim();
            } else {
                contentType = contentType.trim();
            }

            if ("multipart/form-data".equals(contentType)) {
                // multipart的不编码数据（html的form表单可通过此格式提交附加文件）
                parseParts(false); // 解析文件
                success = true;
                return;
            }

            // 如果不支持解析请求体，就不再进行后续的解析
            if (!getConnector().isParseBodyMethod(getMethod())) {
                success = true;
                return;
            }

            // 不是标准的post表单数据内容类型
            if (!("application/x-www-form-urlencoded").equals(contentType)) {
                success = true;
                return;
            }

            // 开始解析请求体
            int len = getContentLength();

            if (len > 0) {
                // 可以确定请求体长度的请求体解析
                int maxPostSize = connector.getMaxPostSize();
                if (maxPostSize >= 0 && len > maxPostSize) {
                    // XXX - 日志打印
                    checkSwallowInput();
                    parameters.setParseFailedReason(Parameters.FailReason.POST_TOO_LARGE);
                    return;
                }
                
                byte[] formData = null;
                if (len < CACHED_POST_LEN) {
                    if (postData == null) {
                        postData = new byte[CACHED_POST_LEN];
                    }
                    formData = postData;
                } else {
                    formData = new byte[len];
                }
                
                try {
                    // 读取请求体中的数据
                    if (readPostBody(formData, len) != len) {
                        parameters.setParseFailedReason(Parameters.FailReason.REQUEST_BODY_INCOMPLETE);
                        return;
                    }
                } catch (IOException e) {
                    // XXX - 日志打印
                    parameters.setParseFailedReason(Parameters.FailReason.CLIENT_DISCONNECT);
                    return;
                }

                // 解析请求体
                parameters.processParameters(formData, 0, len);
                
            } else if ("chunked".equalsIgnoreCase(coyoteRequest
                    .getHeader("transfer-encoding"))) {
                // 对发送请求时无法确定请求体长度的请求体解析
                byte[] formData = null;
                try {
                    formData = readChunkedPostBody(); // 读取请求体分块
                    
                } catch (IllegalStateException ise) {
                    // 超出最大限制
                    // XXX - 日志打印
                    parameters.setParseFailedReason(Parameters.FailReason.POST_TOO_LARGE);
                    return;
                    
                } catch (IOException e) {
                    // 客户端关闭
                    // XXX - 日志打印
                    parameters.setParseFailedReason(Parameters.FailReason.CLIENT_DISCONNECT);
                    return;
                }

                if (formData != null) {
                    parameters.processParameters(formData, 0, len);
                }
            } // if end

            success = true;
            
        } finally {
            if (!success) {
                parameters.setParseFailedReason(Parameters.FailReason.UNKNOWN);
            }
        }
        
    }


    /**
     * 读取客户端发来的部分请求体中的数据
     * 
     * @return 返回读取的数据
     */
    protected byte[] readChunkedPostBody() throws IOException {
        ByteChunk body = new ByteChunk();

        byte[] buffer = new byte[CACHED_POST_LEN];
        
        int len = 0;
        while (len > -1) { // 循环读取到暂时没有数据或触发异常
            len = getStream().read(buffer, 0, CACHED_POST_LEN);

            // 如果超出了最大限制，就触发大文件钩子并抛出异常
            if (connector.getMaxPostSize() >= 0
                && body.getLength() + len > connector.getMaxPostSize()) {
                
                checkSwallowInput();
                throw new IllegalStateException("coyoteRequest.chunkedPostTooLarge");
            }
            
            if (len > 0) {
                body.append(buffer, 0, len);
            }
        }
        
        if (body.getLength() == 0) {
            return null;
        }
        
        if (body.getLength() < body.getBuffer().length) {
            int length = body.getLength();
            byte[] result = new byte[length];
            System.arraycopy(body.getBuffer(), 0, result, 0, length);
            return result;
        }
        
        return body.getBuffer();
    }


    /**
     * 读取请求体中的数据
     * 
     * @param body 要存入的数组
     * @param len 数据的长度
     * @return 返回读取的数据长度
     * @throws IOException 可能抛出I/O异常
     */
    protected int readPostBody(byte[] body, int len) throws IOException {
        int offset = 0;
        
        do {
            int readLen = getStream().read(body, offset, len - offset);
            if (readLen <= 0) {
                return offset;
            }
            offset += readLen;
            
        } while (len - offset > 0);
        
        return len;
    }


    /**
     * @return 返回所有参数的参数名
     */
    @Override
    public Enumeration<String> getParameterNames() {
        if (!parametersParsed) {
            parseParameters();
        }
        
        return coyoteRequest.getParameters().getParameterNames();
    }


    /**
     * 返回指定参数名的参数值
     * 
     * @param name 参数名
     * @return 返回的参数值集合
     */
    @Override
    public String[] getParameterValues(String name) {
        if (!parametersParsed) {
            parseParameters();
        }
        
        return coyoteRequest.getParameters().getParameterValues(name);
    }


    /**
     * @return 返回参数集合
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        if (parameterMap.isLocked()) {
            return parameterMap;
        }

        Enumeration<String> parameterNames = getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = getParameterValues(name);
            parameterMap.put(name, values);
        }

        return parameterMap;
    }


    /**
     * @return 返回协议和协议版本
     */
    @Override
    public String getProtocol() {
        return coyoteRequest.protocol().toString();
    }


    /**
     * @return 返回协议类型（http还是https）
     */
    @Override
    public String getScheme() {
        return coyoteRequest.scheme().toString();
    }


    /**
     * 这个值的获取是在请求包中取出来的
     * 
     * @return 返回响应此请求的服务器名
     */
    @Override
    public String getServerName() {
        return coyoteRequest.serverName().toString();
    }


    /**
     * 这个值的获取是在请求包中取出来的
     * 
     * @return 返回响应此请求的服务器端口
     */
    @Override
    public int getServerPort() {
        return coyoteRequest.getServerPort();
    }


    /**
     * 取得读取器。以读取器的方式处理输入缓冲区。
     * 
     * @return 返回读取器
     * 
     * @throws IOException 可能抛出I/O有异常
     * @throws IllegalStateException 如果输入缓冲区已经以输入流
     *         的方式被使用了，将抛出此异常
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (usingInputStream) {
            throw new IllegalStateException("coyoteRequest.getReader.ise");
        }

        // 设置解码方式
        if (coyoteRequest.getCharacterEncoding() == null) {
            Context context = getContext();
            if (context != null) {
                String enc = context.getRequestCharacterEncoding();
                if (enc != null) {
                    setCharacterEncoding(enc);
                }
            }
        }
        
        usingInputStream = true;
        if (reader == null) {
            reader = new CoyoteReader(inputBuffer);
        }
        
        return reader;
    }


    /**
     * 返回客户端ip地址。如果不存在，
     * 则触发请求主机ip属性的钩子
     * 
     * @return 返回客户端ip地址
     */
    @Override
    public String getRemoteAddr() {
        if (remoteAddr == null) {
            coyoteRequest.action(ActionCode.REQ_HOST_ADDR_ATTRIBUTE, coyoteRequest);
            remoteAddr = coyoteRequest.remoteAddr().toString();
        }
        return remoteAddr;
    }


    /**
     * 返回客户端主机域名。如果不存在，
     * 则触发请求客户端主机域名属性的
     * 钩子
     * 
     * @return 返回客户端主机域名
     */
    @Override
    public String getRemoteHost() {
        if (remoteHost == null) {
            if (!connector.getEnableLookups()) {
                // 如果不允许dns解析，那么ip地址就是主机域名
                remoteHost = getRemoteAddr(); 
            } else {
                // 触发域名解析的钩子
                coyoteRequest.action(ActionCode.REQ_HOST_ATTRIBUTE, coyoteRequest);
                remoteHost = coyoteRequest.remoteHost().toString();
            }
        }
        
        return remoteHost;
    }


    /**
     * 设置属性。属性值为null将会移除该属性名的键值对
     * 
     * @param name 属性名
     * @param value 属性值
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("coyoteRequest.setAttribute.namenull");
        }

        if (value == null) {
            removeAttribute(name);
            return;
        }
        
        // XXX - 特殊属性
        
        if (Globals.IS_SECURITY_ENABLED
            && name.equals(Globals.SENDFILE_FILENAME_ATTR)) {
            
            String canonicalPath;

            try {
                canonicalPath = new File(value.toString()).getCanonicalPath();
            } catch (IOException e) {
                throw new SecurityException("coyoteRequest.sendfileNotCanonical" + value);
            }
            
            System.getSecurityManager().checkRead(canonicalPath);
            value = canonicalPath;
        }

        Object oldValue = attributes.put(name, value);
        
        if (name.startsWith("com.ranni.")) {
            coyoteRequest.setAttribute(name, value);
        }
        
        // XXX - 属性添加通知

    }


    /**
     * 移除属性
     * 
     * @param name 属性名
     */
    @Override
    public void removeAttribute(String name) {
        if (name.startsWith("com.ranni.")) {
            coyoteRequest.getAttributes().remove(name);
        }

        boolean b = attributes.containsKey(name);
        if (b) {
            Object value = attributes.get(name);
            attributes.remove(name);
            
            // XXX - 属性删除通知
        }
    }


    /**
     * @return 如果没解析，先解析请求头的Accept-Language参数。选取
     *         这个参数的第一个值作为请求解析的首选语言。如果不存在，
     *         则返回默认的请求解析语言。
     */
    @Override
    public Locale getLocale() {
        if (!localesParsed) {
            parseLocales();
        }
        
        if (locales.size() > 0) {
            return locales.get(0);
        }
        
        return defaultLocale;
    }


    /**
     * 解析处理此请求的语言。（解析请求头的Accept-Language属性）<br/>
     * 例子：<br/>
     * Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6<br/>
     * <br/>
     * zh-CN,zh;q=0.9：zh表示中文（简体中文或繁体中文），zh-CN表示简体中文，q=0.9表示权重<br/>
     * <br/>
     * 例子说明：<br/>
     * <ul>
     *  <li>优先使用权重为0.9的中文中的简体中文；</li>
     *  <li>如果没有，使用权重0.8的英文；</li>
     *  <li>如果没有，使用权重0.7的英式英文；</li>
     *  <li>如果没有，使用权重0.6的美式英文。</li>
     * </ul>
     */
    protected void parseLocales() {
        localesParsed = true;
        
//        TreeMap<Double, ArrayList<Locale>> locales = new TreeMap<>((key1, key2) -> (int) (key2 - key1)); // 转整型忽略小数会有问题，因此不传入比较器，而是直接令key全都变为相反数
        TreeMap<Double, ArrayList<Locale>> locales = new TreeMap<>();
        Enumeration<String> values = getHeaders("accept-language");
        // 按上面的例子，这里取出来的values迭代器的第一个值就是：zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6
        
        // 解析值
        while (values.hasMoreElements()) {
            String value = values.nextElement();
            parseLocalesHeader(value, locales);
        }
        
        for (ArrayList<Locale> item : locales.values()) {
            for (Locale locale : item) {
                addLocale(locale);
            }
        }
        
    }


    /**
     * 添加请求处理语言
     * 
     * @param locale 添加的处理语言
     */
    public void addLocale(Locale locale) {
        locales.add(locale);
    }


    /**
     * 解析此请求的处理语言并按权重存放到传入的tree型map中
     * 
     * @param value accept-language的值
     * @param locales 要存入的TreeMap
     */
    private void parseLocalesHeader(String value, TreeMap<Double, ArrayList<Locale>> locales) {
        List<AcceptLanguage> acceptLanguages = null; // 语言处理器

        try {
            // 解析accept-language的值
            acceptLanguages = AcceptLanguage.parse(new StringReader(value));
        } catch (IOException e) {
            return;
        }
        
        // 按权重放入locales中
        for (AcceptLanguage acceptLanguage : acceptLanguages) {
            Double key = -acceptLanguage.getQuality(); // 变为负数使得成为期望的升序排序
            ArrayList<Locale> values = locales.get(key);
            if (values == null) {
                values = new ArrayList<>();
                locales.put(key, values);
            }
            values.add(acceptLanguage.getLocale());
        }
    }


    /**
     * @return 返回请求处理语言的迭代器
     */
    @Override
    public Enumeration<Locale> getLocales() {
        if (!localesParsed) {
            parseLocales();
        }
        
        if (locales.size() > 0) {
            return Collections.enumeration(locales);
        }

        ArrayList<Locale> res = new ArrayList<>();
        res.add(defaultLocale);
        return Collections.enumeration(res);
    }


    /**
     * @return 返回安全标志位
     */
    @Override
    public boolean isSecure() {
        return secure;
    }


    /**
     * @return 返回虚拟路径转换后的真实路径
     *
     * @param path 要转换的路径
     *             
     * @deprecated 在Servlet API 2.1版本，将使用
     *  <code>ServletContext.getRealPath()</code>
     */
    @Deprecated
    @Override
    public String getRealPath(String path) {
        Context context = getContext();

        if (context == null) {
            return null;
        }

        ServletContext servletContext = context.getServletContext();
        
        try {
            return servletContext.getRealPath(path);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    /**
     * @return 返回发起请求的主机端口或者最后一个代理主机的端口
     */
    @Override
    public int getRemotePort() {
        if (remotePort == -1) {
            coyoteRequest.action(ActionCode.REQ_REMOTEPORT_ATTRIBUTE, coyoteRequest);
            remotePort = coyoteRequest.getRemotePort();
        }

        return remotePort;
    }


    /**
     * 返回接收此请求的服务器名。说直白点就是与服务器通信的
     * 那个信道（channel）中的服务器名，如果没有，就会把
     * IP地址作为服务器名<br>
     * 
     * 具体的实现方法：<br>
     * @see com.ranni.util.net.Nio2Endpoint.Nio2SocketWrapper#populateLocalName()
     * 
     * @return 返回接收此请求的服务器名。
     */
    @Override
    public String getLocalName() {
        if (localName == null) {
            coyoteRequest.action(ActionCode.REQ_LOCAL_NAME_ATTRIBUTE, coyoteRequest);
            localName = coyoteRequest.localName().toString();
        }
        
        return localName;
    }


    /**
     * 返回处理此请求的服务器IP地址。说直白点就是与服务器通信的
     * 那个信道（channel）中的服务器IP地址<br>
     * 
     * 具体的实现方法：<br>
     * @see com.ranni.util.net.Nio2Endpoint.Nio2SocketWrapper#populateLocalAddr()
     * 
     * @return 返回处理此请求的服务器IP地址
     */
    @Override
    public String getLocalAddr() {
        if (localAddr == null) {
            coyoteRequest.action(ActionCode.REQ_LOCAL_ADDR_ATTRIBUTE, coyoteRequest);
            localAddr = coyoteRequest.localAddr().toString();
        }
        
        return localAddr;
    }


    /**
     * 返回处理此请求的服务器端口号。说直白点就是与服务器通信的
     * 那个信道（channel）中的服务器端口号<br>
     * 
     * 具体的实现方法：<br>
     * @see com.ranni.util.net.Nio2Endpoint.Nio2SocketWrapper#populateLocalPort()
     * 
     * @return 返回处理此请求的服务器端口号
     */
    @Override
    public int getLocalPort() {
        if (localPort == -1) {
            coyoteRequest.action(ActionCode.REQ_LOCALPORT_ATTRIBUTE, coyoteRequest);
            localPort = coyoteRequest.getLocalPort();
        }
        
        return localPort;
    }


    /**
     * @return 返回此请求关联的webapp的全局上下文作用域
     */
    @Override
    public ServletContext getServletContext() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        ServletContext servletContext = context.getServletContext();

        return servletContext;
    }


    /**
     * @return 返回此请求的调度类型。（如普通请求，转发请求等类型）
     * 
     * @see DispatcherType
     */
    @Override
    public DispatcherType getDispatcherType() {
        if (internalDispatcherType == null) {
            return DispatcherType.REQUEST;
        }

        return this.internalDispatcherType;
    }


    // TODO ------------------------------ 未实现 ------------------------------

    /**
     * 异步响应容器
     *
     * @return 返回异步响应容器
     * @throws IllegalStateException
     */
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
    
    
    /**
     * 取得请求转发器
     *
     * @param path 转发路径
     * @return 返回请求转发器
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }
    
    
    /**
     * 解析请求包携带的文件
     *
     * @param explicit 如果不能从wrapper获取到MultipartConfigElement，
     *                 且Context容器不允许进行Multipart解析。那么，参数
     *                 为true则将抛出异常，否则赋值parts为一个空的集合
     */
    private void parseParts(boolean explicit) {
        if (parts != null || partsParseException != null) {
            return;
        }

        Context context = getContext();
        MultipartConfigElement mce = getWrapper().getMultipartConfigElement();

        if (mce == null) {
            if (context.getAllowCasualMultipartParsing()) {
                mce = new MultipartConfigElement(null, connector.getMaxPostSize(),
                        connector.getMaxPostSize(), connector.getMaxPostSize());
            } else {
                if (explicit) {
                    partsParseException = new IllegalStateException("coyoteRequest.noMultipartConfig");
                    return;
                } else {
                    parts = Collections.emptyList();
                    return;
                }
            }
        }

        Parameters parameters = coyoteRequest.getParameters();
        parameters.setLimit(getConnector().getMaxParameterCount());

        boolean success = false;

        File location;
        String locationStr = mce.getLocation();
        if (locationStr == null || locationStr.length() == 0) {
            location = (File) context.getServletContext().getAttribute(ServletContext.TEMPDIR);
        } else  {
            location = new File(locationStr);
            if (!location.isAbsolute()) {
                location = new File((File) context.getServletContext().getAttribute(ServletContext.TEMPDIR),
                        locationStr).getAbsoluteFile();
            }
        }

        if (!location.exists() && context.getCreateUploadTargets()) {
            if (!location.mkdirs()) {
                // XXX - 日志记录
            }
        }

        // 必须是个文件夹
        if (!location.isDirectory()) {
            parameters.setParseFailedReason(Parameters.FailReason.MULTIPART_CONFIG_INVALID);
            partsParseException = new IOException("coyoteRequest.uploadLocationInvalid" + location);
            return;
        }

        // TODO - 未完成

    }


    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }
    
    
    /**
     * @return 返回进行了用户认证的远程用户
     */
    @Override
    public String getRemoteUser() {
        return null;
    }


    /**
     * 用户是否在此角色中
     *
     * @param role 角色
     * @return 返回是否被包含
     */
    @Override
    public boolean isUserInRole(String role) {
        return false;
    }


    /**
     * @return 返回用户主体
     */
    @Override
    public Principal getUserPrincipal() {
        return null;
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


    protected void parseCookies() {
        if (cookiesParsed) {
            return;
        }

        cookiesParsed = true;

        ServerCookies serverCookies = coyoteRequest.getCookies();
        serverCookies.setLimit(connector.getMaxCookieCount());
        CookieProcessor cookieProcessor = getContext().getCookieProcessor();
        cookieProcessor.parseCookieHeader(coyoteRequest.getMimeHeaders(), serverCookies);
    }
    
    
    public ServerCookies getServerCookies() {
        return coyoteRequest.getCookies();
    }

    public void setRequestedSessionId(String id) {
        this.requestedSessionId = id;
    }

    public void setRequestedSessionCookie(boolean b) {
        this.requestedSessionCookie = b;
    }

    public void setRequestedSessionURL(boolean b) {
        this.requestedSessionURL = b;
    }

    /**
     * @param b 如果为<b>true</b>，则表示 session id存放在SSL中
     */
    public void setRequestedSessionSSL(boolean b) {
        this.requestedSessionSSL = b;
    }

    public void setCoyoteRequest(com.ranni.coyote.Request coyoteRequest) {
        this.coyoteRequest = coyoteRequest;
        inputBuffer.setRequest(coyoteRequest);
    }


    /**
     * servlet/3.0及以后的异步容器，暂不支持
     * 
     * @return 返回是否支持异步处理。暂未实现，固定返回<b>false</b>
     */
    public boolean isAsync() {
        return false;
    }


    /**
     * @return 返回解码后的请求URI
     */
    public String getDecodedRequestURI() {
        return coyoteRequest.decodedURI().toString();
    }


    /**
     * 返回session
     * 
     * @param b 如果不存在，是否新建session
     * @return 返回此请求关联的session
     */
    public Session getSessionInternal(boolean b) {
        return doGetSession(b);
    }
}
