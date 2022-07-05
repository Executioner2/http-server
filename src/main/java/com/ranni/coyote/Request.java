package com.ranni.coyote;

import com.ranni.connector.ApplicationBufferHandler;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.buf.UDecoder;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.http.Parameters;
import com.ranni.util.http.ServerCookies;

import javax.servlet.ReadListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Title: HttpServer
 * Description:
 * 处理底层的socket，将http的请求中字节流的
 * 处理封装到此类中。
 * 注意：此类非HttpServletRequest的实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 23:29
 * @Ref org.apache.coyote.Request
 */
public final class Request {

    /**
     * 初始化cookie数量
     */
    private static final int INITIAL_COOKIE_SIZE = 4;

    /**
     * 钩子事件
     */
    private ActionHook hook;

    /**
     * 缓冲区可读数据量
     */
    private int available;

    /**
     * 内部便签？
     */
    private final Object notes[] = new Object[Constants.MAX_NOTES];

    /**
     * 请求线程id
     */
    private long threadId;

    /**
     * 响应此请求的服务器端口号
     */
    private int serverPort = -1;

    /**
     * 接收此请求的端口号
     */
    private int localPort;

    /**
     * 发送此请求的客户端端口号
     */
    private int remotePort;
    
    /**
     * cookie
     */
    private final ServerCookies serverCookies = new ServerCookies(INITIAL_COOKIE_SIZE);

    /**
     * why - 读取了所有数据？
     */
    private final AtomicBoolean allDataReadEventSent = new AtomicBoolean(false);

    /**
     * 是否有expect标头
     */
    private boolean expectation;

    /**
     * 请求包处理状态
     */
    private final RequestInfo reqProcessorMX = new RequestInfo(this);

    /* ==================================== 消息字节 start ==================================== */

    /**
     * 请求的协议（HTTP 或 HTTPS）
     */
    private final MessageBytes schemeMB = MessageBytes.newInstance();

    /**
     * 请求的方法
     */
    private final MessageBytes methodMB = MessageBytes.newInstance();

    /**
     * 请求URI
     */
    private final MessageBytes uriMB = MessageBytes.newInstance();

    /**
     * 解码后的URI
     */
    private final MessageBytes decodedUriMB = MessageBytes.newInstance();

    /**
     * GET请求携带的查询参数
     */
    private final MessageBytes queryMB = MessageBytes.newInstance();

    /**
     * 请求协议和协议版本
     */
    private final MessageBytes protoMB = MessageBytes.newInstance();

    /**
     * 请求发起者或最后一个代理服务器的IP
     */
    private final MessageBytes remoteAddrMB = MessageBytes.newInstance();

    
    private final MessageBytes peerAddrMB = MessageBytes.newInstance();

    /**
     * 接收此请求的服务器名
     */
    private final MessageBytes localNameMB = MessageBytes.newInstance();

    /**
     * 响应此请求的服务器名
     */
    private final MessageBytes serverNameMB = MessageBytes.newInstance();

    /**
     * 请求发起者或最后一个代理服务器的域名
     */
    private final MessageBytes remoteHostMB = MessageBytes.newInstance();

    /**
     * 接收此请求的服务器IP
     */
    private final MessageBytes localAddrMB = MessageBytes.newInstance();
    
    /* ==================================== 消息字节 end ==================================== */
    
    
    /**
     * 请求头
     */
    private final MimeHeaders headers = new MimeHeaders();
    private final Map<String,String> trailerFields = new HashMap<>();

    /**
     * 路径参数。例如在url中携带的jsessionid<br>
     * 例子：<br>
     * http://localhost/test;jsessionid=F0D358CE192599DE7BF6AD271394D3BF
     */
    private final Map<String,String> pathParameters = new HashMap<>();

    /**
     * 输入缓冲区 
     */
    private InputBuffer inputBuffer = null;

    /**
     * URL解析器
     */
    private final UDecoder urlDecoder = new UDecoder();

    /**
     * 请求体数据长度
     */
    private long contentLength = -1;

    /**
     * 请求体数据类型
     */
    private MessageBytes contentTypeMB;

    /**
     * 字符编码器
     */
    private Charset charset;

    /**
     * 编码格式
     */
    private String characterEncoding;

    /**
     * 请求参数
     */
    private final Parameters parameters = new Parameters();

    /**
     * 属性 
     */
    private final HashMap<String, Object> attributes = new HashMap<>();
    
    private Response coyoteResponse;

