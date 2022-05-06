package com.ranni.container.engine;

import com.ranni.container.*;
import com.ranni.core.Service;
import com.ranni.lifecycle.LifecycleException;

/**
 * Title: HttpServer
 * Description:
 * 标准的Engine实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 15:01
 */
public class StandardEngine extends ContainerBase implements Engine {
    private String mapperClass = "com.ranni.container.engine.StandardEngineMapper"; // 标准的Engine映射器
    private String defaultHost; // 默认Host容器
    private DefaultContext defaultContext; // 默认Context容器
    private String jvmRouteId; // 用于集群标识
    private Service service; // 此容器关联的服务


    public StandardEngine() {
        pipeline.setBasic(new StandardEngineValve(this));
    }


    /**
     * 返回默认的Host容器
     * 
     * @return
     */
    @Override
    public String getDefaultHost() {
        return this.defaultHost;
    }


    /**
     * 设置默认的Host容器
     * 转小写
     * 
     * @param defaultHost
     */
    @Override
    public void setDefaultHost(String defaultHost) {
        if (defaultHost == null) 
            this.defaultHost = null;
        else
            this.defaultHost = defaultHost.toLowerCase();
    }


    /**
     * 返回JvmRouteId
     * @return
     */
    @Override
    public String getJvmRoute() {
        return this.jvmRouteId;
    }


    /**
     * 设置JvmRouteId
     * 
     * @param jvmRouteId
     */
    @Override
    public void setJvmRoute(String jvmRouteId) {
        this.jvmRouteId = jvmRouteId;
    }


    /**
     * 返回此容器关联的服务
     * 
     * @return
     */
    @Override
    public Service getService() {
        return this.service;
    }

    
    /**
     * 设置此容器关联的服务
     * 
     * @param service
     */
    @Override
    public void setService(Service service) {
        this.service = service;
    }


    /**
     * 添加默认的Context容器
     * 
     * @param defaultContext
     */
    @Override
    public void addDefaultContext(DefaultContext defaultContext) {
        this.defaultContext = defaultContext; 
    }


    /**
     * 返回默认的Context容器
     * 
     * @return
     */
    @Override
    public DefaultContext getDefaultContext() {
        return this.defaultContext;
    }


    /**
     * 如果默认Context不为空，那么导入传入的Context容器
     * 
     * @see {@link com.ranni.container.StandardDefaultContext#importDefaultContext(Context)}
     * 
     * @param context
     */
    @Override
    public void importDefaultContext(Context context) {
        if (this.defaultContext != null)
            this.defaultContext.importDefaultContext(context);
    }


    /**
     * 实现类信息
     * 
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }

    
    @Override
    public void backgroundProcessor() {
        
    }


    /**
     * 添加子容器
     * 只能添加Host作为子容器
     * 
     * @param child
     */
    @Override
    public void addChild(Container child) {
        if (!(child instanceof Host))
            throw new IllegalArgumentException("StandardEngine.addChild  只能添加Host做子容器！");
        
        super.addChild(child);
    }


    /**
     * 不可添加父容器
     * 
     * @param container
     */
    @Override
    public void setParent(Container container) {
        throw new IllegalArgumentException("StandardEngine.setParent  Engine容器不可添加父容器！");
    }


    /**
     * 启动Engine
     * 
     * @throws LifecycleException
     */
    @Override
    public void start() throws LifecycleException {
        super.start();
    }


    /**
     * 返回mapper全限定类名
     * 
     * @return
     */
    public String getMapperClass() {
        return mapperClass;
    }

    
    /**
     * 设置mapper全限定类名
     * 
     * @param mapperClass
     */
    public void setMapperClass(String mapperClass) {
        this.mapperClass = mapperClass;
    }


    /**
     * 添加默认的映射器
     * 固定为StandardEngine中的mapperClass属性（该属性可以修改）
     * 
     * @param mapperClass
     */
    @Override
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(this.mapperClass);
    }
}
