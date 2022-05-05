package com.ranni.container.pip;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 错误调度阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 9:56
 */
public class ErrorDispatcherValve extends ValveBase {

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

    }
}
