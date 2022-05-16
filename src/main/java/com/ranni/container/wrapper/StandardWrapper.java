package com.ranni.container.wrapper;

import com.ranni.connector.Constants;
import com.ranni.connector.http.request.HttpRequestBase;
import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.HttpResponseBase;
import com.ranni.connector.http.response.Response;
import com.ranni.container.*;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.loader.Loader;
import com.ranni.logger.Logger;
import com.ranni.monitor.InstanceEvent;
import com.ranni.monitor.InstanceListener;
import com.ranni.util.Enumerator;
import com.ranni.util.InstanceSupport;

import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 标准的wrapper接口实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:44
 */
public class StandardWrapper extends ContainerBase implements ServletConfig, Wrapper {
    private Servlet instance; // servlet 实例
    private StandardWrapperFacade facade = new StandardWrapperFacade(this);
    private Map<String, String> parameters = new HashMap(); // 参数列表
    private boolean singleThreadModel; // 是否是单线程servlet模式，如果不是，每次请求都会创建新的servlet实例
    private Deque<Servlet> instancePool = null; // 单线程servlet模式下才会有servlet实例池
    private int maxInstances = 20; // 默认的单线程servlet模式下最大实例数量
    private int countAllocated; // 活动的实例数量
    private int debug = Logger.INFORMATION; // 日志输出级别
    private String jspFile; // jsp文件名
    private int loadOnStartup = -1; // 此servlet的service()调用时机，大于0时，表示调用了init()后立即调用service()
    private int nInstances; // STM servlet的数量（STM SingleThreadModel）
    private boolean unloading; // 是否正在卸载中
    private boolean unavailable; // 此wrapper是否不可用
    private long available; // 此wrapper什么时候可用，为0L则表示永久可用，为Long.MAX_VALUE则表示永久不可用)

    protected String servletClass; // servlet类全限定类名
    protected InstanceSupport instanceSupport = new InstanceSupport(this); // 实例监听器工具实例


    public StandardWrapper() {
        pipeline.setBasic(new StandardWrapperValve(this));
    }


    /**
     * 返回实例监听器
     * 
     * @return
     */
    public InstanceSupport getInstanceSupport() {
        return this.instanceSupport;
    }
    

    /**
     * 返回此wrapper可用的日期时间
     *
     * @return
     */
    @Override
    public long getAvailable() {
        return this.available;
    }


    /**
     * 设置此wrapper可用的日期时间
     *
     * @param available
     */
    @Override
    public void setAvailable(long available) {
        if (available < System.currentTimeMillis()) {
            this.available = 0L;
        } else {
            this.available = available;
        }
    }


    /**
     * 取得JSP文件名
     *
     * @return
     */
    @Override
    public String getJspFile() {
        return this.jspFile;
    }


    /**
     * 设置JSP文件名
     *
     * @param jspFile
     */
    @Override
    public void setJspFile(String jspFile) {
        this.jspFile = jspFile;
    }


    /**
     * 返回启动时加载顺序，为负数则表示第一次就加载
     *
     * @return
     */
    @Override
    public int getLoadOnStartup() {
        return this.loadOnStartup;
    }


    /**
     * 设置启动时加载顺序
     *
     * @param value
     */
    @Override
    public void setLoadOnStartup(int value) {
        this.loadOnStartup = value;
    }


    @Override
    public String getRunAs() {
        return null;
    }


    @Override
    public void setRunAs(String runAs) {

    }


    /**
     * 是否是单线程servlet模式
     *
     * @return
     */
    public boolean isSingleThreadModel() {
        return singleThreadModel;
    }


    /**
     * 返回servlet类名
     * @return
     */
    @Override
    public String getServletClass() {
        return this.servletClass;
    }

