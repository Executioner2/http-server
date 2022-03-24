package com.ranni.session;

import javax.servlet.http.HttpSession;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-24 14:00
 */
public interface Session {
    // session是否有效
    boolean isValid();

    // 返回HttpSession
    HttpSession getSession();
}
