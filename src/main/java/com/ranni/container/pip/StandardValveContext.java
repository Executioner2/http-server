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
    private Valve basic; // 基础阀
    private Valve[] valves; // 非基础阀
    private ThreadLocal<Integer> local; // 阀执行计数

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
        local = new ThreadLocal<>();
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
        Integer stage = local.get();
        stage = stage == null ? 0 : stage;
        
        if (stage < valves.length) {
            // 执行非基础阀
            local.set(stage + 1);
            valves[stage].invoke(request, response, this);
        } else if (stage == valves.length && basic != null) {
            // 执行基础阀
            local.set(0);
            basic.invoke(request, response, this);
        } else {
            local.set(0);
            throw new ServletException("没有阀可以执行了！");
        }
    }
}
