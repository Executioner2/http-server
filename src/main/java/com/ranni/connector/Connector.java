package com.ranni.connector;

import com.ranni.container.Container;
import com.ranni.core.Service;
import com.ranni.connector.coyote.AbstractProtocol;
import com.ranni.connector.coyote.Adapter;
import com.ranni.connector.coyote.ProtocolHandler;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.util.LifecycleSupport;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Title: HttpServer
 * Description:
 * HTTP连接器呀我淦
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 21:52
 */
public class Connector implements Lifecycle {
    private ServerSocket serverSocket; // 服务器socket
    private boolean stopped = false; // 连接器线程停止标志位
    private Thread thread; // 线程
    private String threadName; // 线程名
    private int debug = Logger.INFORMATION; // 日志输出级别
    private int port = 8080; // 端口号    
    private boolean initialize; // 连接器初始化标志位
    private boolean started; // 连接器启动标志
    private LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具类实例
    private String address; // IP地址
    private int acceptCount = 10; // 最大连接数
    private Service service; // 所属的服务实例
    private Charset uriCharset = StandardCharsets.UTF_8; // URI编码方式
    private boolean xPoweredBy; // 是否开启X-Powered-By标签
    private int proxyPort; // 服务器端口号
    private String proxyName; // 服务器名
    private boolean allowTrace; // 是否支持TRACE请求方法
    private boolean useIPVHosts; // 主机标识方式，是IP标识还是name标识
    private int maxCookieCount = 200;
    protected boolean secure; // 安全标志位
    protected String scheme; // 协议类型
    protected int redirectPort = 80; // 转发端口
    protected Container container; // 容器
    protected int maxPostSize = 2 * 1024 * 1024; // Post的最大请求体积
    protected int maxParameterCount = 10000; // 容器自动解析参数数量
    protected boolean useBodyEncodingForURI; // 是否将请求体的解码格式应用于URI
    protected HashSet<String> parseBodyMethodsSet; // 可以被解析请求体的请求方法集合
    protected String parseBodyMethods = "POST"; // 可以被解析请求体的请求方法    
    protected final ProtocolHandler protocolHandler; // 协议处理器

    public Connector() {
        this("HTTP/1.1");
//        threadName = "HttpConnector@" + this.hashCode();
//        thread = new Thread(this);
    }
    
