package com.ranni.coyote;

import com.ranni.connector.InputBuffer;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.http.Parameters;
import com.ranni.util.http.ServerCookies;

import javax.servlet.ReadListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Title: HttpServer
 * Description:
 * 处理底层的socket，将http的请求中字节流的
 * 处理封装到此类中。
 * 注意：此类非HttpServletRequest的实现
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 23:29
 * @Ref org.apache.coyote.Request
 */
public final class Request {

    private ActionHook hook;
    private ReadListener listener;
    private int available;
    
    private final Object notes[] = new Object[Constants.MAX_NOTES];

    private long threadId; // 请求线程id
    private Charset charset;

    private MessageBytes methodMB; // 请求方法


    public final Object getNote(int pos) {
        return notes[pos];
    }
    
    
    /**
     * 事件响应
     * 
     * @param actionCode 动作代码
     * @param param 携带参数
     */
    public void action(ActionCode actionCode, Object param) {
        if (hook != null) {
            if (param == null) {
                hook.action(actionCode, this);
            } else {
                hook.action(actionCode, param);
            }
        }
    }

    public ReadListener getReadListener() {
        return listener;
    }

    public int getAvailable() {
        return available;
    }

    public void setReadListener(ReadListener listener) {
        this.listener = listener;
    }

    public boolean isFinished() {
        return false;
    }

    public boolean isRequestThread() {
        return Thread.currentThread().getId() == threadId;
    }

    public boolean isRead() {
        return false;
    }

    public void setErrorException(IOException ioe) {
    }

    public int doRead(InputBuffer inputBuffer) throws IOException {
        return 0;
    }

    public Charset getCharset() throws UnsupportedEncodingException {
        return charset;
    }

    public MessageBytes method() {
        return methodMB;
    }

    public void addPathParameter(String name, String value) {
        
    }

    public String getPathParameter(String name) {
        return null;
    }

    public ServerCookies getCookies() {
        return null;
    }

    public void setServerPort(int port) {
        
    }

    public String getHeader(String name) {
        return null;
    }

    public MimeHeaders getMimeHeaders() {
        return null;
    }

    public MessageBytes queryString() {
        return null;
    }

    public MessageBytes requestURI() {
        return null;
    }

    public Parameters getParameters() {
        return null;
    }

    public Object getAttribute(String name) {
        return null;
    }

    public long getContentLengthLong() {
        return 0;
    }

    public String getCharacterEncoding() {
        return null;
    }
}
