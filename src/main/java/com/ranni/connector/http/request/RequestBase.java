package com.ranni.connector.http.request;

import com.ranni.connector.Connector;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import com.ranni.util.Enumerator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 所有协议请求的基本对象，不仅限于HTTP请求
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-21 23:06
 */
@Deprecated
public abstract class RequestBase implements Request, ServletRequest {
    protected String authorization; // 认证信息
    protected Connector connector; // 连接器
    protected Context context; // 关联的容器
    protected String info; // 实现类的信息
    protected ServletRequest request; // 请求对象
    protected ServletRequest facade = new HttpRequestFacade(this); // 请求对象对外的包装对象
    protected Response response; // 响应对象
    protected Socket socket; // 本次请求的套接字
    protected InputStream input; // socket中的输入流
    protected Wrapper wrapper; // Container的包装器
    protected ServletInputStream stream; // input处理流
    protected BufferedReader reader; // 缓存读取
    protected int contentLength; // 请求体长度
    protected String contentType; // 请求体类型
    protected String protocol; // 协议类型+端口号
    protected String remoteAddr; // 远程主机地址
    protected String scheme; // 协议
    protected boolean secure; // 是否是安全的请求
    protected String serverName; // 服务器名
    protected int serverPort; // 服务器端口号
    protected Map<String, Object> attributes = new HashMap();
    protected String characterEncoding; // 字符编码格式
    protected String remoteHost; // 远程主机完全限定名
    protected ArrayList<Locale> locales = new ArrayList(); // 与此请求相关联首选语言

    public RequestBase() {
    }

    public RequestBase(Socket socket) {
        this.socket = socket;
    }

    @Override
    public String getAuthorization() {
        return this.authorization;
    }

    @Override
    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    @Override
    public Connector getConnector() {
        return connector;
    }

    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String getInfo() {
        return this.info;
    }

    @Override
    public ServletRequest getRequest() {
        return this.facade;
    }

    @Override
    public Response getResponse() {
        return this.response;
    }

    @Override
    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public Socket getSocket() {
        return this.socket;
    }

    @Override
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public InputStream getStream() {
        return this.input;
    }

    @Override
    public void setStream(InputStream input) {
        this.input = input;
    }

    @Override
    public Wrapper getWrapper() {
        return this.wrapper;
    }

    @Override
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public ServletInputStream createInputStream() throws IOException {
        return new RequestStream(this);
    }

    @Override
    public void finishRequest() throws IOException {
        if (reader != null) {
            reader.close();
        }

        if (stream != null) {
            stream.close();
        }
    }

    /**
     * 置为初始值，便于下次使用
     */
    @Override
    public void recycle() {
        authorization = null;
        connector = null;
        context = null;
        info = null;
        request = null;
        response = null;
        input = null;
        wrapper = null;
        stream = null;
        reader = null;
        contentLength = -1;
        contentType = null;
        protocol = null;
        remoteAddr = null;
        scheme = null;
        secure = false;
        serverName = null;
        serverPort = 0;
        attributes.clear();
        characterEncoding = null;
        remoteHost = null;
        locales.clear();
    }

    @Override
    public void setContentLength(int length) {
        this.contentLength = length;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public void setRemoteAddr(String remote) {
        this.remoteAddr = remote;
    }

    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public void setServerName(String name) {
        this.serverName = name;
    }

    @Override
    public void setServerPort(int port) {
        this.serverPort = port;
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
        return this.characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        // 测试用s编码格式能否正确解码'a'
        byte[] buffer = {'a'};
        String dummy = new String(buffer, s);

        this.characterEncoding = s;
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
        if (reader != null) throw new IllegalStateException("getReader已被调用");

        if (stream == null) stream = createInputStream();

        return this.stream;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public int getServerPort() {
        return this.serverPort;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (stream != null) throw new IllegalStateException("getInputStream已经被调用！");
        if (reader == null) {
            String encoding = getCharacterEncoding();
            if (encoding == null) encoding = "ISO-8859-1";
            InputStreamReader isr = new InputStreamReader(input, encoding);
            reader = new BufferedReader(isr);
        }
        return reader;
    }

    @Override
    public void setRemoteHost(String host) {
        this.remoteHost = host;
    }

    @Override
    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return this.remoteHost;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) throw new IllegalArgumentException("参数name不能为空！");
        if (value == null) {
            removeAttribute(name);
            return;
        }
        synchronized (attributes) {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String s) {
        synchronized (attributes) {
            attributes.remove(s);
        }
    }

    @Override
    public Locale getLocale() {
        synchronized (locales) {
            if (locales == null) return null;
            else return locales.get(0);
        }
    }

    @Override
    public Enumeration getLocales() {
        return new Enumerator(locales);
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public abstract RequestDispatcher getRequestDispatcher(String s);

    /**
     * 返回真实路径，暂未实现
     * @param s
     * @return
     */
    @Override
    public String getRealPath(String s) {
        return null;
    }

    /**
     * 增加本地语言环境
     * @param locale
     */
    public void addLocale(Locale locale) {
        synchronized (locales) {
            locales.add(locale);
        }
    }
}
