package com.ranni.connector;

import com.ranni.connector.http.request.HttpRequestImpl;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.HttpResponseImpl;
import com.ranni.connector.http.response.Response;
import com.ranni.connector.processor.DefaultProcessorPool;
import com.ranni.connector.processor.Processor;
import com.ranni.connector.processor.ProcessorPool;
import com.ranni.connector.socket.DefaultServerSocketFactory;
import com.ranni.connector.socket.ServerSocketFactory;
import com.ranni.container.Container;
import com.ranni.core.Service;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.util.LifecycleSupport;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
public class HttpConnector implements Connector, Runnable, Lifecycle {
    private ServerSocketFactory factory; // 获取服务器socket的工厂
    private ServerSocket serverSocket; // 服务器socket
    private boolean stopped = false; // 连接器线程停止标志位
    private Thread thread; // 线程
    private String threadName; // 线程名
    private boolean secure; // 安全标志位
    private int debug = Logger.INFORMATION; // 日志输出级别
    private int port = 8080; // 端口号    
    private boolean initialize; // 连接器初始化标志位
    private boolean started; // 连接器启动标志
    private LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具类实例
    private String address; // IP地址
    private int acceptCount = 10; // 最大连接数
    private Service service; // 所属的服务实例
    private Charset uriCharset = StandardCharsets.UTF_8; // URI编码方式
    
    protected String scheme; // 协议类型
    protected int redirectPort = 80; // 转发端口
    protected Container container; // 容器
    protected ProcessorPool processorPool; // 处理器池
    protected int maxPostSize = 2 * 1024 * 1024; // Post的最大请求体积
    protected int maxParameterCount = 10000; // 容器自动解析参数数量
    protected boolean useBodyEncodingForURI; // 是否将请求体的解码格式应用于URI
    protected HashSet<String> parseBodyMethodsSet; // 可以被解析请求体的请求方法集合
    protected String parseBodyMethods = "POST"; // 可以被解析请求体的请求方法    


    public HttpConnector() {
        threadName = "HttpConnector@" + this.hashCode();
        thread = new Thread(this);
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
    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * @return TODO 是否重置外观对象
     */
    @Override
    public boolean getDiscardFacades() {
        return true;
    }


    /**
     * @return 返回Post请求的最大体积，-1表示无限制
     */
    @Override
    public int getMaxPostSize() {
        return maxPostSize;
    }


    /**
     * @return 返回容器自动解析的参数数量
     */
    @Override
    public int getMaxParameterCount() {
        return maxParameterCount;
    }


    /**
     * @return 如果为true，则表示URI的解码格式同请求体一样
     */
    @Override
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
    @Override
    public boolean isParseBodyMethod(String method) {
        return parseBodyMethodsSet.contains(method);
    }

    @Override
    public int getMaxCookieCount() {
        return 0;
    }


    @Override
    public Charset getURICharset() {
        return uriCharset;
    }

    /**
     * 返回容器
     * 
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    
    /**
     * 设置与此连接关联容器
     * 
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    
    /**
     * TODO 返回dns查询标志
     * 
     * @return
     */
    @Override
    public boolean getEnableLookups() {
        return false;
    }

    
    /**
     * TODO 设置dns查询标志
     * 
     * @param enableLookups
     */
    @Override
    public void setEnableLookups(boolean enableLookups) {

    }

    
    /**
     * 取得ServerSocketFactory对象
     * 
     * @return
     */
    @Override
    public ServerSocketFactory getFactory() {
        if (factory == null) {
            setFactory(DefaultServerSocketFactory.getServerSocketFactory());
        }
        return factory;
    }

    
    /**
     * 设置ServerSocketFactory对象
     * 
     * @param factory
     */
    @Override
    public void setFactory(ServerSocketFactory factory) {
        this.factory = factory;
    }

    
    /**
     * TODO 返回此实现类的信息和版本号
     * 
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }

    
    /**
     * 返回转发端口
     * 
     * @return
     */
    @Override
    public int getRedirectPort() {
        return this.redirectPort;
    }

    
    /**
     * 设置转发端口
     * 
     * @param redirectPort
     */
    @Override
    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
    }
    

    /**
     * 返回协议类型
     * 
     * @return
     */
    @Override
    public String getScheme() {
        return this.scheme;
    }
    

    /**
     * 设置协议类型
     * @param scheme
     */
    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
    

