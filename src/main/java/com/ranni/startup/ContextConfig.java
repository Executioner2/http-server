package com.ranni.startup;

import com.ranni.annotation.core.Controller;
import com.ranni.annotation.core.Controllers;
import com.ranni.common.Globals;
import com.ranni.connector.Mapper;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.container.context.StandardContext;
import com.ranni.container.scope.ApplicationContext;
import com.ranni.container.wrapper.StandardWrapper;
import com.ranni.core.FilterDef;
import com.ranni.deploy.FilterMap;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleEvent;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.logger.Logger;
import com.ranni.naming.FileDirContext;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 15:47
 */
public class ContextConfig implements LifecycleListener {    
    private Context context; // 关联的Context容器
    private int debug = Logger.INFORMATION; // 日志输出级别
    private boolean ok; // 是否配置成功标志位
    private boolean applicationConfig; // webapp正在配置的标志位
    private String servletBaseClass = "com.ranni.container.wrapper.StandardServlet"; // 模板servlet类
    
    
    /**
     * 触发生命周期事件
     * 
     * @param event
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        
        try {
            context = (Context) event.getLifecycle();
            if (context instanceof StandardContext) {
                int contextDebug = ((StandardContext) context).getDebug();
                if (contextDebug > this.debug)
                    this.debug = contextDebug;
            }
                
        } catch (ClassCastException cce) {
            log("ContextConfig.lifecycleEvent  类转换异常", cce);
            return;
        }
        
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();
        
    }


    /**
     * Context容器停止事件
     * 移除所有子容器
     * 移除应用程序监听器
     * 移除所有应用程序参数
     * 移除所有过滤定义
     * 移除所有过滤器映射
     * 移除所有实例监听器
     * 移除所有参数
     * 移除所有servlet映射
     * 移除所有欢迎页
     */
    private synchronized void stop() {

        // 移除子容器
        Container[] children = context.findChildren();
        for (int i = 0; i < children.length; i++) {
            context.removeChild(children[i]);
        }
        

        // TODO 移除错误页
        
        // 移除所有过滤定义
        FilterDef[] filterDefs = context.findFilterDefs();
        for (int i = 0; i < filterDefs.length; i++) {
            context.removeFilterDef(filterDefs[i]);
        }

        // 移除所有过滤器映射
        FilterMap[] filterMaps = context.findFilterMaps();
        for (int i = 0; i < filterMaps.length; i++) {
            context.removeFilterMap(filterMaps[i]);
        }
        

        // TODO 移除MIME
        

        // TODO 移除 security role

        // 移除所有servlet映射
        String[] servletMappings = context.findServletMappings();
        for (int i = 0; i < servletMappings.length; i++) {
            context.removeServletMapping(servletMappings[i]);
        }

        // TODO 移除taglibs

        // TODO 移除所有欢迎页面

        // 移除wrapper的生命周期类
        String[] wrapperLifecycles = context.findWrapperLifecycles();
        for (int i = 0; i < wrapperLifecycles.length; i++) {
            context.removeWrapperLifecycle(wrapperLifecycles[i]);
        }

        // 移除wrapper的监听器类
        String[] wrapperListeners = context.findWrapperListeners();
        for (int i = 0; i < wrapperListeners.length; i++) {
            context.removeWrapperListener(wrapperListeners[i]);
        }

        ok = true;
    }


    /**
     * Context容器启动事件
     * 解析服务器默认的xml文件
     * 解析webapp的xml文件
     * 
     * FIXME - 无安全校验
     */
    private synchronized void start() {
        
        context.setConfigured(false);
        ok = true;
        
        // XXX - 如果容器不可覆盖，那么设当前容器为服务器的默认容器
        
        // 解析服务器默认xml文件
//        defaultConfig();
        
        // 解析webapp的xml文件
        applicationConfig();
        
        if (ok) {
            context.setConfigured(true);
        } else {
            log("ContextConfig.start  容器配置无效！");
            context.setConfigured(false);
        }
        
    }


    /**
     * 加载启动类
     * TODO - 插入启动类注解扫描事件
     */
    private void applicationConfig() {
        if (applicationConfig)
            return;
        
        applicationConfig = true;
        
        Class webappBootstrapClass = (Class) context.getServletContext().getAttribute(Globals.APPLICATION_BOOTSTRAP_CLASS);

        Annotation[] declaredAnnotations = webappBootstrapClass.getDeclaredAnnotations();
        for (Annotation annotation : declaredAnnotations) {
            if (annotation instanceof Controllers) {
                // 创建映射关系
                scanController(((Controllers) annotation).value());
            }
        }
        
        applicationConfig = false;
    }


