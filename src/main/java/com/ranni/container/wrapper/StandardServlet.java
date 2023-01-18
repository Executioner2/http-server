package com.ranni.container.wrapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ranni.annotation.core.*;
import com.ranni.common.Globals;
import com.ranni.container.ContainerServlet;
import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import com.ranni.handler.JSONException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * 标准的servlet实现类，所有Controller实例都会被关联到一个StandardServlet实例
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 15:30
 */
public final class StandardServlet extends HttpServlet implements ContainerServlet {
    
    private Object controller; // controller实例 
    private Class clazz; // controller类
    private String baseUri; // 基本路径
    private int prefixLen; // 前缀长度
    private Map<String, Method> methodMap = new HashMap<>(); // 方法映射
    private String info = "StandardServlet/1.0"; // 实现信息
    private Wrapper wrapper; // wrapper
    private ServletConfig servletConfig; // servlet配置
    

    public StandardServlet() {
        
    }
    
    
    /**
     * 初始化
     * 
     * @param servletConfig servlet配置信息
     * @throws ServletException 可能抛出Servlet异常
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        
        Map<String, Class> attribute = (Map<String, Class>) servletConfig.getServletContext()
                .getAttribute(Globals.APPLICATION_CONTROLLER_CLASSES);
        
        Class aClass = attribute.get(wrapper.getName());
        this.clazz = aClass;
        
        try {
            this.controller = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ServletException(e);
        } 

        this.baseUri = ((Controller) clazz.getDeclaredAnnotation(Controller.class)).value();
        this.prefixLen = ((Context) wrapper.getParent()).getPath().length() + baseUri.length();
        
        // 扫描所有Mapping注解标识的method
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            RequestMapping annotation = method.getDeclaredAnnotation(RequestMapping.class);
            if (annotation == null) {
                continue;
            }

            String path = annotation.value();
            methodMap.put(path, method);
        }
    }


    /**
     * 返回这个servletConfig
     * 
     * @return
     */
    @Override
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }


    /**
     * 处理请求的入口方法
     * XXX - 目标方法执行的前后插入监听
     * 
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI().substring(prefixLen);
        if (!methodMap.containsKey(uri)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "请求未找到！ requestURI：" + req.getRequestURI());
            return;
        }

        Method method = methodMap.get(uri);
        RequestMapping requestMapping = method.getDeclaredAnnotation(RequestMapping.class);
        
        // 请求方法不对
        if (!("".equals(requestMapping.method())) && !requestMapping.method().equals(req.getMethod())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请求方法错误！  method：" + req.getMethod());
            return;
        }
        
        // 调用处理方法
        try {
            Object[] args = parseParams(req, method); // 自动值填充
            Object res = method.invoke(controller, args); // 执行controller对应的方法并返回值
            
            ContentType contentType = requestMapping.contentType();
            resp.setContentType(contentType.getValue());
            // TODO - 后置处理
            
            resp.setHeader("Access-Control-Allow-Origin", "*");
            if (resp.getCharacterEncoding() == null) {
                resp.setCharacterEncoding("utf-8");
            }
            if (requestMapping.charset() != Charset.NULL) {
                resp.setCharacterEncoding(requestMapping.charset().getValue());
            }

            if (res == null) {                
                return;
            }
            
            // XXX - 应该做更灵活的处理
            switch (contentType) {
                case TEXT: case HTML: {
                    PrintWriter writer = resp.getWriter();
                    writer.print(res);
                    break;
                } case JSON: {
                    PrintWriter writer = resp.getWriter();
                    try {
                        writer.print(JSON.toJSONString(res));    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                        
                    break;
                } case OCTET_STREAM: {
                    if (!(res instanceof Serializable)) {
                        throw new SerialException("对象不能序列化 " + res);
                    }
                    ServletOutputStream outputStream = resp.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                    oos.writeObject(res);
                    break;
                } case FORM_DATA: {
                    // XXX - multipart/form-data类型。很重要，需要尽早实现  
                } default: {
                    throw new IllegalArgumentException("未知响应类型！");
                }
            }            
            
        } catch (IllegalAccessException e) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "无方法执行权限！ " + method.getName());
            e.printStackTrace(System.err);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, e.toString());
            e.printStackTrace(System.err);
        }
    }

    @Override
    public String getServletInfo() {
        return this.info;
    }


    /**
     * 销毁
     */
    @Override
    public void destroy() {
        Map<String, Class> attribute = (Map<String, Class>) servletConfig.getServletContext()
                .getAttribute(Globals.APPLICATION_CONTROLLER_CLASSES);
        
        if (attribute != null)
            attribute.remove(wrapper.getName());
        
        methodMap.clear();
        wrapper = null;
        servletConfig = null;
        clazz = null;
        baseUri = null;
        controller = null;
        
        log("StandardServlet.destroy  servlet已销毁！");
    }


    /**
     * 解析请求参数
     * FIXME - 存在编码问题
     * 
     * @param hsr
     * @param method
     */
    private Object[] parseParams(HttpServletRequest hsr, Method method) throws JSONException {
        Parameter[] parameters = method.getParameters(); // 所有形参
        Object[] res = new Object[parameters.length];
        boolean isGet = "GET".equals(hsr.getMethod());
        
        for (int i = 0; i < parameters.length; i++) {
            
            for (Annotation annotation : parameters[i].getDeclaredAnnotations()) {

                String paramName = ((RequestParam) annotation).value();
                if (paramName == null || "".equals(paramName))
                    paramName = parameters[i].getName();
                
                if (annotation instanceof RequestParam) {
                    
                    // XXX 是否应该让非GET请求的URI携带参数无效化
                    String value = hsr.getParameter(paramName);

                    if (value != null) {
                        res[i] = JSONObject.parseObject(value, parameters[i].getType());
//                        res[i] = JSONUtil.getInstance(parameters[i].getType(), value);

                        if (res[i] == null)
                            res[i] = value;
                    }
                    
                    break;
                    
                } else if (annotation instanceof RequestBody) {
                    // 将JSON字符串转为实例                    
                    Class<?> type = parameters[i].getType();
                    Object obj = null;
                    
                    // 解析JSON字符串并填充到实例中
                    if (Collection.class.isAssignableFrom(type)) {
                        // 是数组                        
                        obj = JSONObject.parseObject(hsr.getParameter(paramName), ((RequestBody) annotation).value());
//                        obj = JSONUtil.parseJSON(((RequestBody) annotation).value(), ArrayList.class, hsr.getParameter(paramName));
                    } else {
                        obj = JSONObject.parseObject(hsr.getParameter(paramName), type);
//                        obj = JSONUtil.parseJSON(type, null, hsr.getParameter(paramName));
                    }
                    
                    res[i] = obj;
                    
                    break;
                }
            }
            
        }
        
        return res;
    }
    

    /**
     * 返回与此内部servlet关联的wrapper
     * 
     * @return
     */
    @Override
    public Wrapper getWrapper() {
        return this.wrapper;
    }


    /**
     * 设置与此内部servlet关联的wrapper
     * 
     * @param wrapper
     */
    @Override
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }
}
