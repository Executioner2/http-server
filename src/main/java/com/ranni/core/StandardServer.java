package com.ranni.core;

import com.ranni.deploy.NamingResources;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

/**
 * Title: HttpServer
 * Description:
 * 标准的服务器实现类
 * XXX 后续应该实现server.xml读取
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 9:28
 */
public class StandardServer implements Lifecycle, Server {
    
    private LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具实例
    private boolean initialize; //  服务器初始化标志位
    private boolean started; // 服务器启动标志位
    private Service[] services = new Service[0]; // 服务集合
    private int port = 8085; // 服务器的端口
    private String shutdown = "SHUTDOWN"; // 关服务器的指令
    
    
    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public NamingResources getGlobalNamingResources() {
        return null;
    }

    @Override
    public void setGlobalNamingResources(NamingResources globalNamingResources) {

    }


    /**
     * 返回服务器的端口号，不是webapp的
     * 
     * @return
     */
    @Override
    public int getPort() {
        return this.port;
    }


    /**
     * 设置服务器的端口号，不是webapp的
     * 
     * @param port
     */
    @Override
    public void setPort(int port) {
        this.port = port;
    }


    /**
     * 取得关闭指令
     * 
     * @return
     */
    @Override
    public String getShutdown() {
        return this.shutdown;
    }


    /**
     * 设置关闭指令
     * 
     * @param shutdown
     */
    @Override
    public void setShutdown(String shutdown) {
        this.shutdown = shutdown;
    }


    /**
     * 添加服务
     * 如果服务器已经初始化，该服务也要初始化
     * 如果服务器已经启动，该服务如果实现了生命周期接口就也要启动
     * 
     * @param service
     */
    @Override
    public void addService(Service service) {
        
        service.setServer(this);
        
        synchronized (services) {
            Service[] newArs = new Service[this.services.length + 1];
            System.arraycopy(services, 0, newArs, 0, services.length);
            newArs[services.length] = service;
            services = newArs;
            
            if (initialize) {
                try {
                    service.initialize();
                } catch (LifecycleException e) {
                    e.printStackTrace(System.err);
                }
            }
            
            if (started && service instanceof Lifecycle) {
                try {
                    ((Lifecycle) service).start();
                } catch (LifecycleException e) {
                    ;
                }
            }
            
        }
        
    }


    /**
     * 等待收到关机指令
     */
    @Override
    public void await() {

    }


    /**
     * 根据服务名取得服务
     * 
     * @param name
     * @return
     */
    @Override
    public Service findService(String name) {
        
        if (name == null)
            return null;
        
        synchronized (services) {
            for (Service service : services) {
                if (name.equals(service.getName()))
                    return service;
            }
        }
        
        return null;        
    }


    /**
     * 返回所有的服务
     * 
     * @return
     */
    @Override
    public Service[] findServices() {
        
        synchronized (services) {
            return services;
        }
    }


    /**
     * 移除服务
     * 如果服务器已经启动，那么该服务需要关闭
     * 
     * @param service
     */
    @Override
    public void removeService(Service service) {
        
        synchronized (services) {
            
            int i = 0;
            
            for (; i < services.length; i++) {
                if (services[i] == service)
                    break;
            }
            
            if (i == services.length)
                return;

            Service[] newArs = new Service[this.services.length - 1];
            for (int j = 0, k = 0; j < services.length; j++) {
                if (j != i)
                    newArs[k++] = services[j];
            }

            services = newArs;
            
            if (started && service instanceof Lifecycle) {
                try {
                    ((Lifecycle) service).stop();
                } catch (LifecycleException e) {
                    ;
                }
            }

        }
        
    }


    /**
     * 初始化
     * 
     * @throws LifecycleException
     */
    @Override
    public void initialize() throws LifecycleException {
        if (initialize)
            throw new LifecycleException("StandardServer.initialize  服务器已经初始化！");
        
        initialize = true;
        
        // 初始化所有服务
        for (Service service : services) {
            service.initialize();
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
     * 启动服务器
     * 
     * @throws LifecycleException
     */
    @Override
    public void start() throws LifecycleException {
        
        if (started)
            throw new LifecycleException("StandardServer.start  服务器已经启动！");
        
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
        
        // 启动所有服务
        synchronized (services) {
            for (Service service : services) {
                if (service instanceof Lifecycle)
                    ((Lifecycle) service).start();
            }
        }
        
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);        
    }


    /**
     * 停止服务器
     * 
     * @throws LifecycleException
     */
    @Override
    public void stop() throws LifecycleException {
        if (!started)
            throw new LifecycleException("StandardServer.start  服务器已经停止！");

        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // 停止所有服务
        synchronized (services) {
            for (Service service : services) {
                if (service instanceof Lifecycle)
                    ((Lifecycle) service).stop();
            }
        }

        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }
}
