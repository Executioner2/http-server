package com.ranni.connector;

import com.ranni.container.Context;
import com.ranni.container.session.Session;
import com.ranni.coyote.Constants;
import com.ranni.util.http.FastHttpDateFormat;
import com.ranni.util.SessionConfig;
import com.ranni.util.buf.CharChunk;
import com.ranni.util.buf.UEncoder;
import com.ranni.util.buf.UriUtil;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.http.parse.MediaTypeCache;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/24 21:48
 * @Ref org.apache.catalina.connector.Response
 */
public class Response implements HttpServletResponse {

    // ==================================== 基本属性字段 ====================================

    /**
     * 响应体编码
     */
    private static final boolean ENFORCE_ENCODING_IN_GET_WRITER;

    static {
        ENFORCE_ENCODING_IN_GET_WRITER = Boolean.parseBoolean(
                System.getProperty("com.ranni.connector.Response.ENFORCE_ENCODING_IN_GET_WRITER",
                        "true"));
    }

    /**
     * 媒体类型缓存
     */
    private static final MediaTypeCache MEDIA_TYPE_CACHE = new MediaTypeCache(100);
    
    /**
     * 与此请求相关联的请求实例
     */
    private Request request;

    /**
     * CoyoteResponse
     */
    private com.ranni.coyote.Response coyoteResponse;

    /**
     * 输出缓冲区
     */
    private OutputBuffer outputBuffer;

    /**
     * PrintWriter的包装实例
     */
    protected CoyoteWriter writer;
    
    /**
     * ServletOutputStream的包装实例
     */
    protected CoyoteOutputStream outputStream;
    
    /**
     * 此响应的外观类
     */
    private ResponseFacade facade;

    /**
     * 应用response实例<br>
     * 和facade有何区别？applicationResponse可以是多个applicationResponse
     * 嵌套形成的。但最核心部分还是facade
     */
    private HttpServletResponse applicationResponse = null;

    /**
     * cookie集合
     */
    private final List<Cookie> cookies = new ArrayList<>();

    /**
     * 重定向路径
     */
    protected final CharChunk redirectURLCC = new CharChunk();

    /**
     * URL编码器
     */
    protected final UEncoder urlEncoder = new UEncoder(UEncoder.SafeCharsSet.WITH_SLASH);

    /**
     * 是否使用了PrintWriter
     */
    protected boolean usingWriter = false;

    /**
     * 是否使用了OutputStream
     */
    protected boolean usingOutputStream = false;

    /**
     * 是否使用了编码集
     */
    private boolean isCharacterEncodingSet = false;
    
    /**
     * 已经提交了响应包 
     */
    protected boolean appCommitted = false;

    
    // ==================================== 构造方法 ====================================
    
    public Response() {
        this(OutputBuffer.DEFAULT_BUFFER_SIZE);
    }
    
    public Response(int size) {
        this.outputBuffer = new OutputBuffer(size);
    }

    
    // ==================================== 通用方法 ====================================

    /**
     * @return 返回此响应的外观实例
     */
    public HttpServletResponse getResponse() {
        if (facade == null) {
            facade = new ResponseFacade(this);
        }
        if (applicationResponse == null) {
            applicationResponse = facade;
        }
        return applicationResponse;
    }


    /**
     * 设置应用响应实例。
     * 
     * @param applicationResponse 应用响应实例
     */
    public void setResponse(HttpServletResponse applicationResponse) {
        ServletResponse s = applicationResponse;
        
        // 取得最核心部分，也就是facade
        while (s instanceof HttpServletResponseWrapper) {
            s = ((HttpServletResponseWrapper) s).getResponse();
        }
        
        if (s != facade) {
            throw new IllegalArgumentException("response.illegalWrap");
        }
        this.applicationResponse = applicationResponse;
    }


    /**
     * 将cookie添加到响应头中
     * 
     * @param cookie 需要添加的cookie
     */
    public void addSessionCookieInternal(final Cookie cookie) {
        if (isCommitted()) {
            return;
        }

        String name = cookie.getName();
        final String headername = "Set-Cookie";
        final String startsWith = name + "=";
        String header = generateCookieString(cookie);
        boolean set = false;
        MimeHeaders headers = getCoyoteResponse().getMimeHeaders();
        int n = headers.size();
        for (int i = 0; i < n; i++) {
            if (headers.getName(i).toString().equals(headername)) {
                if (headers.getValue(i).toString().startsWith(startsWith)) {
                    headers.getValue(i).setString(header);
                    set = true;
                }
            }
        }
        if (!set) {
            addHeader(headername, header);
        }
    }


