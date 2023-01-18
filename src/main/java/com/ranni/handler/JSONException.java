package com.ranni.handler;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 19:55
 */
@Deprecated
public class JSONException extends Exception {
    private String msg;
    private Throwable throwable;
    
    public JSONException() {
    }

    public JSONException(String message) {
        super(message);
        this.msg = message;
    }

    public JSONException(String message, Throwable cause, String msg, Throwable throwable) {
        super(message, cause);
        this.msg = msg;
        this.throwable = throwable;
    }

    @Override
    public String getMessage() {
        return this.msg;
    }
}