    public Connector(String protocol) {
        ProtocolHandler p = null;
        try {
            p = ProtocolHandler.create(protocol, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (p != null) {
            protocolHandler = p;
        } else {
            protocolHandler = null;
        }
    }
    
    
    public Connector(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    

    /**
     * 取得日志输出级别
     *
     * @return
     */
    public int getDebug() {
        return debug;
    }

    
    /**
     * 设置日志输出级别
     *
     * @param debug
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * @return TODO 是否重置外观对象
     */
    public boolean getDiscardFacades() {
        return true;
    }


    /**
     * @return 返回Post请求的最大体积，-1表示无限制
     */
    public int getMaxPostSize() {
        return maxPostSize;
    }


    /**
     * @return 返回容器自动解析的参数数量
     */
    public int getMaxParameterCount() {
        return maxParameterCount;
    }


    /**
     * @return 如果为true，则表示URI的解码格式同请求体一样
     */
    public boolean getUseBodyEncodingForURI() {
        return useBodyEncodingForURI;
    }


    /**
     * 此请求方法是否包含在可被解析请求体的方法集合中
     *
     * @param method 请求方法
     * @return 如果返回true，则表示可以解析此请求的
     *         请求体，否则反之
     */
    public boolean isParseBodyMethod(String method) {
        return parseBodyMethodsSet.contains(method);
    }

    /**
     * 设置可以被解析请求体的请求方法
     * 
     * @param methods 可以被解析请求体的方法。如果有多个用','分隔（不包括''）
     */
    public void setParseBodyMethods(String methods) {
        HashSet<String> methodSet = new HashSet<>();

        if (methodSet != null) {
            methodSet.addAll(Arrays.asList(methods.split("\\s*,\\s*")));
        }
        
        if (methodSet.contains("TRACE")) {
            throw new IllegalArgumentException("coyoteConnector.parseBodyMethodNoTrace");
        }

        this.parseBodyMethods = methods;
        this.parseBodyMethodsSet = methodSet;
    }


    /**
     * @return 返回可以被解析请求体的请求方法
     */
    public String getParseBodyMethods() {
        return this.parseBodyMethods;
    }
    

    public int getMaxCookieCount() {
        return maxCookieCount;
    }


    public Charset getURICharset() {
        return uriCharset;
    }

    
    /**
     * 是否在响应头中标明后端使用的什么框架或语言
     *
     * @return 如果返回<b>true</b>，则开启X-Powered-By。
     */
    public boolean getXPoweredBy() {
        return xPoweredBy;
    }


    /**
     * @param xPoweredBy 如果为<b>true</b>，则表示要开启X-Powered-By
     */
    public void setXPoweredBy(boolean xPoweredBy) {
        this.xPoweredBy = xPoweredBy;
    }


    /**
     * @return 返回此连接器关联的服务器名
     */
    public String getProxyName() {
        return this.proxyName;
    }


    /**
     * @return 返回此连接器关联的服务器端口
     */
    public int getProxyPort() {
        return this.proxyPort;
    }


    /**
     * 设置此连接器关联的服务器端口
     * 
     * @param proxyPort 服务器端口
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }


    /**
     * 设置此连接器关联的服务器名
     * 
     * @param proxyName 服务器名
     */
    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }
    

    /**
     * 返回容器
     * 
     * @return
     */
    public Container getContainer() {
        return this.container;
    }

    
    /**
     * 设置与此连接关联容器
     * 
     * @param container
     */
    public void setContainer(Container container) {
        this.container = container;
    }

    
    /**
     * TODO 返回dns查询标志
     * 
     * @return
     */
    public boolean getEnableLookups() {
        return false;
    }

    
    /**
     * TODO 设置dns查询标志
     * 
     * @param enableLookups
     */
    public void setEnableLookups(boolean enableLookups) {

    }

    
    /**
     * TODO 返回此实现类的信息和版本号
     * 
     * @return
     */
    public String getInfo() {
        return null;
    }

    
    /**
     * 返回转发端口
     * 
     * @return
     */
    public int getRedirectPort() {
        return this.redirectPort;
    }

    
    /**
     * 设置转发端口
     * 
     * @param redirectPort
     */
    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
    }
    

    /**
     * 返回协议类型
     * 
     * @return
     */
    public String getScheme() {
        return this.scheme;
    }
    

    /**
     * 设置协议类型
     * @param scheme
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
    

    /**
     * 返回安全标志
     * 
     * @return
     */
    public boolean getSecure() {
        return this.secure;
    }
    

    /**
     * 设置安全标志
     * 
     * @param secure
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }


    /**
     * 返回所属的服务实例
     * 
     * @return
     */
    public Service getService() {
        return this.service;
    }

    /**
     * 设置所属的服务实例
     * @param service
     */
    public void setService(Service service) {
        this.service = service;
    }


    /**
     * 创建请求对象
     * @return
     */
    public Request createRequest() {
        Request request = new Request(this);
        return request;
    }
    

    /**
     * 创建响应对象
     * @return
     */
    public Response createResponse() {
        Response response = new Response();
        return response;
    }


    /**
     * 设置连接器IP地址
     *
     * @param ia
     */
    public void setAddress(InetAddress ia) {
        if (protocolHandler instanceof AbstractProtocol) {
            ((AbstractProtocol) protocolHandler).setAddress(ia);
        }
    }


    /**
     * 设置调度器
     * 
     * @param adapter 调度器
     */
    public void setAdapter(Adapter adapter) {
        protocolHandler.setAdapter(adapter);
    }
    
    

    /**
     * 启动前初始化
     *
     * @throws Exception
     */
    public void initialize() throws LifecycleException {
        if (initialize)
            throw new LifecycleException("连接器已初始化！");

        initialize = true;
        if (debug >= Logger.WARNING)
            log("连接器初始化");

        if (null == parseBodyMethodsSet) {
            setParseBodyMethods(getParseBodyMethods());
        }
    }
    

    /**
     * 添加监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }
    

    /**
     * 返回所有监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()}
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }
    

    /**
     * 移除指定监听器
     *
     * @see {@link LifecycleSupport#removeLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }
    

    /**
     * 启动连接器
     * 启动顺序
     *  1、启动容器
     *  2、启动处理器池
     *  3、启动此连接器线程
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws LifecycleException {        
        if (started) throw new LifecycleException("此connector连接器实例已经启动！");
        if (protocolHandler == null) {
            throw new LifecycleException("协议处理器不能为null！");
        }
        
        log("启动连接器！");

        // 连接器启动
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;

        try {
            protocolHandler.start();

            // 连接器启动后
            lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
            log("连接器启动完成！");            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    

    /**
     * 停止连接器
     * 停止顺序
     *  1、停止此连接器线程
     *  2、停止处理器池
     *  3、停止容器
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("此connector连接器实例已经停止！");
        
        // 连接器停止
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);

        stopped = true;
        
        try {
            protocolHandler.stop();
        } catch (Exception e) {
            log("HttpConnector.stoppingSocket", e);
        }

        // 连接器停止后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);
    }

    /**
     * 记录信息到日志文件
     *
     * @param msg
     */
    private void log(String msg) {
        Logger logger = container.getLogger();
        String localName = threadName;
        if (localName == null)
            localName = "HttpConnector";
        if (logger != null)
            logger.log(localName + " " + msg);
        else
            System.out.println(localName + " " + msg);
    }

    
    /**
     * 记录信息到日志文件
     *
     * @param msg
     * @param throwable
     */
    private void log(String msg, Throwable throwable) {
        Logger logger = container.getLogger();
        String localName = threadName;
        if (localName == null)
            localName = "HttpConnector";
        if (logger != null)
            logger.log(localName + " " + msg, throwable);
        else {
            System.out.println(localName + " " + msg);
            throwable.printStackTrace(System.out);
        }

    }

    
    /**
     * 返回端口号
     * 
     * @return
     */
    public int getPort() {
        return port;
    }


    /**
     * 设置端口号
     * 
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
        if (protocolHandler instanceof AbstractProtocol) {
            ((AbstractProtocol) protocolHandler).setPort(port);
        }
    }


    /**
     * 返回连接器IP地址
     * 
     * @return
     */
    public String getAddress() {
        return address;
    }


    /**
     * 设置连接器IP地址
     * 
     * @param address
     */
    public void setAddress(String address) {
        this.address = address;
        if (protocolHandler instanceof AbstractProtocol) {
            try {
                ((AbstractProtocol<?>) protocolHandler).setAddress(InetAddress.getByName(address));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 返回最大连接数
     * 
     * @return
     */
    public int getAcceptCount() {
        return acceptCount;
    }


    /**
     * 设置最大连接数
     * 
     * @param acceptCount
     */
    public void setAcceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
    }


    /**
     * @return 如果返回<b>true</b>，则表示支持TRACE请求方法
     */
    public boolean getAllowTrace() {
        return this.allowTrace;
    }


    /**
     * 设置是否支持TRACE请求方法的标志位
     *  
     * @param allowTrace 如果为<b>true</b>，则表示支持TRACE请求方法
     */
    public void setAllowTrace(boolean allowTrace) {
        this.allowTrace = allowTrace;
    }


    /**
     * @return 如果返回<b>true</b>，则机由IP标识。否则，主机由名字标识
     */
    public boolean getUseIPVHosts() {
        return useIPVHosts;
    }


    /**
     * 设置主机标识方式。
     * 
     * @param useIPVHosts 如果为<b>true</b>，主机由IP标识。否则，主机由名字标识
     */
    public void setUseIPVHosts(boolean useIPVHosts) {
        this.useIPVHosts = useIPVHosts;
    }
}
