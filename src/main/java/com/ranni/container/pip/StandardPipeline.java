package com.ranni.container.pip;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.LifecycleListener;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准管道实现类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:45
 */
public class StandardPipeline implements Pipeline, Contained, Lifecycle {
    private Container container;
    private Valve[] valves = new Valve[0]; // 阀
    private Valve basic; // 基础阀
    private final ValveContext valveContext = new StandardValveContext();

    public StandardPipeline(Container container) {
        setContainer(container);
    }

    /**
     * 返回基础阀
     * @return
     */
    @Override
    public Valve getBasic() {
        return basic;
    }

    /**
     * 设置基础阀
     * @param valve
     */
    @Override
    public void setBasic(Valve valve) {
        this.basic = valve;
    }

    /**
     * 添加阀
     *
     * @param valve
     */
    @Override
    public void addValve(Valve valve) {
        if (valve instanceof Contained) {
            ((Contained) valve).setContainer(container);
        }
        synchronized (valves) {
            Valve[] newValues = new Valve[valves.length + 1];
            System.arraycopy(valves, 0, newValues, 0, valves.length);
            newValues[valves.length] = valve;
            valves = newValues;
        }
    }

    /**
     * 返回所有非基础阀
     * @return
     */
    @Override
    public Valve[] getValves() {
        return valves;
    }

    /**
     * 开始依次调用管道中的阀
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        valveContext.setValve(basic, valves);
        valveContext.invokeNext(request, response);
    }

    @Override
    public void removeValve(Valve valve) {

    }

    /**
     * 返回与此管道相关联的容器
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此管道相关联的容器
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public void start() throws LifecycleException {

    }

    @Override
    public void stop() throws LifecycleException {

    }
}
