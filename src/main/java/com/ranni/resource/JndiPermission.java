package com.ranni.resource;

import java.security.BasicPermission;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-12 16:23
 */
public class JndiPermission extends BasicPermission {
    public JndiPermission(String name) {
        super(name);
    }

    public JndiPermission(String name, String actions) {
        super(name, actions);
    }
}
