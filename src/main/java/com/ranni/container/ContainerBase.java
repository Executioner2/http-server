package com.ranni.container;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.loader.Loader;
import com.ranni.container.pip.Pipeline;
import com.ranni.container.pip.StandardPipeline;
import com.ranni.container.pip.Valve;
import com.ranni.logger.Logger;
import com.ranni.naming.ProxyDirContext;
import com.ranni.container.session.Manager;
import com.ranni.util.LifecycleSupport;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 容器抽象类，实现所有容器类通用的方法
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 14:59
 */
public abstract class ContainerBase implements Container, Pipeline, Lifecycle {
    protected boolean threadDone; // 后台线程   
    protected int backgroundProcessorDelay; // 后台线程的休眠因子    
    protected Loader loader; // 加载器
    protected Logger logger; // 日志记录器
    protected Pipeline pipeline = new StandardPipeline(this); // 管道
    protected Mapper mapper; // 默认关联mapper，即mappers中只有一个mapper，那么就会将此mapper设置为默认关联mapper
    protected Map<String, Mapper> mappers = new HashMap<>(); // 协议与mapper的映射集
    protected Container parent; // 父容器
    protected String name; // 容器名字，同时也是URL中的路径
    protected Map<String, Container> children = new HashMap<>(); // 子容器
    protected boolean started; // 启动标志
    protected DirContext resources; // 容器资源
    protected Manager manager; // session管理器
    protected int debug = Logger.WARNING; // 日志级别
    protected ClassLoader parentClassLoader; // 父容器的类加载器
    protected Thread thread; // 后台任务线程
    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具实例
    protected String mapperClass; // 映射器全限定类名


    /**
     * 获取父容器，如果没有就返回自身
     * 
     * @return
     */
    @Deprecated // XXX 此方法可能存在未知问题
    @Override
    public Container getMappingObject() {
        Container container = this;
        Container parent = null;
        
        while (container != null && parent != this) {
            parent = container;
            container.getParent();            
        }
        
        return parent;
    }
    
    
    /**
     * 取得session管理器
     *
     * @return
     */
    @Override
    public Manager getManager() {
        if (this.manager != null) {
            return this.manager;
        } else if (this.parent != null) {
            return this.parent.getManager();
        }
        return null;
    }


    /**
     * 设置session管理器
     *
     * @param manager
     */
    @Override
    public synchronized void setManager(Manager manager) {
        // 容器已经启动了，并且之前有个实现了生命周期管理接口的session管理器那就先关闭之前那个
        if (started && this.manager != null
            && this.manager instanceof Lifecycle) {
            try {
                ((Lifecycle) this.manager).stop();
            } catch (Exception e) {
                log("ContainerBase.setManager  旧的管理器关闭失败！" + e);
            }
        }

        this.manager = manager;
        if (this.manager != null)
            this.manager.setContainer(this);

        if (started && this.manager instanceof Lifecycle) {
            try {
                ((Lifecycle) this.manager).start();
            } catch (Exception e) {
                log("ContainerBase.setManager  新的管理器启动失败！" + e);
            }
        }
    }


    /**
     * 返回类加载器
     * @return
     */
    @Override
    public Loader getLoader() {
        if (loader != null) {
            return loader;
        } else if (parent != null) {
            return parent.getLoader();
        }
        return null;
    }


    /**
     * 设置类加载器
     *
     * @param loader
     */
    @Override
    public synchronized void setLoader(Loader loader) {
        Loader oldLoader = this.loader;
        if (oldLoader == loader)
            return;

        this.loader = loader;

        // 如果容器已经启动了
        // 那么需要先停止旧的加载器（继承了Lifecycle的话），再启动新的加载器（继承了Lifecycle的话）
        if (started && oldLoader != null
            && oldLoader instanceof Lifecycle) {
            try {
                ((Lifecycle) oldLoader).stop();
            } catch (Exception exception) {
                log("ContainerBase.setLoader：旧的加载器停止失败！");
            }
        }

        if (loader != null)
            loader.setContainer(this);

        if (started && loader != null
            && loader instanceof Lifecycle) {
            try {
                ((Lifecycle) loader).start();
            } catch (Exception exception) {
                log("ContainerBase.setLoader：新的的加载器启动失败！");
            }
        }
    }


    /**
     * 返回此容器的名字
     *
     * @return
     */
    @Override
    public String getName() {
        return this.name;
    }


