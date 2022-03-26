package com.ranni.connector;

import com.ranni.connector.http.request.HttpRequestBase;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.HttpResponseBase;
import com.ranni.connector.http.response.Response;
import com.ranni.connector.processor.DefaultProcessorPool;
import com.ranni.connector.processor.Processor;
import com.ranni.connector.processor.ProcessorPool;
import com.ranni.connector.socket.DefaultServerSocketFactory;
import com.ranni.connector.socket.ServerSocketFactory;
import com.ranni.container.Container;
import com.ranni.container.SimpleContainer;

import java.io.IOException;
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
    protected String scheme; // 协议类型
    protected int redirectPort = 80; // 转发端口
    protected Container container; // 容器
    protected ProcessorPool processorPool; // 处理器池

    private boolean stopped = false; // 连接器停止标签

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
        return new HttpRequestBase();
    }

    /**
     * 创建响应对象
     * @return
     */
    @Override
    public Response createResponse() {
        return new HttpResponseBase();
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
            ;
        }

        return s;
    }

    /**
     * 启动前初始化
     * @throws Exception
     */
    @Override
    public void initialize() throws Exception {
        serverSocket = open();
        if (serverSocket == null) throw new IllegalStateException("创建server socket失败！");
        setScheme("http");

        // 创建处理器线程池
        processorPool = DefaultProcessorPool.getProcessorPool();

        if (processorPool == null) throw new IllegalStateException("创建processor pool失败！");
    }

    /**
     * XXX 连接器线程入口
     */
    @Override
    public void run() {
        while (!stopped) {
            Socket socket = null;

            try {
                socket = serverSocket.accept();
                // 从处理池中拿到一个请求，如果处理池满了并且处理池的处理器数量达到了最大值就丢弃该请求
                Processor processor = processorPool.getProcessor();

                if (processor == null) {
                    socket.close();
                    break;
                }

                processor.setHttpConnector(this);
                processor.setContainer(new SimpleContainer()); // XXX 这里容器也是每次都创建，后面得优化
                processor.assign(socket);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            ;
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将用完的处理器压回栈
     * @param processor
     */
    public void recycle(Processor processor) {
        processorPool.giveBackProcessor(processor);
    }

    /**
     * 启动连接器
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * 停止连接器
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {

    }
}
