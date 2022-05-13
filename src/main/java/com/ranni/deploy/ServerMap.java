package com.ranni.deploy;

import com.ranni.core.Server;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 23:46
 */
public class ServerMap {
    private Server server;
    private ServerConfigure serverConfigure;

    public ServerMap() {
    }

    public ServerMap(Server server, ServerConfigure serverConfigure) {
        this.server = server;
        this.serverConfigure = serverConfigure;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public ServerConfigure getServerConfigure() {
        return serverConfigure;
    }

    public void setServerConfigure(ServerConfigure serverConfigure) {
        this.serverConfigure = serverConfigure;
    }
}
