package com.ranni.container.engine;

import com.ranni.connector.Request;
import com.ranni.connector.Response;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的Engine基础阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 15:06
 */
public class StandardEngineValve extends ValveBase {
    
    public StandardEngineValve(Engine engine) {
        setContainer(engine);
    }


    /**
     * 返回实现类的信息
     * 
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }


    /**
     * 执行阀
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
        if (!(request instanceof HttpServletRequest) 
            || !(response instanceof HttpServletResponse)) {
            return;
        }
        
        HttpServletRequest hsr = (HttpServletRequest) request;
        
        // 如果是HTTP/1.1请求，就必须携带serverName
        if ("HTTP/1.1".equals(hsr.getProtocol())
            && hsr.getServerName() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, request.getRequest().getServerName());
        }
        
        Host host = request.getHost();
        
        if (host == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "没有找到对应的Host！");
            return;
        }
           
        host.invoke(request, response);
    }
}