    /**
     * 设置servlet类名
     * @param servletClass
     */
    @Override
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }


    /**
     * 此wrapper是否不可用
     *
     * @return
     */
    @Override
    public boolean isUnavailable() {
        if (available == 0L) {
            return false;
        } else if (available <= System.currentTimeMillis()) {
            available = 0L;
            return false;
        } else {
            return true;
        }
    }


    /**
     * 添加初始化参数
     *
     * @param name
     * @param value
     */
    @Override
    public void addInitParameter(String name, String value) {
        synchronized (parameters) {
            parameters.put(name, value);
        }
    }


    /**
     * 添加实例监听器
     * 
     * @see {@link InstanceSupport#addInstanceListener(InstanceListener)} 调用此方法，线程安全
     * 
     * @param listener
     */
    @Override
    public void addInstanceListener(InstanceListener listener) {
        instanceSupport.addInstanceListener(listener);
    }


    /**
     * 返回一个servlet
     * 先判断是否是单线程servlet模式
     * 如果不是：
     *      每次请求都会创建新的servlet实例
     * 如果是：
     *      先尝试从实例池中取得空闲的servlet实例。如果没有空闲的实例那么
     *      判断是否可以创建新的servlet实例，如果可以则创建新的实例并返回。
     *      如果不能创建，则会进入等待状态，等待有空闲的servlet实例
     *
     * TODO 在合适的位置加入监听事件
     *
     * @return
     * @throws ServletException
     */
    @Override
    public Servlet allocate() throws ServletException {
        if (unloading) {
            throw new ServletException("StandardWrapper.allocate  正在卸载中，不能请求分配！");
        }

        if (!singleThreadModel) {
            // 非单线程servlet模式
            if (instance == null) {
                synchronized (this) {
                    instance = loadServlet();
                    // 插入实例获取事件，这里很重要，有个ControllerConfig将会监听此事件，然后对Controller实例进行扫描配置
                    instanceSupport.fireInstanceEvent(InstanceEvent.INSTANCE_EVENT, instance);
                }
            }

            // 第一次的加载有可能加载的使STM servlet
            // 所以要进行此判断
            if (!singleThreadModel) {
                countAllocated++;
                return instance;
            }
        }

        // 到这儿就是单线程servlet模式
        synchronized (instancePool) {
            while (countAllocated >= nInstances) {
                // 如果STM servlet的数量小于正在处理请求的数量，并且创建的STM实例数量小于maxInstances
                // 那么就往STM池中放新的STM实例，否则进入等待状态
                if (nInstances < maxInstances) {
                    instancePool.push(loadServlet());
                    nInstances++;
                } else {
                    try {
                        instancePool.wait();
                    } catch (InterruptedException e) {
                        ;
                    }
                }
            }

            countAllocated++;
            return instancePool.pop();
        }
    }


    /**
     * 加载一个servlet实例
     * TODO 增加日志记录
     *
     * @return
     */
    private synchronized Servlet loadServlet() throws ServletException {
        if (!singleThreadModel && instance != null)
            return instance;

        Servlet servlet = null;

        String actualClass = servletClass;

        if (actualClass == null && jspFile != null) {
            // 实际的类是个jsp文件
            Wrapper jspWrapper = (Wrapper) getParent().findChild(Constants.JSP_SERVLET_NAME);
            if (jspWrapper != null)
                actualClass = jspWrapper.getServletClass();
        }

        // 还是为null，设置不可用
        if (actualClass == null) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  servletClass为null！");
        }

        // 取得加载器
        Loader loader = getLoader();
        if (loader == null) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  loader为null！");
        }

        // 取得类加载器
        ClassLoader classLoader = loader.getClassLoader();

        if (classLoader == null && isContainerProvidedServlet(actualClass)) {
            classLoader = this.getClass().getClassLoader();
        }

        // 取得加载类
        Class<?> clazz = null;
        try {
            if (classLoader != null) {
                clazz = classLoader.loadClass(actualClass);
            } else {
                clazz = Class.forName(actualClass);
            }
        } catch (ClassNotFoundException e) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  未找到此class文件：" + actualClass);
        }

        if (clazz == null) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  要载入的类为null：" + actualClass);
        }

        // 取得构造方法
        Constructor<?> constructor = null;
        try {
             constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  取得构造方法失败！" + actualClass);
        }

        if (constructor == null) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  构造方法为null：" + actualClass);
        }

        // 实例化servlet类
        try {
            servlet = (Servlet) constructor.newInstance();
        } catch (Exception e) {
            // TODO 设置不可用
            throw new ServletException("StandardWrapper.loadServlet  实例化servlet失败：" + actualClass);
        }

        // 容器类型servlet要特殊处理
        if (servlet instanceof ContainerServlet
            && isContainerProvidedServlet(actualClass)) {

            ((ContainerServlet) servlet).setWrapper(this);
        }

        // 调用servlet的init()
        try {
            servlet.init(facade);
            if (loadOnStartup > 0 && jspFile != null) {
                HttpRequestBase hreq = new HttpRequestBase();
                HttpResponseBase hres = new HttpResponseBase();
                hreq.setServletPath(jspFile);
                hreq.setQueryString("jsp_precompile=true");
                servlet.service(hreq, hres);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 是否是单线程servlet模式
        singleThreadModel = servlet instanceof SingleThreadModel;
        if (singleThreadModel) {
            if (instancePool == null) {
                // 创建一个实例池
                instancePool = new LinkedList<>();
            }
        }

        return servlet;
    }


    /**
     * 是否是Ranni内部的servlet
     *
     * @param actualClass
     * @return
     */
    private boolean isContainerProvidedServlet(String actualClass) {
        if ("com.ranni.".startsWith(actualClass)) {
            return true;
        }

        try {
            Class<?> clazz = this.getClass().getClassLoader().loadClass(actualClass);

            // 是否是ContainerBase的子类
            return ContainerBase.class.isAssignableFrom(clazz);
        } catch (Throwable e) {
            return false;
        }
    }


    /**
     * 解除servlet分配
     *
     * @param servlet
     * @throws ServletException
     */
    @Override
    public void deallocate(Servlet servlet) throws ServletException {
        if (!singleThreadModel) {
            countAllocated--;
            return;
        }

        synchronized (instancePool) {
            countAllocated--;
            instancePool.push(servlet);
            instancePool.notifyAll();
        }
    }


    /**
     * 查询初始化参数
     *
     * @param name
     * @return
     */
    @Override
    public String findInitParameter(String name) {
        synchronized (parameters) {
            return parameters.get(name);
        }
    }


    /**
     * 返回初始化参数名
     *
     * @return
     */
    @Override
    public String[] findInitParameters() {
        synchronized (parameters) {
            return parameters.keySet().toArray(new String[parameters.size()]);
        }
    }


    /**
     * 同allocate()，只是不返回值
     *
     * @throws ServletException
     */
    @Override
    public synchronized void load() throws ServletException {
        this.instance = allocate();
    }


    /**
     * 移除实例监听器
     * 
     * @see {@link InstanceSupport#removeInstanceListener(InstanceListener)} 调用此方法，线程安全
     * 
     * @param listener
     */
    @Override
    public void removeInstanceListener(InstanceListener listener) {
        instanceSupport.removeInstanceListener(listener);
    }


    /**
     * TODO 接收不可用异常并做输出处理
     *
     * @param unavailableException
     */
    @Override
    public void unavailable(UnavailableException unavailableException) {
        this.unavailable = true;
    }


    /**
     * 卸载加载的属性
     * TODO 在合适的位置加入监听事件
     *
     * @throws ServletException
     */
    @Override
    public synchronized void unload() throws ServletException {
        if (!singleThreadModel && instance == null)
            return;

        unloading = true;
        if (countAllocated > 0) {
            // 还有正在处理请求的servlet
            // 循环等待（50 * 10）ms
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }


        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        // 注意，因为singleThreadModel只有在loadServlet()中才有可能被修改为true。
        // 而singleThreadModel默认为false，第一次成功的加载必定使instance引用到一个servlet对象
        ClassLoader classLoader = instance.getClass().getClassLoader();

        // 销毁instance实例
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            instance.destroy();
        } catch (Throwable t) {
            instance = null;
            instancePool = null;
            nInstances = 0;
            unloading = false;
        } finally {
            // 恢复上下文类加载器
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        instance = null;

        if (singleThreadModel && instancePool != null) {
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                while (!instancePool.isEmpty()) {
                    instancePool.pop().destroy();
                }
            } catch (Throwable t) {
                // TODO 此处加入事件
                throw new ServletException("StandardWrapper.unload  卸载实例池失败！" + t);
            } finally {
                unloading = false;
                instancePool = null;
                nInstances = 0;
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }


    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public void backgroundProcessor() {
        
    }


    /**
     * 进入管道依次执行阀
     *
     * @param request
     * @param response
     *
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        pipeline.invoke(request, response);
    }


    /**
     * Wrapper本就是最小容器
     * 若Wrapper对象执行该方法将抛出异常
     *
     * @param child
     *
     * @exception IllegalStateException Wrapper对象执行该方法抛出异常
     */
    @Override
    public void addChild(Container child) {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @param name
     *
     * @return
     *
     * @exception  IllegalStateException Wrapper对象执行该方法抛出异常
     */
    @Override
    public Container findChild(String name) {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @return
     */
    @Override
    public Container[] findChildren() {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * Wrapper对象调用此方法将抛出异常
     *
     * @param child
     */
    @Override
    public void removeChild(Container child) {
        throw new IllegalStateException ("标准wrapper没有child");
    }


    /**
     * wrapper容器启动
     * 启动顺序：
     *  1、加载器
     *  2、管道
     *  3、容器自身
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (started) throw new LifecycleException("此wrapper容器实例已经启动！");
        System.out.println("启动wrapper容器：" + this); // TODO sout

        // 此wrapper容器启动之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        started = true;

        // 启动加载器
        if (loader != null && loader instanceof Lifecycle)
            ((Lifecycle) loader).start();

        // 启动管道
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();

        // 启动此wrapper容器自身
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);

        // 此wrapper容器启动之后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }


    /**
     * 关闭当前容器
     * 关闭顺序
     *  1、容器本身
     *  2、管道
     *  3、加载器
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("此wrapper容器实例已经停止！");
        
        if (debug >= 3)
            log("StandardWrapper.stoppingWrapper  " + this);
        
        try {
            // 执行servlet的destroy()
            instance.destroy();
        } catch (Throwable t) {
            ;
        }

        instance = null;

        // 关闭此wrapper容器之前
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);

        // 关闭此wrapper容器
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;

        // 关闭管道
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // 关闭加载器
        if (loader != null && loader instanceof Lifecycle) {
            ((Lifecycle) loader).stop();
        }

        // 关闭此wrapper容器之后
        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);

    }

    /**
     * 返回servlet的名字
     * @return
     */
    @Override
    public String getServletName() {
        return getName();
    }


    /**
     * 返回servlet全局作用域对象
     *
     * @return
     */
    @Override
    public ServletContext getServletContext() {
        if (parent == null)
            return null;
        else if (parent instanceof Context)
            return ((Context) parent).getServletContext();
        else
            return null;
    }


    /**
     * 取得实例化参数
     *
     * @param s
     * @return
     */
    @Override
    public String getInitParameter(String s) {
        return findInitParameter(s);
    }


    /**
     * 返回实例化参数名的迭代器
     *
     * @return
     */
    @Override
    public Enumeration getInitParameterNames() {
        synchronized (parameters) {
            return new Enumerator(parameters.keySet());
        }
    }
}
