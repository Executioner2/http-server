package com.ranni.core;

import com.ranni.connector.Connector;
import com.ranni.connector.Mapper;
import com.ranni.container.Container;
import com.ranni.container.Engine;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import java.util.concurrent.*;

/**
 * Title: HttpServer
 * Description:
 * 标准的服务实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 10:53
 */
public final class StandardService implements Lifecycle, Service {

    // ==================================== 属性字段 ====================================

    /**
     * 关联的容器
     */
    private Engine container;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 关联的服务器
     */
    private Server server;

    /**
     * 连接器集合
     */
    private Connector[] connectors = new Connector[0];

    /**
     * 生命周期管理工具实例
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * 服务启动标志位
     */
    private boolean started;

    /**
     * 服务初始化标志位
     */
    private boolean initialize;

    /**
     * 映射实例
     */
    private Mapper mapper; 

    /**
     * 线程池
     */
    private Executor executor;

    /**
     * 线程池初始线程数
     */
    private int minSpareThreads = 10;

    /**
     * 线程池最大线程数
     */
    private int maxThreads = 200;

    // ==================================== 核心方法 ====================================

    /**
     * 返回关联的容器 
     * 
     * @return
     */
    @Override
    public Engine getContainer() {
        return this.container;
    }


    /**
     * 设置关联的容器
     * 
     * @param engine
     */
    @Override
    public void setContainer(Engine engine) {
        Container oldContainer = this.container;
        
        // 设服务为null，不再接收请求
        if (oldContainer != null && oldContainer instanceof Engine)
            ((Engine) oldContainer).setService(null);
        
        // 新的容器关联此服务实例并启动（如果服务已经启动了的话）
        this.container = engine;
        if (this.container != null && this.container instanceof Engine)
            this.container.setService(this);
        if (started && this.container != null
            && this.container instanceof Lifecycle) {
            try {
                ((Lifecycle) this.container).start();
            } catch (LifecycleException e) {
                ;
            }
        }
        
        // 此服务下所有的连接器重新关联容器
        synchronized (connectors) {
            for (Connector connector : connectors)
                connector.setContainer(this.container);
        }
        
        // 停止旧的容器（如果服务已经启动了的话）
        if (started && oldContainer != null 
            && oldContainer instanceof Lifecycle) {
            try {
                ((Lifecycle) oldContainer).stop();
            } catch (LifecycleException e) {
                ;
            }
        }
    }


    /**
     * 返回实现类信息
     * 
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 返回服务名
     * 
     * @return
     */
    @Override
    public String getName() {
        return this.name;
    }


    /**
     * 设置服务名
     * 
     * @param name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }


    /**
     * 返回所属的服务器
     * 
     * @return
     */
    @Override
    public Server getServer() {
        return this.server;
    }


    /**
     * 设置所属的服务器
     * 
     * @param server
     */
    @Override
    public void setServer(Server server) {
        this.server = server;
    }


    /**
     * @return 返回映射实例
     */
    @Override
    public Mapper getMapper() {
        return mapper;
    }