    /**
     * 返回安全标志
     * 
     * @return
     */
    @Override
    public boolean getSecure() {
        return this.secure;
    }
    

    /**
     * 设置安全标志
     * 
     * @param secure
     */
    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }


    /**
     * 返回所属的服务实例
     * 
     * @return
     */
    @Override
    public Service getService() {
        return this.service;
    }

    /**
     * 设置所属的服务实例
     * @param service
     */
    @Override
    public void setService(Service service) {
        this.service = service;
    }


    /**
     * 创建请求对象
     * @return
     */
    @Override
    public Request createRequest() {
        HttpRequestImpl request = new HttpRequestImpl();
        request.setConnector(this);
        return request;
    }
    

    /**
     * 创建响应对象
     * @return
     */
    @Override
    public Response createResponse() {
        HttpResponseImpl response = new HttpResponseImpl();
        response.setConnector(this);
        return response;
    }
    

    /**
     * 取得server socket
     * 
     * @return
     */
    private ServerSocket open() throws IOException, IllegalStateException {
        
        ServerSocketFactory factory = getFactory();
        
        if (address == null) {
            log("创建0.0.0.0/0.0.0.0地址的ServerSocket");
            try {
                return factory.createSocket(port, acceptCount);
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + port);
            }
        }
            
        try {
            InetAddress ia = InetAddress.getByName(address);
            log("创建指定地址的ServerSocket");
            try {
                return factory.createSocket(port, acceptCount, ia);
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + address + ":" + port);
            }
            
        } catch (Exception e) {
            log("创建0.0.0.0/0.0.0.0地址的ServerSocket");
            try {
                return factory.createSocket(port, acceptCount);
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + port);
            }
        }
        
    }
    

    /**
     * 启动前初始化
     *
     * @throws Exception
     */
    @Override
    public void initialize() throws LifecycleException {
        if (initialize)
            throw new LifecycleException("连接器已初始化！");

        initialize = true;
        if (debug >= Logger.WARNING)
            log("连接器初始化");

        try {
            serverSocket = open();
        } catch (IOException e) {
            throw new LifecycleException(threadName + e);
        }
        
        // 创建处理器线程池，此时还不是启动状态
        try {
            processorPool = DefaultProcessorPool.getProcessorPool();
        } catch (Exception e) {
            throw new LifecycleException(threadName + e);   
        }        

        processorPool.setConnector(this);
    }
    

    /**
     * XXX 连接器线程入口
     */
    @Override
    public void run() {
        log("连接器线程启动！");
        while (!stopped) {
            Socket socket = null;

            try {
                socket = serverSocket.accept();
                if (debug >= 4)
                    log("接收到请求！   " + socket);

                // 从处理池中拿到一个请求，如果处理池满了并且处理池的处理器数量达到了最大值就丢弃该请求
                Processor processor = processorPool.getProcessor();

                if (processor == null) {
                    socket.close();
                    break;
                }

                Container container = getContainer();
                if (container == null) throw new ServletException("没有容器！！！！");

                processor.setHttpConnector(this);
                processor.setContainer(container);
                processor.assign(socket);

            } catch (IOException e) {
                log(e.getMessage());
                e.printStackTrace();
            } catch (ServletException e) {
                log(e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            ;
        } finally {
            try {
                serverSocket.close();
                log("服务器socket关闭");
            } catch (IOException e) {
                log(e.getMessage());
                e.printStackTrace();
            }
        }

        log("连接器线程"+ Thread.currentThread().getName() +"关闭！");
    }

    
    /**
     * 将用完的处理器压回栈
     * @param processor
     */
    public void recycle(Processor processor) {
        processorPool.giveBackProcessor(processor);
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
        log("启动连接器！");

        // 连接器启动
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;

        // 启动处理器池
        if (processorPool instanceof Lifecycle)
            ((Lifecycle) processorPool).start();
        
        // 连接器启动
        thread.setName(threadName);
        thread.start();

        // 连接器启动后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);

        log("连接器启动完成！");
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

        if (processorPool instanceof Lifecycle)
            ((Lifecycle) processorPool).stop();
        
        // 向本机的serverSocket发送一条空请求，由于已经关闭了处理器池
        // 此请求不会被处理，但会使得serverSocket的accept()暂时接触阻塞
        // 到while的循环中会得知stooped为true，便可以跳出循环，无异常结束关闭serverSocket
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort()));
        } catch (IOException e) {
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
}
