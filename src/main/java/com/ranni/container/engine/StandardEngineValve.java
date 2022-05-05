package com.ranni.container.engine;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Engine;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;

import javax.servlet.ServletException;
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

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

    }
}
