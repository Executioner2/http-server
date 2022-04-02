package com.ranni.exception;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-02 20:22
 */
public final class LifecycleException extends Exception {
    protected String message;
    protected Throwable throwable;


    public LifecycleException() {
        this(null, null);
    }

    public LifecycleException(String message) {
        this(message, null);
    }

    public LifecycleException(String message, Throwable cause) {
        super();
        this.message = message;
        this.throwable = cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("LifecycleException:  ");
        if (message != null) {
            sb.append(message);
            if (throwable != null) {
                sb.append(":  ");
            }
        }
        if (throwable != null) {
            sb.append(throwable.toString());
        }
        return (sb.toString());
    }
}