    /**
     * 设置此容器的名字
     *
     * @param name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }


    /**
     * 返回父容器
     *
     * @return
     */
    @Override
    public Container getParent() {
        return this.parent;
    }


    /**
     * 设置父容器
     *
     * @param container
     */
    @Override
    public void setParent(Container container) {
        this.parent = container;
    }


    /**
     * 返回父容器的类加载器
     *
     * @return
     */
    @Override
    public ClassLoader getParentClassLoader() {
       if (parentClassLoader != null)
           return parentClassLoader;
       else if (parent != null)
           return parent.getParentClassLoader();
       else
           return ClassLoader.getSystemClassLoader(); // 返回系统类（应用程序类）加载器
    }


    /**
     * 设置父容器的类加载器
     *
     * @param parent
     */
    @Override
    public void setParentClassLoader(ClassLoader parent) {
        this.parentClassLoader = parent;
    }


    /**
     * 返回目录容器，如果没有就返回父Container的，都没有就返回null
     *
     * @return
     */
    @Override
    public DirContext getResources() {
        if (resources != null)
            return resources;
        else if (parent != null)
            return parent.getResources();
        return null;
    }


    /**
     * 设置目录容器
     *
     * @param resources
     */
    @Override
    public synchronized void setResources(DirContext resources) {
        if (this.resources == resources) return;

        Hashtable<String, String> env = new Hashtable<>();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());
        this.resources = new ProxyDirContext(env, resources); // 变成代理目录容器（增加缓存功能）
    }


    /**
     * 添加子容器
     * @param child
     */
    @Override
    public void addChild(Container child) {
        child.setParent(this);
        synchronized (children) {
            log("ContainerBase.addChild  有容器加入！  " + child + "   " + child.getName());
            
            if (started && child instanceof Lifecycle) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    e.printStackTrace();
                }
            }
            
            children.put(child.getName(), child);
        }
    }


    @Override
    public void addContainerListener(ContainerListener listener) {

    }


    /**
     * 设置日志监听器
     *
     * @return
     */
    @Override
    public Logger getLogger() {
        if (logger != null)
            return logger;
        else if (parent != null)
            return parent.getLogger();
        return null;
    }

    /**
     * 设置日志记录器
     *
     * @param logger
     */
    @Override
    public synchronized void setLogger(Logger logger) {
        Logger oldLogger = this.logger; // 旧的日志记录器
        this.logger = logger;

        if (started && oldLogger != null
            && oldLogger instanceof Lifecycle) {
            try {
                ((Lifecycle) oldLogger).stop();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        if (logger != null) {
            logger.setContainer(this);
        }

        // 容器已经启动了，但是又变更了日志记录器且日志记录器实现了生命周期管理接口
        // 那么就要调用该日志记录中的start()
        if (started && logger != null
            && logger instanceof Lifecycle) {
            try {
                ((Lifecycle) logger).start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * 添加mapper到mappers中
     *
     * @param mapper
     *
     * @exception IllegalArgumentException mappers中有这种协议的mapper了就抛出异常
     */
    @Override
    public void addMapper(Mapper mapper) {
        synchronized (mappers) {
            if (mappers.get(mapper.getProtocol()) != null) {
                throw new IllegalArgumentException("addMapper：Protocol：" + mapper.getProtocol() + " 不是唯一的");
            }
            mapper.setContainer(this);

            // TODO 设置生命周期

            mappers.put(mapper.getProtocol(), mapper);

            if (mappers.size() == 1) {
                // 只有一个mapper的时候就直接设为默认关联
                this.mapper = mapper;
            } else {
                // 不止一个就将默认关联置为空
                this.mapper = null;
            }

            // TODO 添加mapper事件
        }
    }


    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }


    /**
     * 根据子容器名找到对应的子容器
     *
     * @param name
     * @return
     */
    @Override
    public Container findChild(String name) {
        synchronized (children) {
            return children.get(name);
        }
    }


    /**
     * 返回所有的子容器
     *
     * @return
     */
    @Override
    public Container[] findChildren() {
        synchronized (children) {
            return children.values().toArray(new Container[children.values().size()]);
        }
    }


    @Override
    public ContainerListener[] findContainerListeners() {
        return new ContainerListener[0];
    }


    /**
     * 根据协议返回对应的mapper
     * 如果当前context有关联的mapper就返回
     * 否则就返回mappers中对应协议的mapper（如果有的话）
     *
     * @param protocol
     *
     * @return
     */
    @Override
    public Mapper findMapper(String protocol) {
        if (mapper != null) return mapper;

        synchronized (mappers) {
            return mappers.get(protocol);
        }
    }


    /**
     * 返回mappers中所有的mapper
     *
     * @return
     */
    @Override
    public Mapper[] findMappers() {
        synchronized (mappers) {
            return mappers.values().toArray(new Mapper[mappers.size()]);
        }
    }


    /**
     * 进入管道依次执行阀
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        pipeline.invoke(request, response);
    }


    /**
     * 移除阀
     *
     * @param valve
     */
    @Override
    public void removeValve(Valve valve) {
        pipeline.removeValve(valve);
    }


    /**
     * 取得这个请求的处理容器
     *
     * @param request
     * @param update
     * @return
     */
    @Override
    public Container map(Request request, boolean update) {
        // 根据请求协议查询mapper
        Mapper mapper = findMapper(request.getRequest().getProtocol());

        if (mapper == null) return null;

        return mapper.map(request, update);
    }


    /**
     * 移除子容器
     *
     * @param child
     */
    @Override
    public void removeChild(Container child) {
        synchronized (children) {
            log("ContainerBase.removeChild  有容器被删除！  " + child);
            
            if (started && child instanceof Lifecycle) {
                try {
                    ((Lifecycle) child).stop();
                } catch (LifecycleException e) {
                    e.printStackTrace();
                }
            }
            
            children.remove(child.getName());
        }
    }


    @Override
    public void removeContainerListener(ContainerListener listener) {

    }


    /**
     * 从mappers中移除mapper
     *
     * @param mapper
     */
    @Override
    public void removeMapper(Mapper mapper) {
        synchronized (mappers) {
            if (mappers.get(mapper.getProtocol()) != null) {
                mappers.remove(mapper.getProtocol());

                // TODO 设置生命周期

                if (mappers.size() != 1) {
                    this.mapper = null;
                } else {
                    this.mapper = mappers.values().iterator().next();
                }

                // TODO 从mappers中移除mapper
            }
        }
    }


    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }


    /**
     * 返回基础阀
     *
     * @return
     */
    @Override
    public Valve getBasic() {
        return pipeline.getBasic();
    }


    /**
     * 设置基础阀
     *
     * @param valve
     */
    @Override
    public void setBasic(Valve valve) {
        pipeline.setBasic(valve);
    }


    /**
     * 添加阀
     *
     * @param valve
     */
    @Override
    public void addValve(Valve valve) {
        pipeline.addValve(valve);
    }


    /**
     * 返回所有非基础阀
     *
     * @return
     */
    @Override
    public Valve[] getValves() {
        return pipeline.getValves();
    }

    
    /**
     * 写入日志文件
     *
     * @param message
     */
    public void log(String message) {
        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message);
        else
            System.out.println(logName() + ": " + message);
    }


    /**
     * 写入日志文件
     *
     * @param message
     */
    public void log(String message, Throwable throwable) {
        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            System.out.println(logName() + ": " + message + ": " + throwable);
            throwable.printStackTrace(System.out);
        }
    }
    

    /**
     * 取得简短类名
     *
     * @return
     */
    protected String logName() {
        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");
    }
    

    /**
     * 添加默认的映射器
     * mappers中第一位就是默认mapper
     *
     * @param mapperClass
     */
    protected void addDefaultMapper(String mapperClass) {
        if (mapperClass == null || mapperClass.isBlank())
            return;
        if (mappers.size() >= 1)
            return;

        try {
            Mapper defaultMapper = null;
            Class clazz = Class.forName(mapperClass);
            Object o = clazz.getConstructor().newInstance();
            if (!(o instanceof Mapper))
                throw new ClassCastException("该类不是Mapper的实现类！");

            defaultMapper = (Mapper) o;
            defaultMapper.setProtocol("http");
            addMapper(defaultMapper);
        } catch (Exception e) {
            log("containerBase.addDefaultMapper  " + e.getMessage());
        }
    }
    

    /**
     * 返回日志输出级别
     *
     * @return
     */
    public int getDebug() {
        return this.debug;
    }


    /**
     * 后台任务间隔因子
     * 小于等于0表示无后台任务线程
     * 
     * @return
     */
    @Override
    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    
    /**
     * 后台任务间隔因子
     * 值小于等于0表示没有单独的后台任务线程
     * 
     * @param backgroundProcessorDelay
     */
    @Override
    public void setBackgroundProcessorDelay(int backgroundProcessorDelay) {
        this.backgroundProcessorDelay = backgroundProcessorDelay;
    }


    /**
     * 设置后台任务线程
     * @param thread
     */
    @Override
    public void setThread(Thread thread) {
        this.thread = thread;
    }
    

    /**
     * 启动后台任务线程
     */
    @Override
    public void threadStart() {        
        if (getBackgroundProcessorDelay() <= 0)
            return;
        
        if (thread == null)
            thread = new Thread(new ContainerBackgroundProcessor());
        
        threadDone = false;
        String threadName = "ContainerBackgroundProcessor["+ toString() +"]";
        thread.setName(threadName);
        thread.setDaemon(true);
        thread.start();
        log("后台线程启动成功！");
    }


    /**
     * 关闭后台任务线程
     */
    @Override
    public synchronized void threadStop() {
        threadDone = true;
    }


    /**
     * 添加生命周期监听
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 返回所有生命周期监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()} 该方法是线程安全的方法
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除指定的生命周期监听实例
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
     * 通用容器启动方法
     *
     * @throws LifecycleException
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (started)
            throw new LifecycleException("ContainerBase.start  容器已经启动！");

        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        
        addDefaultMapper(this.mapperClass);
        started = true;
        
        // 启动组件
        if (loader != null && loader instanceof Lifecycle)
            ((Lifecycle) loader).start();
        if (logger != null && logger instanceof Lifecycle)
            ((Lifecycle) logger).start();
        if (manager != null && manager instanceof Lifecycle)
            ((Lifecycle) manager).start();
        if (resources != null && resources instanceof Lifecycle)
            ((Lifecycle) resources).start();
        
        // 启动所有的映射器
        for (Mapper mapper : findMappers()) {
            if (mapper instanceof Lifecycle)
                ((Lifecycle) mapper).start();
        }
        
        // 启动子容器
        for (Container container : findChildren()) {
            if (container instanceof Lifecycle)
                ((Lifecycle) container).start();
        }
        
        // 启动管道
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();
        
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
        
    }


    /**
     * 通用容器停止方法
     *
     * @throws LifecycleException
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started)
            throw new LifecycleException("ContainerBase.stop  容器已经停止！");
        
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        
        // 关闭管道
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).stop();
        
        // 关闭所有子容器
        for (Container container : findChildren()) {
            if (container instanceof Lifecycle) 
                ((Lifecycle) container).stop();
        }
        
        // 关闭所有映射器
        for (Mapper mapper : findMappers()) {
            if (mapper instanceof Lifecycle) 
                ((Lifecycle) mapper).stop();
        }
        
        // 关闭组件
        if (resources != null && resources instanceof Lifecycle)
            ((Lifecycle) resources).stop();
        if (manager != null && manager instanceof Lifecycle) 
            ((Lifecycle) manager).stop();
        if (logger != null && logger instanceof Lifecycle)
            ((Lifecycle) logger).stop();
        if (loader != null && loader instanceof Lifecycle)
            ((Lifecycle) loader).stop();
        
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }


    /**
     * 容器的后台任务线程
     */
    protected class ContainerBackgroundProcessor implements Runnable {

        @Override
        public void run() {
            while (!threadDone) {
                try {
                    Thread.sleep(backgroundProcessorDelay * 1000L);
                } catch (InterruptedException e) {
                    ;
                }
                
                if (!threadDone) {
                    Container parent = getMappingObject(); // XXX 此方法可能存在未知问题
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (parent.getLoader() != null)
                        cl = parent.getLoader().getClassLoader();
                    processChild(parent, cl);
                }
            }
        }

        
        /**
         * 处理子容器的后台任务
         * 
         * @param container
         * @param cl
         */
        protected void processChild(Container container, ClassLoader cl) {
            // 执行父容器的后台任务
            try {
                if (container.getLoader() != null) {
                    Thread.currentThread().setContextClassLoader(container.getLoader().getClassLoader());
                }
                container.backgroundProcessor();
            } catch (Throwable t) {
                log("后台任务执行异常！" + t);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
            
            // 执行父容器的子容器的后台任务
            for (Container item : container.findChildren()) {
                if (item.getBackgroundProcessorDelay() <= 0)
                    item.backgroundProcessor();
            }
        }
        
        
    } 
    
    
}
