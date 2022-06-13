package com.ranni.connector;

import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import com.ranni.coyote.ActionCode;
import com.ranni.coyote.Adapter;
import com.ranni.util.ServerInfo;
import com.ranni.util.SessionConfig;
import com.ranni.util.URLEncoder;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.CharChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.EncodedSolidusHandling;
import com.ranni.util.http.ServerCookie;
import com.ranni.util.http.ServerCookies;
import com.ranni.util.net.SSLSupport;
import com.ranni.util.net.SocketEvent;

import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * Title: HttpServer
 * Description:
 * 适配器，用于将处理器传过来的请求交给相应的容器去处理
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 21:55
 * @Ref org.apache.catalina.connector.CoyoteAdapter
 */
public class CoyoteAdapter implements Adapter {

    // ==================================== 属性字段 ====================================

    /**
     * 连接器
     */
    private final Connector connector;

    /**
     * 是否允许URI中有反斜杠
     */
    protected static final boolean ALLOW_BACKSLASH = 
            Boolean.parseBoolean(System.getProperty("com.ranni.connector.CoyoteAdapter.ALLOW_BACKSLASH", "false"));


    /**
     * 保存在CoyoteRequest/CoyoteResponse 的Note中的 HttpServletRequest/HttpServletResponse
     */
    public static final int ADAPTER_NOTES = 1;

    /**
     * 响应头中X-Powered-By属性的信息
     */
    private static final String POWERED_BY = "Servlet/4.0 JSP/2.3 " +
            "(" + ServerInfo.getServerInfo() + " Java/" +
            System.getProperty("java.vm.vendor") + "/" +
            System.getProperty("java.runtime.version") + ")";


    /**
     * session id存储模式
     */
    private static final EnumSet<SessionTrackingMode> SSL_ONLY =
            EnumSet.of(SessionTrackingMode.SSL);
    

    // ==================================== 构造方法 ====================================
    
    public CoyoteAdapter(Connector connector) {
        this.connector = connector;
    }

    
    // ==================================== 核心方法 ====================================

    /**
     * 将处理器传过来的请求放到相应的容器的管道中执行
     * 
     * @param req coyote请求对象
     * @param res coyote响应对象
     *
     * @throws Exception 可能抛出I/O异常
     */
    @Override
    @Deprecated // XXX - 这个标记仅表明此方法实现不完整
    public void service(com.ranni.coyote.Request req, com.ranni.coyote.Response res) throws Exception {
        Request request = (Request) req.getNote(ADAPTER_NOTES);
        Response response = (Response) res.getNote(ADAPTER_NOTES);

        if (request == null) {
            // 如果coyoteRequest中没有保存，就创建
            request = connector.createRequest();
            request.setCoyoteRequest(req);
            response = connector.createResponse();
            response.setCoyoteRequest(res);
            
            // request和response相互关联
            request.setResponse(response);
            response.setRequest(request);
            
            // 绑定至CoyoteRequest/CoyoteResponse中
            req.setNote(ADAPTER_NOTES, request);
            res.setNote(ADAPTER_NOTES, response);
            
            req.getParameters().setQueryStringCharset(connector.getURICharset());
        }
        
        if (connector.getXPoweredBy()) {
            response.addHeader("X-Powered-By", POWERED_BY);
        }
        
        boolean async = false; // 是否是异步请求
        boolean postParseSuccess = false; // 是否预解析成功
        
        req.setRequestThread();
        
        try {
            // 解析部分请求参数设置处理请求的容器
            postParseSuccess = postParseRequest(req, request, res, response);
            if (postParseSuccess) {
                connector.getService().getContainer().invoke(request, response);
            }
            
            if (request.isAsync()) {
                // XXX - 异步请求未实现
            } else {
                request.finishRequest();
                response.finishResponse();
            }
            
            
        } catch (IOException e) {
            
        } finally {
            
            req.clearRequestThread();
            
            if (!async) {
                request.recycle();
                response.recycle();
            }
        }
    }