    /**
     * 设置输出缓冲区的挂起标志位
     * 
     * @param suspended 挂起标志位
     */
    public void setSuspended(boolean suspended) {
        outputBuffer.setSuspended(suspended);
    }


    /**
     * @return 如果返回<b>true</b>，则表示输出缓冲区处于挂起状态
     */
    public boolean isSuspended() {
        return outputBuffer.isSuspended();
    }


    /**
     * @return 如果返回<b>true</b>，则表示输出缓冲区处于关闭状态
     */
    public boolean isClosed() {
        return outputBuffer.isClosed();
    }
    
    
    /**
     * 将cookie转换为cookie字符串
     *
     * @param cookie 需要转换为字符串的cookie
     * @return 返回转换为字符串的cookie
     */
    public String generateCookieString(final Cookie cookie) {
        return getContext().getCookieProcessor().generateHeader(cookie, request.getRequest());
    }


    /**
     * 设置CoyoteResponse
     *
     * @param coyoteResponse 要设置的CoyoteResponse
     */
    public void setCoyoteRequest(com.ranni.coyote.Response coyoteResponse) {
        this.coyoteResponse = coyoteResponse;
        outputBuffer.setResponse(coyoteResponse);
    }
    
    
    /**
     * @return 返回CoyoteResponse
     */
    public com.ranni.coyote.Response getCoyoteResponse() {
        return coyoteResponse;
    }


    /**
     * @return 返回Context容器
     */
    public Context getContext() {
        return request.getContext();
    }


    /**
     * 设置与此响应相关的请求
     * 
     * @param request 请求
     */
    public void setRequest(Request request) {
        this.request = request;
    }


    /**
     * @return 返回与此响应相关联的请求实例
     */
    public Request getRequest() {
        return this.request;
    }


    /**
     * 回收
     */
    public void recycle() {
        cookies.clear();
        outputBuffer.recycle();
        usingOutputStream = false;
        usingWriter = false;
        appCommitted = false;
        isCharacterEncodingSet = false;

        applicationResponse = null;
        if (getRequest().getDiscardFacades()) {
            if (facade != null) {
                facade.clear();
                facade = null;
            }
            if (outputStream != null) {
                outputStream.clear();
                outputStream = null;
            }
            if (writer != null) {
                writer.clear();
                writer = null;
            }
        } else if (writer != null) {
            writer.recycle();
        }
    }


    /**
     * 完成响应。执行关闭输出缓冲区的操作
     * 
     * @throws IOException 可能抛出I/O异常
     */
    public void finishResponse() throws IOException {
        outputBuffer.close();
    }


    /**
     * @return 如果返回<b>true</b>，则表示此请求的处理遇到了错误（有错误信息需要响应）
     */
    public boolean isError() {
        return getCoyoteResponse().isError();
    }


    /**
     * 设置错误
     */
    public boolean setError() {
        return getCoyoteResponse().setError();
    }


    /**
     * @return 如果返回<b>true</b>，则表示有错误需要报告
     */
    public boolean isErrorReportRequired() {
        return getCoyoteResponse().isErrorReportRequired();
    }


    /**
     * 设置需要错误报告
     * 
     * @return 如果返回<b>true</b>，则表示设置成功
     */
    public boolean setErrorReported() {
        return getCoyoteResponse().setErrorReported();
    }


    /**
     * @return 返回内容长度
     */
    public int getContentLength() {
        return getCoyoteResponse().getContentLength();
    }
    

    // ==================================== 核心方法 ====================================

