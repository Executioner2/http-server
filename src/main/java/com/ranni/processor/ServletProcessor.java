package com.ranni.processor;

import com.ranni.connector.Constants;
import com.ranni.processor.http.HttpRequest;
import com.ranni.processor.http.HttpRequestFacade;
import com.ranni.processor.http.HttpResponse;
import com.ranni.processor.http.HttpResponseFacade;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 20:52
 */
public class ServletProcessor {
    public ServletProcessor() {

    }

    /**
     * 用于将请求交给具体的servlet处理
     * @param request
     * @param response
     */
    public void process(HttpRequest request, HttpResponse response) {
        try {
            // 获取servlet的路径并创建url对象
            StringBuffer requestURL = request.getRequestURL();
            String servletName = requestURL.substring(requestURL.lastIndexOf("/") + 1);
            URL[] urls = new URL[1];

            File path = new File(Constants.WEB_ROOT);
            String repository = (new URL("file", null, path.getAbsolutePath() + File.separator)).toString();
//            String repository = "file:" + Constants.WEB_ROOT + File.separator; // 上面的代码大概就是这行的功能

            URLStreamHandler streamHandler = null;
            urls[0] = new URL(null, repository, streamHandler); // 必须要传个URLStreamHandler类型进去，就传个URLStreamHandler类型的null

            // 将urls作为参数创建URL类加载器对象
            URLClassLoader urlClassLoader = new URLClassLoader(urls);
            Class<?> aClass = urlClassLoader.loadClass(servletName); // XXX 被加载的类文件不能有package（即不能把自己打包）
            Servlet servlet = (Servlet) aClass.getConstructor().newInstance();
            servlet.service(new HttpRequestFacade(request), new HttpResponseFacade(response));
            response.finishResponse(); // 释放资源

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }

    }
}