    /**
     * 解析请求行、请求头以及设置处理请求的容器。
     * 
     * @param req CoyoteRequest
     * @param request HttpServletRequest
     * @param res CoyoteResponse
     * @param response HttpServletResponse
     *                 
     * @exception IOException 可能抛出I/O异常
     *                 
     * @return 如果返回<b>true</b>，则表示可以将请求送到容器的管道里。否则反之
     */
    protected boolean postParseRequest(com.ranni.coyote.Request req, Request request, com.ranni.coyote.Response res, Response response) throws IOException, ServletException {
        
        if (req.scheme().isNull()) {
            // 以连接器协议为准
            req.scheme().setString(connector.getScheme());
            request.setSecure(connector.getSecure());            
        } else {
            request.setSecure(req.scheme().equals("https"));
        }

        String proxyName = connector.getProxyName();
        int proxyPort = connector.getProxyPort();
        if (proxyPort != 0) {
            req.setServerPort(proxyPort);
        } else if (req.getServerPort() == -1) {
            if (req.scheme().equals("https")) {
                req.setServerPort(443);
            } else {
                req.setServerPort(80);
            }
        }

        if (proxyName != null) {
            req.serverName().setString(proxyName);
        }

        MessageBytes undecodedURI = req.requestURI();

        if (undecodedURI.equals("*")) {
            // 没有具体的URI，检查是否是预检请求
            if (req.method().equalsIgnoreCase("OPTIONS")) {
                StringBuilder allow = new StringBuilder();
                allow.append("GET, HEAD, POST, PUT, DELETE, OPTIONS");
                if (connector.getAllowTrace()) {
                    allow.append(", TRACE");
                }
                res.setHeader("Allow", allow.toString());
                return false;
                
            } else {
                response.sendError(400, "Invalid URI");
            }
        }

        MessageBytes decodedURI = req.decodedURI();
        
        if (undecodedURI.getType() == MessageBytes.T_BYTES) {
            decodedURI.duplicate(undecodedURI);

            // 解析路径参数
            parsePathParameters(req, request);
            
            try {
                req.getURLDecoder().convert(decodedURI.getByteChunk(), EncodedSolidusHandling.REJECT);
            } catch (IOException ioe) {
                response.sendError(400, "invalid URI: " + ioe.getMessage());
            }
            
            // 标准化
            if (normalize(decodedURI)) {
                // 转换为字符格式
                convertURI(decodedURI, request);
                
                if (!checkNormalize(req.decodedURI())) {
                    response.sendError(400, "Invalid URI");
                }
                
            } else {
                response.sendError(400, "Invalid URI");
            }
            
        } else {
            decodedURI.toChars();
            CharChunk uriCC = decodedURI.getCharChunk();
            int semicolon = uriCC.indexOf(';');
            if (semicolon > 0) {
                decodedURI.setChars(uriCC.getBuffer(), uriCC.getStart(), semicolon);
            }
        }
        
        MessageBytes serverName;
        if (connector.getUseIPVHosts()) {
            // 需要从socket信道中取服务名
            serverName = req.localName();
            if (serverName.isNull()) {
                res.action(ActionCode.REQ_LOCAL_NAME_ATTRIBUTE, null);
            }
        } else {
            serverName = req.serverName();
        }


        String version = null;
        Context versionContext = null;
        boolean mapRequired = true;
        
        if (response.isError()) {
            // URI无效
            decodedURI.recycle();
        }
        
        main_loop:
        while (mapRequired) {
            connector.getService().getMapper().map(serverName, decodedURI, version, request.getMappingData());
            
            if (request.getContext() == null) {
                // 没有容器，可能没有部署web项目
                return true;
            }
            
            // 尝试从路径参数中设置sessionID
            String sessionID;
            if (request.getServletContext().getEffectiveSessionTrackingModes()
                    .contains(SessionTrackingMode.URL)) {
                // 从路径参数中取的sessionID
                sessionID = request.getPathParameter(SessionConfig.
                        getSessionUriParamName(request.getContext()));
                
                if (sessionID != null) {
                    request.setRequestedSessionId(sessionID);
                    request.setRequestedSessionURL(true);
                }
            }

            // 尝试从cookie中取得sessionID并做设置
            try {
                parseSessionCookiesId(request);
            } catch (IllegalArgumentException e) {
                // 有多个cookie
                if (!response.isError()) {
                    response.setError();
                    response.sendError(400);
                }
                return true;
            }
            
            parseSessionSslId(request);
            
            sessionID = request.getRequestedSessionId();
            
            mapRequired = false;
            if (version == null || request.getContext() != versionContext) {
                // 没得到版本号对应的context，匹配合适的context
                version = null;
                versionContext = null;

                Context[] contexts = request.getMappingData().contexts;
                if (contexts != null && sessionID != null) {
                    for (int i = contexts.length - 1; i >= 0; i--) {
                        Context context = contexts[i];
                        if (context.getManager().findSession(sessionID) != null) {
                            // 就是这个Context了
                            if (!context.equals(request.getMappingData().context)) {
                                // 换绑
                                version = context.getWebappVersion();
                                versionContext = context;
                                
                                request.getMappingData().recycle();
                                request.recycle();
                                request.recycleCookieInfo(true);
                            }
                            
                            continue main_loop;
                        }
                    }
                }
            }
            
            // 走到这儿就说明匹配上容器了
            if (request.getContext().getPaused()) {
                // 就是这个context容器，但是这个容器处于暂停状态。
                // wrapper容器可能会被更改，所以不能用这个容器
                request.getMappingData().recycle();
                mapRequired = true;                
            }
            
        } // while end


        // 如果重定向路径不为空，设置重定向路径
        MessageBytes redirectPathMB = request.getMappingData().redirectPath;
        if (!redirectPathMB.isNull()) {
            String redirectPath = URLEncoder.DEFAULT.encode(
                    redirectPathMB.toString(), StandardCharsets.UTF_8);

            String query = request.getQueryString();

            // session id从url中取得的，与转发路径进行拼装
            if (request.isRequestedSessionIdFromURL()) {
                redirectPath = redirectPath + ";" + SessionConfig
                        .getSessionUriParamName(request.getContext()) +
                        "=" + request.getRequestedSessionId();
            }

            // 把url中的查询字符串拼接到转发路径后
            if (query != null) {
                redirectPath = redirectPath + "?" + query;
            }

            // 转发请求
            response.sendRedirect(redirectPath);
            return false;
        }
        
        // 如果连接器不支持TRACE方法，那么过滤它
        if (!connector.getAllowTrace()
            && req.method().equalsIgnoreCase("TRACE")) {
            
            Wrapper wrapper = request.getWrapper();
            String header = null;
            if (wrapper != null) {
                String[] methods = wrapper.getServletMethods();
                if (methods != null) {
                    for (String method : methods) {
                        if ("TRACE".equals(method)) {
                            continue;
                        }
                        if (header == null) {
                            header = method;
                        } else {
                            header += ", " + method;
                        }
                    }
                }
            }
            
            if (header != null) {
                res.addHeader("Allow", header);
            }
            response.sendError(405, "TRACE method is not allowed");
            
            return true;
        }
        
        return true;
    }


