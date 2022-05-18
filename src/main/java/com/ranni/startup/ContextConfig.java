package com.ranni.startup;

import com.ranni.annotation.core.Controllers;
import com.ranni.common.Globals;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.core.FilterDef;
import com.ranni.deploy.ApplicationParameter;
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
import java.util.LinkedList;
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

        // 移除应用程序监听器
        String[] applicationListeners = context.findApplicationListeners();
        for (int i = 0; i < applicationListeners.length; i++) {
            context.removeApplicationListener(applicationListeners[i]);
        }

        // 移除所有应用程序参数
        ApplicationParameter[] applicationParameters = context.findApplicationParameters();
        for (int i = 0; i < applicationParameters.length; i++) {
            context.removeApplicationParameter(applicationParameters[i].getName());
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

        // 移除所有实例监听器
        String[] instanceListeners = context.findInstanceListeners();
        for (int i = 0; i < instanceListeners.length; i++) {
            context.removeInstanceListener(instanceListeners[i]);
        }

        // TODO 移除MIME

        // 移除所有参数
        String[] parameters = context.findParameters();
        for (int i = 0; i < parameters.length; i++) {
            context.removeParameter(parameters[i]);
        }

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
        
        // 如果容器不可覆盖，那么设当前容器为服务器的默认容器
        Container container = context.getParent();
        if (!context.getOverride()) {
            if (container instanceof Host) {
                ((Host) container).importDefaultContext(context);
                container = container.getParent();
            }
            
            if (container instanceof Engine) {
                ((Engine) container).importDefaultContext(context);
            }
        }
        
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
        Class webappBootstrapClass = (Class) context.getServletContext().getAttribute(Globals.APPLICATION_BOOTSTRAP_CLASS);

        Annotation[] declaredAnnotations = webappBootstrapClass.getDeclaredAnnotations();
        for (Annotation annotation : declaredAnnotations) {
            if (annotation instanceof Controllers) {
                // 创建映射关系
                scanController(((Controllers) annotation).value());
            }
        }
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
                                String controllerName = binding.getName().substring(0, binding.getName().length() - 6);
                                context.addController(path + segment + controllerName);
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
    
}