    /**
     * 读取了的字节数量
     */
    private long bytesRead;

    /**
     * 请求时间
     */
    private long startTime = -1;

    /**
     * 是否允许发送文件
     */
    private boolean sendfile = true;

    private Exception errorException;

    /* ==================================== 异步处理请求相关属性 start ==================================== */

    /**
     * 读取监听器
     */
    volatile ReadListener listener;
    
    /**
     * 只有在调用了isReady()后才触发监听器
     */
    private boolean fireListener = false;

    /**
     * 跟踪读取注册，防止重复读取
     */
    private boolean registeredForRead = false;

    /**
     * 异步读取的锁
     */
    private final Object nonBlockingStateLock = new Object();

    /* ==================================== 异步处理请求相关属性 end ==================================== */
    
    
    // ------------------------------ 通用方法 ------------------------------
    
    public final Object getNote(int pos) {
        return notes[pos];
    }
    

    public final void setNote(int pos, Object value) {
        notes[pos] = value;
    }


    /**
     * 事件响应
     * 
     * @param actionCode 动作代码
     * @param param 携带参数
     */
    public void action(ActionCode actionCode, Object param) {
        if (hook != null) {
            if (param == null) {
                hook.action(actionCode, this);
            } else {
                hook.action(actionCode, param);
            }
        }
    }


    /**
     * @return 如果返回<b>true</b>，表示已经完成了对请求体的读取
     */
    public boolean isFinished() {
        AtomicBoolean result = new AtomicBoolean(false);
        action(ActionCode.REQUEST_BODY_FULLY_READ, result);
        return result.get();
    }


    /**
     * @return 如果返回<b>true</b>，表示请求正处于当前线程
     */
    public boolean isRequestThread() {
        return Thread.currentThread().getId() == threadId;
    }


    /**
     * 设置请求错误异常。如果有异常，对请求的处理将不成功
     * 
     * @param ioe 设置的请求错误异常
     */
    public void setErrorException(IOException ioe) {
        errorException = ioe;
    }

    
    /**
     * @return 返回已经读取了的数据量
     */
    public long getBytesRead() {
        return bytesRead;
    }

    public RequestInfo getRequestProcessor() {
        return reqProcessorMX;
    }
    
    public void updateCounters() {
        reqProcessorMX.updateCounters();
    }


    /**
     * @return 如果返回<b>true</b>，则表示已经将请求交给了容器处理
     */
    public boolean isProcessing() {
        return reqProcessorMX.getStage() == Constants.STAGE_SERVICE;
    }

    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    

    public HashMap<String, Object> getAttributes() {
        return attributes;
    }
    
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    
    public void setExpectation(boolean expectation) {
        this.expectation = expectation;
    }

    
    public Exception getErrorException() {
        return errorException;
    }
    

    public boolean isExceptionPresent() {
        return errorException != null;
    }
    
    
    public boolean hasExpectation() {
        return expectation;
    }

    public InputBuffer getInputBuffer() {
        return inputBuffer;
    }


    public void setInputBuffer(InputBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
    }


    @Override
    public String toString() {
        return "R( " + requestURI().toString() + ")";
    }
    

    public long getStartTime() {
        return startTime;
    }
    

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    

    public long getThreadId() {
        return threadId;
    }
    

    public void clearRequestThread() {
        threadId = 0;
    }
    

    public void setRequestThread() {
        threadId = Thread.currentThread().getId();
    }
    
    
    public UDecoder getURLDecoder() {
        return this.urlDecoder;
    }


    public void recycle() {
        bytesRead = 0;

        contentLength = -1;
        contentTypeMB = null;
        charset = null;
        characterEncoding = null;
        expectation = false;
        headers.recycle();
        trailerFields.clear();
        serverNameMB.recycle();
        serverPort = -1;
        localAddrMB.recycle();
        localNameMB.recycle();
        localPort = -1;
        peerAddrMB.recycle();
        remoteAddrMB.recycle();
        remoteHostMB.recycle();
        remotePort = -1;
        available = 0;
        sendfile = true;

        serverCookies.recycle();
        parameters.recycle();
        pathParameters.clear();

        uriMB.recycle();
        decodedUriMB.recycle();
        queryMB.recycle();
        methodMB.recycle();
        protoMB.recycle();

        schemeMB.recycle();

//        remoteUser.recycle();
//        remoteUserNeedsAuthorization = false;
//        authType.recycle();
        attributes.clear();

        errorException = null;

        listener = null;
        synchronized (nonBlockingStateLock) {
            fireListener = false;
            registeredForRead = false;
        }
        allDataReadEventSent.set(false);

        startTime = -1;
        threadId = 0;
    }
    
    
    // ------------------------------ 请求数据处理 ------------------------------
    
