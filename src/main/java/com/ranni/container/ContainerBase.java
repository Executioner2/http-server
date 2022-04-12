package com.ranni.container;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.loader.Loader;
import com.ranni.container.pip.Pipeline;
import com.ranni.container.pip.Valve;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.logger.Logger;
import com.ranni.resource.ProxyDirContext;

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
public abstract class ContainerBase implements Container, Pipeline {
    protected Loader loader; // 加载器
    protected Logger logger; // 日志记录器
    protected Pipeline pipeline = new SimplePipeline(this); // 管道
    protected Mapper mapper; // 默认关联mapper，即mappers中只有一个mapper，那么就会将此mapper设置为默认关联mapper
    protected Map<String, Mapper> mappers = new HashMap<>(); // 协议与mapper的映射集
    protected Container parent; // 父容器
    protected String name; // 容器名字
    protected Map<String, Container> children = new HashMap<>(); // 子容器
    protected boolean started; // 启动标志
    protected DirContext resources; // 容器资源

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
    public void setLoader(Loader loader) {
        this.loader = loader;
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


    @Override
    public ClassLoader getParentClassLoader() {
        return null;
    }


    @Override
    public void setParentClassLoader(ClassLoader parent) {

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
        DirContext oldResource = this.resources;

        if (oldResource == this.resources) return;

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
    protected void log(String message) {

        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message);
        else
            System.out.println(logName() + ": " + message);

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
}
