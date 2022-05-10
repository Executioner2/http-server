package com.ranni.startup.core;

import com.ranni.common.SystemProperty;
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
import com.ranni.startup.Constants;
import org.apache.commons.digester3.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.URL;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/6 15:47
 */
public class ContextConfig implements LifecycleListener {
    private static Digester webDigester = createWebDigester(); // xml解析
    
    private Context context; // 关联的Context容器
    private int debug = Logger.INFORMATION; // 日志输出级别
    private boolean ok; // 是否配置成功标志位
    
    
    /**
     * 取得web的xml解析器
     * 
     * @return
     */
    private static Digester createWebDigester() {
        URL url = null;
        Digester webDigester = new Digester();
        webDigester.setValidating(true);

        // 设置约束文件
        url = ContextConfig.class.getResource(Constants.WEB_DTD_RESOURCE_PATH_22); // 取得本地xml约束文件（2-2版本）的URL
        webDigester.register(Constants.WEB_DTD_RESOURCE_PATH_22, url.toString()); // 将本地xml约束文件（2-2版本）注册到digester中（如果有的话）
        url = ContextConfig.class.getResource(Constants.WEB_DTD_RESOURCE_PATH_23); // 取得本地xml约束文件（2-3版本）的URL
        webDigester.register(Constants.WEB_DTD_RESOURCE_PATH_23, url.toString()); // 将本地xml约束文件（2-3版本）注册到digester中（如果有的话）       

        webDigester.addRuleSet(new WebRuleSet()); // 添加规则实例
        
        return webDigester;
    }


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
        defaultConfig();
        
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
     * 解析webapp的xml文件
     */
    private void applicationConfig() {
        // 取得webapp的xml文件的输入流（通过JNDI资源）
        ServletContext servletContext = context.getServletContext();
        InputStream input = null;
        
        if (servletContext != null) {
            input = servletContext.getResourceAsStream(Constants.APPLICATION_WEB_XML);    
        }
        
        if (input == null) {
            log("ContextConfig.applicationConfig  未找到webapp的xml文件！");
            return;
        }
        
        synchronized (webDigester) {
            try {
                URL url = servletContext.getResource(Constants.APPLICATION_WEB_XML);
                InputSource inputSource = new InputSource(url.toExternalForm());

                // XXX 设置欢迎文件
                
                webDigester.clear();
                webDigester.push(context);
                webDigester.parse(inputSource);
            } catch (SAXParseException e) {
                log("ContextConfig.applicationParse  XML文件解析异常！", e);
                ok = false;
            } catch (Exception e) {
                log("ContextConfig.applicationParse", e);
                ok = false;
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        log("ContextConfig.applicationConfig  文件输入流关闭异常！", e);
                    }
                }
            }
        }

    }


    /**
     * 解析服务器默认xml文件
     */
    private void defaultConfig() {
        // 取得服务器的默认xml文件
        File file = new File(System.getProperty(SystemProperty.SERVER_BASE), Constants.DEFAULT_WEB_XML);

        FileInputStream fis = null;
        
        // 测试文件能否正常打开
        try {
            fis = new FileInputStream(file.getCanonicalPath());
            fis.close();
            fis = null;
        } catch (FileNotFoundException e) {
            log("ContextConfig.defaultConfig  文件未找到！", e);
            return;
        } catch (IOException e) {
            log("ContextConfig.defaultConfig  文件不能正常打开！", e);
            return;
        }
        
        synchronized (webDigester) {
            try {
                InputSource inputSource = new InputSource("file://" + file.getAbsolutePath());
                fis = new FileInputStream(file);
                inputSource.setByteStream(fis);
                
                // XXX 设置欢迎文件
                
                webDigester.clear(); // 清空残留记录
                webDigester.push(context); // 将context实例压入栈，使此context实例为根bean
                webDigester.parse(inputSource);
            } catch (SAXParseException e) {
                log("ContextConfig.defaultParse  XML解析异常！", e);
                ok = false;
            } catch (Exception e) {
                log("ContextConfig.defaultParse", e);
                ok = false;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log("ContextConfig.defaultConfig  文件输入流关闭异常！", e);
                    }
                }
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