    /**
     * 解析路径参数。例如取出url中携带的jsessionid<br>
     * http://localhost/test;jsessionid=F0D358CE192599DE7BF6AD271394D3BF
     * 
     * @param req CoyoteRequest
     * @param request HttpServletRequest
     */
    protected void parsePathParameters(com.ranni.coyote.Request req, Request request) {
        // 转字节块处理
        req.decodedURI().toBytes();

        ByteChunk uriBC = req.decodedURI().getByteChunk();
        
        int semicolon = uriBC.indexOf(';', 0);
        
        if (semicolon == -1) {
            // 没有路径参数，直接i返回
            return;
        }

        Charset charset = connector.getURICharset();
        
        while (semicolon > -1) {
            int start = uriBC.getStart();
            int end = uriBC.getEnd();
            
            int parameterStart = semicolon + 1;
            int parameterEnd = ByteChunk.findBytes(uriBC.getBuffer(), start + parameterStart, end, new byte[]{';', '/'});
            
            String pv = null;
            
            if (parameterEnd >= 0) {
                if (charset != null) {
                    pv = new String(uriBC.getBuffer(), start + parameterStart,
                            parameterEnd - parameterStart, charset);
                }
                
                // 将后面的往前移动，覆盖已经读取了的
                byte[] buf = uriBC.getBuffer();
                for (int i = 0; i < end - start - parameterEnd; i++) {
                    buf[start + semicolon + i] = buf[start + parameterEnd + i];
                }
                
                // 下面这个长度计算的说明： 
                // "cnmd/test;n1=123;n2=321/dnmd"  说明："/test;n1=123;n2=321"为字节块在缓冲区中对应的数据部分
                // semicolon = 5; start = 4; end = 23 ; parameterStart = 6; parameterEnd = 12
                // 当"n1=123"被取出来后，这段数据被后面的数据覆盖，缓冲区变为：
                // "cnmd/test;n2=321/dnmd"
                // len = 12 = end - start - parameterEnd + semicolon = 23 - 4 - 12 + 5 = 11 
                uriBC.setBytes(buf, start, end - start - parameterEnd + semicolon);
                
            } else {
                if (charset != null) {
                    pv = new String(uriBC.getBuffer(), start + parameterStart,
                            end - start - parameterStart, charset);
                }
                uriBC.setEnd(start + semicolon);
            }
            
            // 存放到request中
            if (pv != null) {
                int equals = pv.indexOf('=');
                if (equals > -1) {
                    String name = pv.substring(0, equals);
                    String value = pv.substring(equals + 1);
                    request.addPathParameter(name, value);
                }
            }
            
            semicolon = uriBC.indexOf(';', semicolon);
        }

    }

    
    @Override
    public boolean prepare(com.ranni.coyote.Request req, com.ranni.coyote.Response res) throws Exception {
        return false;
    }

    
    @Override
    public boolean asyncDispatch(com.ranni.coyote.Request req, com.ranni.coyote.Response res, SocketEvent status) throws Exception {
        return false;
    }

    
    @Override
    public void log(com.ranni.coyote.Request req, com.ranni.coyote.Response res, long time) {

    }

    
    @Override
    public void checkRecycled(com.ranni.coyote.Request req, com.ranni.coyote.Response res) {

    }

    
    @Override
    public String getDomain() {
        return null;
    }


