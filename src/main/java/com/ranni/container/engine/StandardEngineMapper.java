package com.ranni.container.engine;

import com.ranni.connector.http.request.Request;
import com.ranni.container.Container;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.Mapper;
import com.ranni.container.host.StandardHost;
import com.ranni.logger.Logger;

/**
 * Title: HttpServer
 * Description:
 * 标准的Engine映射器
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/5 15:06
 */
public class StandardEngineMapper implements Mapper {
    protected Engine engine; // 此映射器关联的Context容器
    protected String protocol; // 该映射器负责处理的协议


    /**
     * 返回关联的容器
     * 
     * @return
     */
    @Override
    public Container getContainer() {
        return this.engine;
    }


    /**
     * 设置关联的容器
     * 
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        if (!(container instanceof StandardEngine))
            throw new IllegalArgumentException("不是标准Engine容器！");
        
        this.engine = (Engine) container;
    }


    /**
     * 返回关联的协议
     * 
     * @return
     */
    @Override
    public String getProtocol() {
        return this.protocol;
    }


    /**
     * 设置关联的协议
     * 
     * @param protocol
     */
    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * 返回请求中指向的Context容器
     * 
     * 取得服务名：
     *  1、先从请求实例中取得绑定的服务名；
     *  2、如果不存在，尝试取得默认的Host名；
     *  3、如果不存在，返回null
     *  
     * 查询Host：
     *  1、先尝试从将server直接作为Host名查询；
     *  2、如果不存在，将server作为Host的别名查询；
     *  3、如果不存在，使用默认Host（不存在的话最终也是返回null）；
     *  4、返回最终host
     * 
     * @param request
     * @param update
     * @return
     */
    @Override
    public Container map(Request request, boolean update) {
        int debug = -1;
        if (engine instanceof StandardEngine)
            debug = ((StandardEngine) engine).getDebug();        
        
        // 先尝试从请求实例中获取绑定Host
        String server = request.getRequest().getServerName();
        
        // 不存在则从默认Host中取得
        if (server == null) {
            server = engine.getDefaultHost();
            if (update)
                request.setServerName(server);
        }

        // 还是不存在直接返回null
        if (server == null)
            return null;

        server = server.toLowerCase();
        
        if (debug >= Logger.INFORMATION)
            ((StandardEngine) engine).log("取得服务名： " + server);
        
        // 从map中查询HOST容器
        Host host = (Host) engine.findChild(server);
        
        // 不存在，以别名查询
        if (host == null) {
            if (debug >= Logger.WARNING)
                ((StandardEngine) engine).log("尝试从别名中查询Host");
            
            for (Host child : (Host[]) engine.findChildren()) {
                if (child.findAliases(server)) {
                    host = child;
                    break;
                }
            }
        }
        
        // 尝试使用默认Host
        if (host == null) {
            if (debug >= Logger.WARNING)
                ((StandardEngine) engine).log("尝试使用默认Host");
            host = (Host) engine.findChild(engine.getDefaultHost());
        }

        return host;
    }
}