    /**
     * 从缓冲区读取数据并放到ApplicationBufferHandler中
     *
     * @param handler ApplicationBufferHandler
     * @return 返回已经读取了的数据量
     * @throws IOException 可能抛出I/O异常
     */
    public int doRead(ApplicationBufferHandler handler) throws IOException {
        if (getBytesRead() == 0 && !coyoteResponse.isCommitted()) {
            // 还没有读取到数据并且响应也还没提交就执行请求读取请求包的钩子
            action(ActionCode.ACK, ContinueResponseTiming.ON_REQUEST_BODY_READ);
        }

        int n = inputBuffer.doRead(handler);
        if (n > 0) {
            bytesRead += n;
        }
        return n;
    }
    
    /**
     * @return 返回此请求使用的默认编码器
     * @throws UnsupportedEncodingException 可能抛出不支持的编码异常
     */
    public Charset getCharset() throws UnsupportedEncodingException {
        if (charset == null) {
            getCharacterEncoding();
            if (characterEncoding != null) {
                charset = B2CConverter.getCharset(characterEncoding);
            }
        }
        return charset;
    }


    /**
     * @return 返回编码格式。如果编码格式为null则根据请求体类型返回对应的编码格式
     */
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            characterEncoding = getCharsetFromContentType(getContentType());
        }
        return characterEncoding;
    }


    /**
     * 根据请求内容类型中的charset（如果有的话），返回对应的编码格式
     *
     * @param contentType 请求内容类型
     * @return 返回的编码格式
     */
    private static String getCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        int start = contentType.indexOf("charset=");
        if (start < 0) {
            return null;
        }

        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end > 0) {
            encoding = encoding.substring(0, end);
        }

        return encoding.trim();
    }


    /**
     * 设置编码器
     *
     * @param charset 要设置的编码器
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
        this.characterEncoding = charset.name();
    }
    

    /**
     * 返回请求体的长度。从{@link #getContentLengthLong()}中 </br>
     * 取得一个long类型的内容长度，如果该值大于等于0x7fffffff，则返回-1
     *
     * @return 返回请求体的长度
     */
    public int getContentLength() {
        long length = getContentLengthLong();
        if (length < Integer.MAX_VALUE) {
            return (int) length;
        }
        return -1;
    }

    
    /**
     * 设置请求体的长度
     *
     * @param len 请求体的内容长度
     */
    public void setContentLength(long len) {
        this.contentLength = len;
    }


    /**
     * @return 返回请求体类型
     */
    public String getContentType() {
        contentType();
        if (contentTypeMB == null || contentTypeMB.isNull()) {
            return null;
        }
        return contentTypeMB.toString();
    }

    public void setResponse(Response response) {
        this.coyoteResponse = response;
    }
    
    public Response getResponse() {
        return coyoteResponse;
    }
    
    public void setHook(ActionHook hook) {
        this.hook = hook;
    }
    
    public ActionHook getHook() {
        return hook;
    }
    
    public void setContentType(MessageBytes mb) {
        contentTypeMB = mb;
    }

    public void setContentType(String type) {
        contentTypeMB.setString(type);
    }

    public int getRemotePort() {
        return remotePort;
    }
    
    public void setRemotePort(int port) {
        this.remotePort = port;
    }

    public int getLocalPort() {
        return localPort;
    }
    
    public void setLocalPort(int port) {
        this.localPort = port;
    }
    
    public void addPathParameter(String name, String value) {
        pathParameters.put(name, value);
    }

    public String getPathParameter(String name) {
        return pathParameters.get(name);
    }

    public ServerCookies getCookies() {
        return serverCookies;
    }

    public void setServerPort(int port) {
        serverPort = port;
    }
    
    public int getServerPort() {
        return serverPort;
    }

    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    public MimeHeaders getMimeHeaders() {
        return headers;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public long getContentLengthLong() {
        if (contentLength > -1) {
            return contentLength;
        }

        MessageBytes mb = headers.getUniqueValue("content-length");
        contentLength = mb == null || mb.isNull() ? -1 : mb.getLong();
        
        return contentLength;
    }

    public boolean getSendfile() {
        return sendfile;
    }

    public void setSendfile(boolean sendfile) {
        this.sendfile = sendfile;
    }


    /**
     * @return 返回<b>true</b>，表示支持请求重定向
     */
    public boolean getSupportsRelativeRedirects() {
        if (protocol().equals("") || protocol().equals("HTTP/1.0")) {
            return false;
        }
        return true;
    }


    public Map<String, String> getTrailerFields() {
        return trailerFields;
    }
    
    // ------------------------------ 消息字节 ------------------------------
    
    public MessageBytes contentType() {
        if (contentTypeMB == null) {
            contentTypeMB = headers.getValue("content-type");
        }
        return contentTypeMB;
    }
    

    public MessageBytes protocol() {
        return protoMB;
    }
    

    public MessageBytes scheme() {
        return schemeMB;
    }
    
    public MessageBytes decodedURI() {
        return decodedUriMB;
    }

    public MessageBytes serverName() {
        return serverNameMB;
    }
    
    
    public MessageBytes remoteAddr() {
        return remoteAddrMB;
    }
    

    public MessageBytes peerAddr() {
        return peerAddrMB;
    }
    
    
    public MessageBytes method() {
        return methodMB;
    }
    
    
    public MessageBytes queryString() {
        return queryMB;
    }
    

    public MessageBytes requestURI() {
        return uriMB;
    }
    
    
    public MessageBytes remoteHost() {
        return remoteHostMB;
    }
    

    public MessageBytes localName() {
        return localNameMB;
    }
    

    public MessageBytes localAddr() {
        return localAddrMB;
    }


    // TODO ------------------------------ ReadListener相关（没有用到） ------------------------------

    /**
     * @return 如果返回<b>true</b>，则表示缓冲区已有数据准备好读取了
     */
    public boolean isReady() {
        boolean ready = false;
        synchronized (nonBlockingStateLock) {
            if (registeredForRead) {
                // 已经注册过了读取监听器了（已经调用过了setReadListener()方法）
                fireListener = true;
                return false;
            }
            ready = checkRegisterForRead();
            fireListener = !ready;
        }

        return ready;
    }


    /**
     * 检查读取监听器注册，如果没注册就注册
     *
     * @return 如果返回<b>true</b>，则表示缓冲区已经有数据准备好读取了
     */
    private boolean checkRegisterForRead() {
        AtomicBoolean ready = new AtomicBoolean(false);
        synchronized (nonBlockingStateLock) {
            if (!registeredForRead) {
                action(ActionCode.NB_READ_INTEREST, ready);
                registeredForRead = !ready.get();
            }
        }
        return ready.get();
    }


    /**
     * 可用的数据
     *
     * @throws IOException 可能抛出I/O异常
     */
    public void onDataAvailable() throws IOException {
        boolean fire = false;
        synchronized (nonBlockingStateLock) {
            registeredForRead = false;
            if (fireListener) {
                fireListener = false;
                fire = true;
            }
        }
        if (fire) {
            listener.onDataAvailable();
        }
    }


    /**
     * 设置可读数据量
     * 
     * @param available 可读数据量
     */
    public void setAvailable(int available) {
        this.available = available;
    }
    

    /**
     * @return 返回可读数据量
     */
    public int getAvailable() {
        return available;
    }


    /**
     * @return 返回数据读取监听器
     */
    public ReadListener getReadListener() {
        return listener;
    }
    

    /**
     * 注册读取监听器
     *
     * @param listener 读取监听器
     */
    public void setReadListener(ReadListener listener) {
        if (listener == null) {
            throw new NullPointerException("请求中的读取监听器为null！");
        }

        if (getReadListener() != null) {
            throw new IllegalStateException("已经有读取监听器了！");
        }

        AtomicBoolean result = new AtomicBoolean(false);
        action(ActionCode.ASYNC_IS_ASYNC, result);
        if (!result.get()) {
            throw new IllegalStateException("不可进行异步读取！");
        }

        this.listener = listener;

        /**
         * 请求还没处理完成，且已经有可读数据准备好了
         */
        if (!isFinished() && isReady()) {
            synchronized (nonBlockingStateLock) {
                registeredForRead = true;
                fireListener = true;
            }
            action(ActionCode.DISPATCH_READ, null);
            if (!isRequestThread()) {
                // why - 不在容器线程上，需要调度执行
                action(ActionCode.DISPATCH_EXECUTE, null);
            }
        }
    }
    
}