    // ==================================== 字符串处理 ====================================

    protected void parseSessionSslId(Request request) {
        if (request.getRequestedSessionId() == null &&
                SSL_ONLY.equals(request.getServletContext()
                        .getEffectiveSessionTrackingModes()) &&
                request.connector.secure) {
            String sessionId = (String) request.getAttribute(SSLSupport.SESSION_ID_KEY);
            if (sessionId != null) {
                request.setRequestedSessionId(sessionId);
                request.setRequestedSessionSSL(true);
            }
        }
    }
    
    
    /**
     * Parse session id in Cookie.
     *
     * @param request The Servlet request object
     */
    protected void parseSessionCookiesId(Request request) {

        // If session tracking via cookies has been disabled for the current
        // context, don't go looking for a session ID in a cookie as a cookie
        // from a parent context with a session ID may be present which would
        // overwrite the valid session ID encoded in the URL
        Context context = request.getMappingData().context;
        if (context != null && !context.getServletContext()
                .getEffectiveSessionTrackingModes().contains(
                        SessionTrackingMode.COOKIE)) {
            return;
        }

        // Parse session id from cookies
        ServerCookies serverCookies = request.getServerCookies();
        int count = serverCookies.getCookieCount();
        if (count <= 0) {
            return;
        }

        String sessionCookieName = SessionConfig.getSessionCookieName(context);

        for (int i = 0; i < count; i++) {
            ServerCookie scookie = serverCookies.getCookie(i);
            if (scookie.getName().equals(sessionCookieName)) {
                // Override anything requested in the URL
                if (!request.isRequestedSessionIdFromCookie()) {
                    // Accept only the first session id cookie
                    convertMB(scookie.getValue());
                    request.setRequestedSessionId
                            (scookie.getValue().toString());
                    request.setRequestedSessionCookie(true);
                    request.setRequestedSessionURL(false);
                } else {
                    if (!request.isRequestedSessionIdValid()) {
                        // Replace the session id until one is valid
                        convertMB(scookie.getValue());
                        request.setRequestedSessionId
                                (scookie.getValue().toString());
                    }
                }
            }
        }

    }


