package com.ranni.connector;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 22:04
 */
public class ClientAbortException extends IOException {

    public ClientAbortException() {
        super();
    }

    
    public ClientAbortException(String message) {
        super(message);
    }


    public ClientAbortException(Throwable throwable) {
        super(throwable);
    }
    
    
    public ClientAbortException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
