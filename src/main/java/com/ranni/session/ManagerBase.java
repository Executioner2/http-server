package com.ranni.session;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 19:08
 */
public abstract class ManagerBase {
    public abstract void recycle(StandardSession standardSession);


    public abstract int getDebug();
}