    /**
     * 添加cookie到响应头
     *
     * @param cookie 要添加的cookie
     */
    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted()) {
            return;
        }
        
        cookies.add(cookie);

        String s = generateCookieString(cookie);
        
        addHeader("Set-Cookie", s, getContext().getCookieProcessor().getCharset());
    }


    /**
     * 响应头是否包含此标头名的标头<br/>
     * 
     * <b>Content-Type</b>和<b>Content-Length</b>为
     * 特殊标头。如果传入的name是这两个之中的一个，就不在标头
     * 集合中去找，而是通过判断CoyoteResponse中对应的属性值
     * 推断出响应头中是否包含此标头
     * 
     * @param name 标头名
     * @return 如果返回<b>true</b>，则表示响应头中有此标头
     */
    @Override
    public boolean containsHeader(String name) {
        char cc = name.charAt(0);
        
        if (cc == 'C' || cc == 'c') {
            if (name.equalsIgnoreCase("Content-Type")) {
                return getCoyoteResponse().getContentType() != null;
            }
            if (name.equalsIgnoreCase("Content-Length")) {
                return getCoyoteResponse().getContentLength() != -1;
            }
        }
        
        return false;
    }


    /**
     * 对url进行编码。
     *
     * @param url 要编码的url
     * @return 返回编码后的url
     */
    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }
    

    /**
     * 对url进行编码。
     * 
     * @param url 要编码的url
     * @return 返回编码后的url
     */
    @Override
    public String encodeURL(String url) {
        String absolute;
        
        try {
            absolute = toAbsolute(url);
        } catch (IllegalArgumentException iae) {
            return url;
        }
        
        if (isEncodeable(absolute)) {
            if (url.equalsIgnoreCase("")) {
                url = absolute;
            } else if (url.equals(absolute) && !hasPath(url)) {
                url += '/';
            }
            return toEncoded(url, request.getSessionInternal().getIdInternal());
        } else {
            return url;
        }
        
    }


    /**
     * 对url进行编码
     * 
     * @param url 需要编码的url
     * @param sessionId url中的session id
     * @return 返回编码后的url
     */
    protected String toEncoded(String url, String sessionId) {
        if (url == null || sessionId == null) {
            return url;
        }
        
        String path = url;
        String query = "";
        String anchor = "";
        
        int question = url.indexOf('?');
        if (question >= 0) {
            path = url.substring(0, question);
            query = url.substring(question);
        }
        int pound = path.indexOf('#');
        if (pound >= 0) {
            anchor = path.substring(pound);
            path = path.substring(0, pound); // 锚点用于前端的页面快速定位，对服务器无用
        }

        StringBuilder sb = new StringBuilder(path);
        
        // 拼接session id。url中的session id不能在字符串首位
        if (sb.length() > 0) {
            sb.append(';');
            sb.append(SessionConfig.getSessionUriParamName(request.getContext()));
            sb.append('=');
            sb.append(sessionId);
        }
        
        sb.append(anchor); // 拼接锚点
        sb.append(query); // 拼接查询参数
        
        return sb.toString();
    }
    

    /**
     * 此路径是否是绝对路径
     * 
     * @param url url路径
     * @return 如果返回<b>true</b>，则表示此路径是绝对路径
     */
    private boolean hasPath(String url) {
        int pos = url.indexOf("://");
        if (pos < 0) {
            return false;
        }
        pos = url.indexOf('/', pos + 3);
        if (pos < 0) {
            return false;
        }
        return true;        
    }


    /**
     * 此url是否应该被解码
     * 
     * @param url 要解码的url
     * @return 如果返回<b>true</b>，则表示此url可以解码
     */
    protected boolean isEncodeable(final String url) {
        if (url == null) {
            return false;
        }
        
        if (url.startsWith("#")) {
            return false;
        }
        
        // url中使用了session id
        Session session = request.getSessionInternal(false);
        if (session == null) {
            return false;
        }
        // session id 存于cookie中，对url不用解码
        if (request.isRequestedSessionIdFromCookie()) {
            return false;
        }
        
        if (!request.getServletContext().getEffectiveSessionTrackingModes()
            .contains(SessionTrackingMode.URL)) {
            return false;
        }
        
        return doIsEncodeable(getContext(), session, url);
    }
    
    
    private boolean doIsEncodeable(Context context, Session session, String location) {
        URL url = null;
        
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            return false;
        }
        
        if (!request.getScheme().equalsIgnoreCase(url.getProtocol())) {
            return false;
        }
        
        if (!request.getServerName().equalsIgnoreCase(url.getHost())) {
            return false;
        }

        int serverPort = request.getServerPort();
        if (serverPort == -1) {
            if ("https".equals(request.getScheme())) {
                serverPort = 443;
            } else {
                serverPort = 80;
            }
        }

        int urlPort = url.getPort();
        if (urlPort == -1) {
            if ("https".equals(url.getProtocol())) {
                urlPort = 443;
            } else {
                urlPort = 80;
            }
        }
        
        if (serverPort != urlPort) {
            return false;
        }

        String contextPath = context.getPath();
        if (contextPath != null) {
            String file = url.getFile();
            if (!file.startsWith(contextPath)) {
                return false;
            }
            String tok = ";" + SessionConfig.getSessionUriParamName(context) + "=" +
                    session.getIdInternal();
            if(file.indexOf(tok, contextPath.length()) >= 0) {
                return false;
            }
        }

        return true;
    }


    /**
     * 转换为绝对路径。如果需要转换的路径已经是绝对路径，
     * 则原封返回。<br>
     * <b>注：处理的是重定向的url</b>
     * 
     * @param url 需要转换的路径
     * @return 转换后的url
     */
    protected String toAbsolute(String url) {
        if (url == null) {
            return null;
        }
        
        boolean leadingSlash = url.startsWith("/");
        
        if (url.startsWith("//")) {
            
            // 没有协议，有服务器地址，端口号，资源路径
            redirectURLCC.recycle();
            String scheme = request.getScheme();
            
            try {
                redirectURLCC.append(scheme);
                redirectURLCC.append(':');
                redirectURLCC.append(url);
            } catch (IOException e) {
                throw new IllegalArgumentException(url, e);
            }

            return redirectURLCC.toString();
            
        } else if (leadingSlash || !UriUtil.hasScheme(url)) {
            
            // 没有协议，服务器地址，端口号，仅有资源路径
            redirectURLCC.recycle();
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            
            try {
                redirectURLCC.append(scheme);
                redirectURLCC.append("://");
                redirectURLCC.append(serverName);
                
                if (scheme.equals("http") && serverPort != 80
                    || scheme.equals("https") && serverPort != 443) {
                    
                    redirectURLCC.append(':');
                    redirectURLCC.append(serverPort + "");
                }
                
                if (!leadingSlash) {
                    String relativePath = request.getDecodedRequestURI();
                    int pos = relativePath.lastIndexOf('/');
                    CharChunk encodedURI = urlEncoder.encodeURL(relativePath, 0, pos);
                    redirectURLCC.append(encodedURI);
                    encodedURI.recycle();
                    redirectURLCC.append('/');
                }
                
                redirectURLCC.append(url);
                normalize(redirectURLCC);
                
            } catch (IOException e) {
                throw new IllegalArgumentException(url, e);
            }

            return redirectURLCC.toString();

        } else {
            
            return url;
        }
    }
    

    /**
     * Removes /./ and /../ sequences from absolute URLs.
     * Code borrowed heavily from CoyoteAdapter.normalize()
     *
     * @param cc the char chunk containing the chars to normalize
     */
    private void normalize(CharChunk cc) {
        // Strip query string and/or fragment first as doing it this way makes
        // the normalization logic a lot simpler
        int truncate = cc.indexOf('?');
        if (truncate == -1) {
            truncate = cc.indexOf('#');
        }
        char[] truncateCC = null;
        if (truncate > -1) {
            truncateCC = Arrays.copyOfRange(cc.getBuffer(),
                    cc.getStart() + truncate, cc.getEnd());
            cc.setEnd(cc.getStart() + truncate);
        }

        if (cc.endsWith("/.") || cc.endsWith("/..")) {
            try {
                cc.append('/');
            } catch (IOException e) {
                throw new IllegalArgumentException(cc.toString(), e);
            }
        }

        char[] c = cc.getChars();
        int start = cc.getStart();
        int end = cc.getEnd();
        int index = 0;
        int startIndex = 0;

        // Advance past the first three / characters (should place index just
        // scheme://host[:port]

        for (int i = 0; i < 3; i++) {
            startIndex = cc.indexOf('/', startIndex + 1);
        }

        // Remove /./
        index = startIndex;
        while (true) {
            index = cc.indexOf("/./", 0, 3, index);
            if (index < 0) {
                break;
            }
            copyChars(c, start + index, start + index + 2,
                    end - start - index - 2);
            end = end - 2;
            cc.setEnd(end);
        }

        // Remove /../
        index = startIndex;
        int pos;
        while (true) {
            index = cc.indexOf("/../", 0, 4, index);
            if (index < 0) {
                break;
            }
            // Can't go above the server root
            if (index == startIndex) {
                throw new IllegalArgumentException();
            }
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (c[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyChars(c, start + index2, start + index + 3,
                    end - start - index - 3);
            end = end + index2 - index - 3;
            cc.setEnd(end);
            index = index2;
        }

        // Add the query string and/or fragment (if present) back in
        if (truncateCC != null) {
            try {
                cc.append(truncateCC, 0, truncateCC.length);
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }
    }

    private void copyChars(char[] c, int dest, int src, int len) {
        System.arraycopy(c, src, c, dest, len);
    }


    /**
     * 编码重定向url
     *
     * @param url 需要编码的重定向url
     * @return 返回编码后的重定向url
     */
    @Override
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }


    /**
     * 编码重定向url
     * 
     * @param url 需要编码的重定向url
     * @return 返回编码后的重定向url
     */
    @Override
    public String encodeRedirectURL(String url) {
        if (isEncodeable(toAbsolute(url))) {
            return toEncoded(url, request.getSessionInternal().getIdInternal());
        } else {
            return url;
        }
    }


    /**
     * 发送错误
     * 
     * @param sc 错误状态码
     * @param msg 错误消息
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("coyoteResponse.sendError.ise");
        }
        
        setError();
        
        getCoyoteResponse().setStatus(sc);
        getCoyoteResponse().setMessage(msg);
        
        // 清空缓冲区
        resetBuffer();
        
        setSuspended(true);
    }


    /**
     * 发送错误
     * 
     * @param sc 错误状态码
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, null);
    }


    /**
     * 发送重定向
     * 
     * @param location 重定向路径
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        sendRedirect(location, SC_FOUND);
    }


    /**
     * 发送重定向
     * 
     * @param location 重定向路径
     * @param status 状态码
     * @throws IOException 可能抛出I/O异常
     */
    public void sendRedirect(String location, int status) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("coyoteResponse.sendRedirect.ise");
        }
        
        resetBuffer(true);
        
        try {
            Context context = getContext();
            boolean reqHasContext = (context != null);
            String locationUri;
            
            if (getRequest().getCoyoteRequest().getSupportsRelativeRedirects()
                && (!reqHasContext || context.getUseRelativeRedirects())) {
                // 可以进行相对路径重定向
                locationUri = location;
            } else {
                // 必须是绝对路径重定向
                locationUri = toAbsolute(location);
            }
            
            setStatus(status);
            setHeader("Location", locationUri);
            if (reqHasContext && context.getSendRedirectBody()) {
                flushBuffer();
            }
            
        } catch (IllegalArgumentException e) {
            setStatus(SC_NOT_FOUND);
        }
        
        setSuspended(true);
    }


    /**
     * 跳过特殊标头
     * 
     * @param name 标头名
     * @param value 标头值
     * @return 返回是否时特殊标头
     */
    private boolean checkSpecialHeader(String name, String value) {
        if (name.equalsIgnoreCase("Content-Type")) {
            setContentType(value);
            return true;
        }
        return false;
    }
    

    /**
     * 设置日期标头
     * 
     * @param name 标头名
     * @param date 标头值
     */
    @Override
    public void setDateHeader(String name, long date) {        
        setHeader(name, FastHttpDateFormat.formatDate(date));
    }


    /**
     * 添加日期标头
     *
     * @param name 标头名
     * @param date 标头值
     */
    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, FastHttpDateFormat.formatDate(date));
    }

    
    /**
     * 设置标头
     * 
     * @param name 标头名
     * @param value 标头值
     */
    @Override
    public void setHeader(String name, String value) {
        if (name == null || name.length() == 0
            || value == null || isCommitted()) {
            return;
        }

        char cc=name.charAt(0);
        if (cc=='C' || cc=='c') {
            if (checkSpecialHeader(name, value)) {
                return;
            }
        }
        
        getCoyoteResponse().setHeader(name, value);
    }


    /**
     * 添加标头
     * 
     * @param name 标头名
     * @param value 标头值
     */
    @Override
    public void addHeader(String name, String value) {
        addHeader(name, value, null);
    }


    /**
     * 添加标头
     *
     * @param name 标头名
     * @param value 标头值
     * @param charset 编码方式
     */
    private void addHeader(String name, String value, Charset charset) {
        if (name == null || name.length() == 0 
            || value == null || isCommitted()) {
            return;
        }

        char cc=name.charAt(0);
        if (cc=='C' || cc=='c') {
            if (checkSpecialHeader(name, value)) {
                return;
            }
        }
        
        getCoyoteResponse().addHeader(name, value, charset);
    }


    /**
     * 设置整型值的标头
     * 
     * @param name 标头名
     * @param value 标头值
     */
    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, "" + value);
    }


    /**
     * 添加整型值的标头
     *
     * @param name 标头名
     * @param value 标头值
     */
    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, "" + value);
    }


    /**
     * 设置状态码
     * 
     * @param sc 状态码 
     */
    @Override
    public void setStatus(int sc) {
        setStatus(sc, null);
    }


    /**
     * 设置状态码和响应消息
     * 
     * @param sc 状态码
     * @param msg 响应消息
     */
    @Override
    public void setStatus(int sc, String msg) {
        if (isCommitted()) {
            return;
        }
        
        getCoyoteResponse().setStatus(sc);
        getCoyoteResponse().setMessage(msg);
    }


    /**
     * @return 返回响应状态码
     */
    @Override
    public int getStatus() {
        return getCoyoteResponse().getStatus();
    }

    
    /**
     * @return 返回响应状态消息
     */
    public String getMessage() {
        return getCoyoteResponse().getMessage();
    }


    /**
     * 返回标头名对应的标头值
     * 
     * @param name 标头名
     * @return 返回标头名对应的标头值
     */
    @Override
    public String getHeader(String name) {
        return getCoyoteResponse().getMimeHeaders().getHeader(name);
    }


    /**
     * 返回所有相同标头名的标头值
     * 
     * @param name 标头名
     * @return 标头值集合
     */
    @Override
    public Collection<String> getHeaders(String name) {
        Enumeration<String> enumeration = getCoyoteResponse().getMimeHeaders().values(name);
        Set<String> res = new LinkedHashSet<>();
        while (enumeration.hasMoreElements()) {
            res.add(enumeration.nextElement());
        }
        return res;
    }


    /**
     * @return 返回所有标头名
     */
    @Override
    public Collection<String> getHeaderNames() {
        Enumeration<String> enumeration = getCoyoteResponse().getMimeHeaders().names();
        Set<String> res = new LinkedHashSet<>();
        while (enumeration.hasMoreElements()) {
            res.add(enumeration.nextElement());
        }
        return res;
    }


    /**
     * @return 返回编码方式
     */
    @Override
    public String getCharacterEncoding() {
        String charset = getCoyoteResponse().getCharacterEncoding();
        if (charset != null) {
            return charset;
        }

        Context context = getContext();
        String res = null;
        if (context != null) {
            res = context.getRequestCharacterEncoding();
        }

        if (res == null) {
            res = Constants.DEFAULT_BODY_CHARSET.name();
        }
        
        return res;
    }


    /**
     * @return 返回响应体类型
     */
    @Override
    public String getContentType() {
        return getCoyoteResponse().getContentType();
    }


    /**
     * 以ServletOutputStream的方式使用输出缓冲区{@link #outputBuffer}。
     * 
     * @return 返回一个servlet输出流
     * @throws IOException 可能抛出I/O异常
     * @throws IllegalStateException 如果输出缓冲区以PrintWriter方式使用了，则抛出此异常
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (usingWriter) {
            throw new IllegalStateException("coyoteResponse.getOutputStream.ise");
        }
        
        usingOutputStream = true;
        if (outputStream == null) {
            outputStream = new CoyoteOutputStream(outputBuffer);
        }
        return outputStream;
    }


    /**
     * 以PrintWriter的方式使用输出缓冲区{@link #outputBuffer}。
     * 
     * @return 返回一个PrintWriter实例
     * @throws IOException 可能抛出I/O异常
     * @throws IllegalStateException 如果输出缓冲区以ServletOutputStream方式使用了，则抛出此异常
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (usingOutputStream) {
            throw new IllegalStateException("coyoteResponse.getOutputStream.ise");
        }
        
        // 设置编码
        if (ENFORCE_ENCODING_IN_GET_WRITER) {
            setCharacterEncoding(getCharacterEncoding());
        }

        usingWriter = true;
        outputBuffer.checkConverter();
        if (writer == null) {
            writer = new CoyoteWriter(outputBuffer);
        }
        return writer;
    }


    /**
     * 设置响应体的编码方式
     * 
     * @param charset 编码方式
     */
    @Override
    public void setCharacterEncoding(String charset) {
        if (isCommitted()) {
            return;
        }
        
        if (usingWriter) {
            // 已经设置过了
            return;            
        }
        
        try {
            getCoyoteResponse().setCharacterEncoding(charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }

        isCharacterEncodingSet = (charset != null);
    }


    /**
     * 设置响应体长度
     * 
     * @param len 响应体长度
     */
    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }


    /**
     * 设置响应体长度
     * 
     * @param len 响应体长度
     */
    @Override
    public void setContentLengthLong(long len) {
        if (isCommitted()) {
            return;
        }
        
        getCoyoteResponse().setContentLength(len);
    }


    /**
     * 设置响应类型
     * 
     * @param type 响应类型
     */
    @Override
    public void setContentType(String type) {
        if (isCommitted()) {
            return;
        }
        
        if (type == null) {
            getCoyoteResponse().setContentType(null);
            try {
                getCoyoteResponse().setCharacterEncoding(null);
            } catch (UnsupportedEncodingException e) {
                ;
            }
            isCharacterEncodingSet = false;
            return;
        }

        String[] m = MEDIA_TYPE_CACHE.parse(type);
        if (m == null || m[1] == null) {
            getCoyoteResponse().setContentTypeNoCharset(type);
            return;
        }
        
        if (m[1] != null) {
            getCoyoteResponse().setContentTypeNoCharset(m[0]);
            if (!usingWriter) {
                try {
                    getCoyoteResponse().setCharacterEncoding(m[1]);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                isCharacterEncodingSet = true;
            }    
        }
        
    }


    /**
     * 设置缓冲区大小
     * 
     * @param size 缓冲区大小
     * @exception IllegalStateException 缓冲区已被使用了或者响应已经提交了，便会抛出此异常
     */
    @Override
    public void setBufferSize(int size) {
        if (!outputBuffer.isNew() || isCommitted()) {
            throw new IllegalStateException("coyoteResponse.setBufferSize.ise");
        }
        
        outputBuffer.setBufferSize(size);
    }


    /**
     * @return 返回缓冲区大小
     */
    @Override
    public int getBufferSize() {
        return outputBuffer.getBufferSize();
    }


    /**
     * 刷新缓冲区
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public void flushBuffer() throws IOException {
        outputBuffer.flush();
    }


    /**
     * 重置缓冲区
     */
    @Override
    public void resetBuffer() {
        resetBuffer(false);
    }


    /**
     * 重置缓冲区
     * 
     * @param resetWriterStreamFlags 是否对C2B转换器进行重置、以及对
     *        <code>usingOutputStream</code>
     *        <code>usingWriter</code>
     *        <code>isCharacterEncodingSet</code>
     *        进行重置
     */
    public void resetBuffer(boolean resetWriterStreamFlags) {
        if (isCommitted()) {
            throw new IllegalStateException("coyoteResponse.resetBuffer.ise");
        }
        
        outputBuffer.reset(resetWriterStreamFlags);

        if(resetWriterStreamFlags) {
            usingOutputStream = false;
            usingWriter = false;
            isCharacterEncodingSet = false;
        }
    }


    /**
     * @return 响应是否已经提交
     */
    @Override
    public boolean isCommitted() {
        return getCoyoteResponse().isCommitted();
    }


    /**
     * 重置缓冲区使用。恢复到未使用状态
     */
    @Override
    public void reset() {
        getCoyoteResponse().reset();
        outputBuffer.reset();
        usingWriter = false;
        usingOutputStream = false;
        isCharacterEncodingSet = false;
    }


    /**
     * 设置语言环境
     * 
     * @param loc 语言环境
     */
    @Override
    public void setLocale(Locale loc) {
        if (isCommitted()) {
            return;
        }
        
        getCoyoteResponse().setLocale(loc);
        
        if (usingWriter || isCharacterEncodingSet) {
            return;
        }
        
        if (loc == null) {
            try {
                getCoyoteResponse().setCharacterEncoding(null);
            } catch (UnsupportedEncodingException e) {
                ;
            }
        } else {
            Context context = getContext();
            if (context != null) {
                String charset = context.getCharset(loc);
                if (charset != null) {
                    try {
                        getCoyoteResponse().setCharacterEncoding(charset);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * @return 返回语言环境
     */
    @Override
    public Locale getLocale() {
        return getCoyoteResponse().getLocale();
    }
}
