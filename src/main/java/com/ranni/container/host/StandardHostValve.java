package com.ranni.container.host;

import com.ranni.connector.Request;
import com.ranni.connector.Response;
import com.ranni.container.Context;
import com.ranni.container.Host;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;
import com.ranni.container.session.Manager;
import com.ranni.container.session.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的Host基础阀实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 9:53
 */
public class StandardHostValve extends ValveBase {

    public StandardHostValve(Host host) {
        setContainer(host);
    }

    /**
     * 此实现类的信息
     * 
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 基础阀执行，找到适合的Context容器并执行Context容器的invoke
     * 
     * @param request
     * @param response
     * @param valveContext
     * 
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
        // 取得对应的Context容器
        Context context = request.getContext();
        
        // 没有匹配的Context容器，返回错误状态码以及错误信息
        if (context == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "没有找到对应的Context容器！");
            return;
        }

        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
        
        // 更新Session访问时间
        String sessionId = ((HttpServletRequest) request).getRequestedSessionId();
        if (sessionId != null) {
            Manager manager = context.getManager();
            if (manager != null) {
                Session session = manager.findSession(sessionId);
                if (session != null && session.isValid()) {
                    session.access();
                }
            }
        }

        context.invoke(request, response);
        
        Thread.currentThread().setContextClassLoader(oldCCL);

    }
}