    /**
     * 设置映射实例
     * 
     * @param mapper 映射实例
     */
    @Override
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }
    

    /**
     * 添加连接器
     * 如果服务已经初始化，该连接器也要初始化
     * 如果服务已经启动，该连接器如果实现了生命周期接口就也要启动
     * 
     * @param connector
     */
    @Override
    public void addConnector(Connector connector) {
        
        synchronized (connectors) {
            // 设置连接器关联信息
            connector.setContainer(this.container);
            connector.setService(this);
            
            // 加入到集合
            Connector[] newArs = new Connector[this.connectors.length + 1];
            System.arraycopy(connectors, 0, newArs, 0, connectors.length);
            newArs[connectors.length] = connector;
            connectors = newArs;
            
            // 服务已经初始化，那么此连接器也应该初始化
            if (initialize) {
                try {
                    connector.initialize();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
            
            // 如果服务已经启动，那么此连接器也应该启动
            if (started && connector instanceof Lifecycle) {
                try {
                    ((Lifecycle) connector).start();
                } catch (LifecycleException e) {
                    ;
                }
            }
            
        }
    }


    /**
     * 返回所有连接器
     * 
     * @return
     */
    @Override
    public Connector[] findConnectors() {
        
        synchronized (connectors) {
            return this.connectors;
        }
    }


    /**
     * 移除连接器
     * 如果服务已经启动，那么该连接器需要关闭
     * 
     * @param connector
     */
    @Override
    public void removeConnector(Connector connector) {
        
        synchronized (connectors) {
            int i = 0;
            for (; i < connectors.length; i++) {
                if (connectors[i] == connector)
                    break;
            }
            
            if (i == connectors.length)
                return;

            Connector[] newArs = new Connector[this.connectors.length - 1];
            for (int j = 0, k = 0; j < this.connectors.length; j++) {
                if (j != i)
                    newArs[k++] = connectors[j];
            }
            
            connectors = newArs;
            
            // 如果服务已经启动，则需要关闭连接器
            if (started && connector instanceof Lifecycle) {
                try {
                    ((Lifecycle) connector).stop();
                } catch (LifecycleException e) {
                    ;
                }
            }
        }
        
    }


    // ==================================== 生命周期相关 ====================================

    /**
     * 初始化所有连接器
     * 
     * @throws LifecycleException
     */
    @Override
    public void initialize() throws LifecycleException {
        if (initialize)
            throw new LifecycleException("StandardService.initialize  服务已初始化！");
        
        initialize = true;
        
        for (Connector connector : connectors) {
            connector.initialize();
        }
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
     * 服务启动
     * 
     * @throws LifecycleException
     */
    @Override
    public void start() throws LifecycleException {
        if (started)
            throw new LifecycleException("StandardService.start  服务已启动！");
        
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
        
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
        
        // 启动容器
        if (container != null && container instanceof Lifecycle) {
            synchronized (container) {
                ((Lifecycle) container).start();
            }
        }
        
        // 启动所有连接器
        synchronized (connectors) {
            for (Connector connector : connectors) {
                if (connector instanceof Lifecycle)
                    ((Lifecycle) connector).start();
            }
        }
        
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * 服务停止
     * 
     * @throws LifecycleException
     */
    @Override
    public void stop() throws LifecycleException {
        if (!started)
            throw new LifecycleException("StandardService.stop  服务未启动！");
        
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        
        started = false;
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        
        // 停止所有连接器
        synchronized (connectors) {
            for (Connector connector : connectors) {
                if (connector instanceof Lifecycle)
                    ((Lifecycle) connector).stop();
            }
        }
        
        // 停止容器
        if (container != null && container instanceof Lifecycle) {
            synchronized (container) {
                ((Lifecycle) container).stop();
            }
        }
        
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }


    // ==================================== 多线程 ====================================


    /**
     * @return 返回线程池初始化线程数
     */
    @Override
    public int getMinSpareThreads() {
        return minSpareThreads;
    }


    /**
     * 设置线程池初始化线程数
     * 
     * @param minSpareThreads 线程池初始化线程数
     */
    @Override
    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }


    /**
     * @return 返回线程池最大线程数
     */
    @Override
    public int getMaxThreads() {
        return maxThreads;
    }


    /**
     * 设置线程池最大线程数
     * 
     * @param maxThreads 线程池最大线程数
     */
    @Override
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    
    /**
     * @return 返回多线程执行器
     */
    @Override
    public Executor getExecutor() {
        return executor;
    }


    /**
     * 设置线程池
     *
     * @param executor 使用自定义的线程池
     */
    @Override
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }


    /**
     * 创建线程池
     */
    private void createExecutor() {
        executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), 60,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory());
    }


    /**
     * 多线程任务执行
     *
     * @param task 管理的组件申请的多线程任务
     */
    @Override
    public void execute(Runnable task) {
        if (getExecutor() == null) {
            createExecutor();
        }

        getExecutor().execute(task);
    }
}
