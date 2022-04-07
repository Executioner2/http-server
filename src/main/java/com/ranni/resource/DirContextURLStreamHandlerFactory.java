package com.ranni.resource;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 17:34
 */
public class DirContextURLStreamHandlerFactory implements URLStreamHandlerFactory {
    public DirContextURLStreamHandlerFactory() {
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals("jndi")) {
            return new DirContextURLStreamHandler();
        } else {
            return null;
        }
    }
}