    /**
     * 扫描controller
     * XXX - 这是个极其耗时长的工作
     * 
     * @param paths
     */
    private void scanController(String[] paths) {
        DirContext resources = context.getResources();

        for (String path : paths) {
            try {
                NamingEnumeration<NameClassPair> it = resources.list(context.getLoader().getClassesPath() + "\\" + path.replaceAll("\\.", "\\\\"));
                Queue<NamingEnumeration<NameClassPair>> queue = new LinkedList<>();
                Queue<String> segments = new LinkedList<>();
                queue.offer(it);
                segments.offer("."); // 上一段包名
                
                while (!queue.isEmpty()) {
                    it = queue.poll();
                    String segment = segments.poll();
                    while (it.hasMore()) {
                        Binding binding = (Binding) it.nextElement(); // FIXME - webapp打成jar包的情况没做处理
                        if (binding.getObject() instanceof FileDirContext) {
                            queue.offer(resources.list(((FileDirContext) binding.getObject()).getDocBase()));
                            segments.offer(segment + '.' + binding.getName() + '.');
                        } else {
                            try {
                                // 添加到容器中
                                String controllerName = binding.getName().substring(0, binding.getName().length() - 6);
                                addController(path + segment + controllerName);
                                
                            } catch (Exception e) {
                                ok = false;
                                e.printStackTrace();
                                return;
                            }
                        }
                    }
                }
                

            } catch (NamingException e) {
                ok = false;
                e.printStackTrace();
                return;
            }
        }
    }


    /**
     * 添加这个controller类到全局上下文作用域中
     * 创建一个对应的模板servlet实例
     * 添加对应的servletMapping
     * 
     * @param controller
     * @throws Exception
     */
    private void addController(String controller) throws Exception {
        Class<?> aClass = context.getLoader().getClassLoader().loadClass(controller);
        Controller annotation = aClass.getDeclaredAnnotation(Controller.class);
        if (annotation == null)
            return;

        ApplicationContext servletContext = (ApplicationContext) context.getServletContext();
        StandardWrapper standardWrapper = null;
        Object controllerMap = null;

        synchronized (servletContext) {
            controllerMap = servletContext.getAttribute(Globals.APPLICATION_CONTROLLER_CLASSES);
            if (controllerMap == null) {
                controllerMap = new HashMap<String, Object>();
                servletContext.setAttribute(Globals.APPLICATION_CONTROLLER_CLASSES, controllerMap);
                servletContext.setAttributeReadOnly(Globals.APPLICATION_CONTROLLER_CLASSES); // 设置为只读，避免容器重载导致此属性被清除
            }
        }

        Container child = context.findChild(controller);

        // 进入到这里说明子容器也已经都启动了
        if (child != null) {
            if (child instanceof Lifecycle)
                ((Lifecycle) child).stop();

            context.removeChild(child);
        }
        
        synchronized (controllerMap) {
            ((Map<String, Class>) controllerMap).put(controller, aClass);
        }

        standardWrapper = new StandardWrapper();
        standardWrapper.setServletClass(servletBaseClass);
        standardWrapper.setName(controller);
        standardWrapper.setPath(annotation.value());
        context.addServletMapping(annotation.value(), controller); // XXX - 需要移除此方法
        context.addChild(standardWrapper);
        Engine engine = (Engine) context.getParent().getParent();
        Mapper mapper = engine.getService().getMapper();
        mapper.addWrapper(standardWrapper);
    }
    

    /**
     * 日志输出
     * 
     * @param message
     */
    private void log(String message) {

        Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "]: " + message);
        else
            System.out.println("ContextConfig[" + context.getName() + "]: " + message);
    }


    /**
     * 日志输出
     * 
     * @param message
     * @param throwable
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "] " + message, throwable);
        else {
            System.out.println("ContextConfig[" + context.getName() + "]: " + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 修改模板servlet类
     * 
     * @param servletBaseClass
     */
    public void setServletBaseClass(String servletBaseClass) {
        if (applicationConfig)
            throw new IllegalStateException("ContextConfig.setServletBaseClass  application正在配置中，无法更改模板servlet类");
        
        this.servletBaseClass = servletBaseClass;
    }
}
