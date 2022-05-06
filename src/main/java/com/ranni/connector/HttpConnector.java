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
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.util.LifecycleSupport;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
    private boolean stopped = false; // 连接器线程停止标签
    private String threadName; // 线程名
    private int debug = Logger.INFORMATION; // 日志输出级别

    protected boolean started; // 连接器启动标志
    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具类实例
    protected String scheme; // 协议类型
    protected int redirectPort = 80; // 转发端口
    protected Container container; // 容器
    protected ProcessorPool processorPool; // 处理器池


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
     * 返回容器
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此连接关联容器
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * TODO 返回dns查询标志
     * @return
     */
    @Override
    public boolean getEnableLookups() {
        return false;
    }

    /**
     * TODO 设置dns查询标志
     * @param enableLookups
     */
    @Override
    public void setEnableLookups(boolean enableLookups) {

    }

    /**
     * 取得ServerSocketFactory对象
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
     * @param factory
     */
    @Override
    public void setFactory(ServerSocketFactory factory) {
        this.factory = factory;
    }

    /**
     * TODO 返回此实现类的信息和版本号
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }

    /**
     * TODO 返回转发端口
     * @return
     */
    @Override
    public int getRedirectPort() {
        return 0;
    }

    /**
     * TODO 设置转发端口
     * @param redirectPort
     */
    @Override
    public void setRedirectPort(int redirectPort) {

    }

    /**
     * 返回协议类型
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
     * TODO 返回安全标志
     * @return
     */
    @Override
    public boolean getSecure() {
        return false;
    }

    /**
     * TODO 设置安全标志
     * @param secure
     */
    @Override
    public void setSecure(boolean secure) {

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
     * @return
     */
    private ServerSocket open() {
        ServerSocketFactory f = getFactory();

        if (f == null) throw new IllegalStateException("获取server socket失败!");

        ServerSocket s = null;

        try {
            s = f.createSocket(8080);
        } catch (IOException e) {
            log("server socket创建失败，请检查端口是否被占用！" + e.getMessage());
        }

        return s;
    }

    /**
     * 启动前初始化
     *
     * @throws Exception
     */
    @Override
    public void initialize() throws Exception {
        log("连接器初始化");
        serverSocket = open();
        if (serverSocket == null)
            throw new IllegalStateException("创建server socket失败！");
        setScheme("http");

        // 创建处理器线程池，此时还不是启动状态
        processorPool = DefaultProcessorPool.getProcessorPool();

        if (processorPool == null) throw new IllegalStateException("创建processor pool失败！");

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

        // 连接器启动前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);

        started = true;

        if (container instanceof Lifecycle)
            ((Lifecycle) container).start();

        if (processorPool instanceof Lifecycle)
            ((Lifecycle) processorPool).start();

        Thread thread = new Thread(this);
        // 连接器启动
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        thread.setName("HttpConnector@"+this.hashCode());
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

        if (container instanceof Lifecycle)
            ((Lifecycle) container).stop();

        // 向本机的serverSocket发送一条空请求，由于已经关闭了处理器池
        // 此请求不会被处理，但会使得serverSocket的accept()暂时接触阻塞
        // 到while的循环中会得知stooped为true，便可以跳出循环，无异常结束关闭serverSocket
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(DefaultServerSocketFactory.ipaddress, DefaultServerSocketFactory.port));
        } catch (IOException e) {
            log("HttpConnector.stoppingSocket", e);
        }

        // 连接器停止后
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
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

}
