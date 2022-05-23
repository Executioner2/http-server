package com.ranni.coyote;

import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 21:34
 */
public class CloseNowException extends IOException {
    public CloseNowException() {
        super();
    }


    public CloseNowException(String message, Throwable cause) {
        super(message, cause);
    }


    public CloseNowException(String message) {
        super(message);
    }


    public CloseNowException(Throwable cause) {
        super(cause);
    }
}
