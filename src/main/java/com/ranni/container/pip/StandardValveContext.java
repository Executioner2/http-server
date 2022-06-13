package com.ranni.container.pip;

import com.ranni.connector.Request;
import com.ranni.connector.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准阀容器，主要用于遍历阀的执行
 * 此类在Tomcat 4 中作为标准管道实现类的内部类存在的
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:14
 */
public final class StandardValveContext implements ValveContext {
    private int stage; // 阀执行计数
    private Valve basic; // 基础阀
    private Valve[] valves; // 非基础阀

    /**
     * 返回该类的信息
     *
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 设置基础阀和非基础阀
     * 并且将阀执行计数清0
     *
     * @param basic
     * @param valves
     */
    @Override
    public void setValve(Valve basic, Valve[] valves) {
        stage = 0;
        this.basic = basic;
        this.valves = valves;
    }

    /**
     * 该方法实现执行下一个阀
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invokeNext(Request request, Response response) throws IOException, ServletException {
        // 在这里就要开始计数+1，因为有可能在阀中调用此方法
        int count = stage;
        stage++;

        if (count < valves.length) {
            // 执行非基础阀
            valves[count].invoke(request, response, this);
        } else if (count == valves.length && basic != null) {
            // 执行基础阀
            basic.invoke(request, response, this);
        } else {
            throw new ServletException("没有阀可以执行了！");
        }
    }
}
