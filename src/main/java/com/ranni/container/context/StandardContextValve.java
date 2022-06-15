package com.ranni.container.context;

import com.ranni.connector.http.request.HttpRequest;
import com.ranni.connector.Request;
import com.ranni.connector.Response;
import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * 适用于context的基础阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-29 21:36
 */
public class StandardContextValve extends ValveBase {

    public StandardContextValve(Context context) {
        setContainer(context);
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 在这个方法中寻找最终请求的wrapper包装的servlet对象
     * 并执行wrapper的invoke方法
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
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) return;

        String requestURI = ((HttpRequest)request).getDecodedRequestURI();

        Wrapper wrapper = null;

        try {
            wrapper = request.getWrapper();
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestURI);
        }

        if (wrapper == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
            return;
        }
        
        wrapper.invoke(request, response);
    }
}
