package com.ranni.coyote;

import com.ranni.connector.ApplicationBufferHandler;
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
     * 钩子事件
     */
    private ActionHook hook;

    /**
     * 读取监听器
     */
    volatile ReadListener listener;

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
     * 路径参数
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
    private final HashMap<String,Object> attributes = new HashMap<>();
    
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
    
    
    
    // ------------------------------ 通用方法 ------------------------------
    
    public final Object getNote(int pos) {
        return notes[pos];
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

    public ReadListener getReadListener() {
        return listener;
    }

    public int getAvailable() {
        return available;
    }

    public void setReadListener(ReadListener listener) {
        this.listener = listener;
    }

    public boolean isFinished() {
        return false;
    }

    public boolean isRequestThread() {
        return Thread.currentThread().getId() == threadId;
    }

    public boolean isRead() {
        return false;
    }

    public void setErrorException(IOException ioe) {
    }

    public int doRead(ApplicationBufferHandler inputBuffer) throws IOException {
        return 0;
    }

    public Charset getCharset() throws UnsupportedEncodingException {
        return charset;
    }

    public MessageBytes method() {
        return methodMB;
    }

    public void addPathParameter(String name, String value) {
        
    }

    public String getPathParameter(String name) {
        return null;
    }

    public ServerCookies getCookies() {
        return null;
    }

    public void setServerPort(int port) {
        
    }

    public String getHeader(String name) {
        return null;
    }

    public MimeHeaders getMimeHeaders() {
        return null;
    }

    public MessageBytes queryString() {
        return null;
    }

    public MessageBytes requestURI() {
        return null;
    }

    public Parameters getParameters() {
        return null;
    }

    public Object getAttribute(String name) {
        return null;
    }

    public long getContentLengthLong() {
        return 0;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public void setCharset(Charset charset) {
        
    }

    public int getContentLength() {
        return 0;
    }

    public String getContentType() {
        return null;
    }

    public MessageBytes protocol() {
        return null;
    }

    public MessageBytes scheme() {
        return null;
    }

    public MessageBytes serverName() {
        return null;
    }


    public int getServerPort() {
        return 0;
    }

    public MessageBytes remoteAddr() {
        return null;
    }

    public MessageBytes remoteHost() {
        return null;
    }

    public void setAttribute(String name, Object value) {
        
    }

    public HashMap<String, Object> getAttributes() {
        return null;
    }

    public int getRemotePort() {
        return 0;
    }

    public MessageBytes localName() {
        return null;
    }

    public MessageBytes localAddr() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }
}