    /**
     * Character conversion of the URI.
     *
     * @param uri MessageBytes object containing the URI
     * @param request The Servlet request object
     * @throws IOException if a IO exception occurs sending an error to the client
     */
    protected void convertURI(MessageBytes uri, Request request) throws IOException {

        ByteChunk bc = uri.getByteChunk();
        int length = bc.getLength();
        CharChunk cc = uri.getCharChunk();
        cc.allocate(length, -1);

        Charset charset = connector.getURICharset();

        B2CConverter conv = request.getURIConverter();
        if (conv == null) {
            conv = new B2CConverter(charset, true);
            request.setURIConverter(conv);
        } else {
            conv.recycle();
        }

        try {
            conv.convert(bc, cc, true);
            uri.setChars(cc.getBuffer(), cc.getStart(), cc.getLength());
        } catch (IOException ioe) {
            // Should never happen as B2CConverter should replace
            // problematic characters
            request.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }


    /**
     * Character conversion of the a US-ASCII MessageBytes.
     *
     * @param mb The MessageBytes instance containing the bytes that should be converted to chars
     */
    protected void convertMB(MessageBytes mb) {

        // This is of course only meaningful for bytes
        if (mb.getType() != MessageBytes.T_BYTES) {
            return;
        }

        ByteChunk bc = mb.getByteChunk();
        CharChunk cc = mb.getCharChunk();
        int length = bc.getLength();
        cc.allocate(length, -1);

        // Default encoding: fast conversion
        byte[] bbuf = bc.getBuffer();
        char[] cbuf = cc.getBuffer();
        int start = bc.getStart();
        for (int i = 0; i < length; i++) {
            cbuf[i] = (char) (bbuf[i + start] & 0xff);
        }
        mb.setChars(cbuf, 0, length);

    }


    /**
     * This method normalizes "\", "//", "/./" and "/../".
     *
     * @param uriMB URI to be normalized
     *
     * @return <code>false</code> if normalizing this URI would require going
     *         above the root, or if the URI contains a null byte, otherwise
     *         <code>true</code>
     */
    public static boolean normalize(MessageBytes uriMB) {

        ByteChunk uriBC = uriMB.getByteChunk();
        final byte[] b = uriBC.getBytes();
        final int start = uriBC.getStart();
        int end = uriBC.getEnd();

        // An empty URL is not acceptable
        if (start == end) {
            return false;
        }

        int pos = 0;
        int index = 0;


        // The URL must start with '/' (or '\' that will be replaced soon)
        if (b[start] != (byte) '/' && b[start] != (byte) '\\') {
            return false;
        }

        // Replace '\' with '/'
        // Check for null byte
        for (pos = start; pos < end; pos++) {
            if (b[pos] == (byte) '\\') {
                if (ALLOW_BACKSLASH) {
                    b[pos] = (byte) '/';
                } else {
                    return false;
                }
            } else if (b[pos] == (byte) 0) {
                return false;
            }
        }

        // Replace "//" with "/"
        for (pos = start; pos < (end - 1); pos++) {
            if (b[pos] == (byte) '/') {
                while ((pos + 1 < end) && (b[pos + 1] == (byte) '/')) {
                    copyBytes(b, pos, pos + 1, end - pos - 1);
                    end--;
                }
            }
        }

        // If the URI ends with "/." or "/..", then we append an extra "/"
        // Note: It is possible to extend the URI by 1 without any side effect
        // as the next character is a non-significant WS.
        if (((end - start) >= 2) && (b[end - 1] == (byte) '.')) {
            if ((b[end - 2] == (byte) '/')
                    || ((b[end - 2] == (byte) '.')
                    && (b[end - 3] == (byte) '/'))) {
                b[end] = (byte) '/';
                end++;
            }
        }

        uriBC.setEnd(end);

        index = 0;

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            index = uriBC.indexOf("/./", 0, 3, index);
            if (index < 0) {
                break;
            }
            copyBytes(b, start + index, start + index + 2,
                    end - start - index - 2);
            end = end - 2;
            uriBC.setEnd(end);
        }

        index = 0;

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            index = uriBC.indexOf("/../", 0, 4, index);
            if (index < 0) {
                break;
            }
            // Prevent from going outside our context
            if (index == 0) {
                return false;
            }
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (b[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyBytes(b, start + index2, start + index + 3,
                    end - start - index - 3);
            end = end + index2 - index - 3;
            uriBC.setEnd(end);
            index = index2;
        }

        return true;

    }


    /**
     * Check that the URI is normalized following character decoding. This
     * method checks for "\", 0, "//", "/./" and "/../".
     *
     * @param uriMB URI to be checked (should be chars)
     *
     * @return <code>false</code> if sequences that are supposed to be
     *         normalized are still present in the URI, otherwise
     *         <code>true</code>
     *
     * @deprecated This code will be removed in Apache Tomcat 10 onwards
     */
    @Deprecated
    public static boolean checkNormalize(MessageBytes uriMB) {

        CharChunk uriCC = uriMB.getCharChunk();
        char[] c = uriCC.getChars();
        int start = uriCC.getStart();
        int end = uriCC.getEnd();

        int pos = 0;

        // Check for '\' and 0
        for (pos = start; pos < end; pos++) {
            if (c[pos] == '\\') {
                return false;
            }
            if (c[pos] == 0) {
                return false;
            }
        }

        // Check for "//"
        for (pos = start; pos < (end - 1); pos++) {
            if (c[pos] == '/') {
                if (c[pos + 1] == '/') {
                    return false;
                }
            }
        }

        // Check for ending with "/." or "/.."
        if (((end - start) >= 2) && (c[end - 1] == '.')) {
            if ((c[end - 2] == '/')
                    || ((c[end - 2] == '.')
                    && (c[end - 3] == '/'))) {
                return false;
            }
        }

        // Check for "/./"
        if (uriCC.indexOf("/./", 0, 3, 0) >= 0) {
            return false;
        }

        // Check for "/../"
        if (uriCC.indexOf("/../", 0, 4, 0) >= 0) {
            return false;
        }

        return true;

    }


    /**
     * Copy an array of bytes to a different position. Used during
     * normalization.
     *
     * @param b The bytes that should be copied
     * @param dest Destination offset
     * @param src Source offset
     * @param len Length
     */
    protected static void copyBytes(byte[] b, int dest, int src, int len) {
        System.arraycopy(b, src, b, dest, len);
    }
    
    
}
