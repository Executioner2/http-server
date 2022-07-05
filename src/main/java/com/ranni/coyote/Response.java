package com.ranni.coyote;

import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.http.parse.MediaType;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Title: HttpServer
 * Description:
 * 封装socket输出流，缓冲等一系列底层
 * 实现的类。
 * 并非HttpServletResponse实现
 *
 * TODO:
 * XXX - 异步servlet待实现 
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 20:07
 */
public final class Response {

    // ==================================== 基本属性字段 ====================================

    /**
     * 服务器默认语言环境
     */
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    /**
     * 响应状态码
     */
    int status = 200;

    /**
     * 响应状态消息
     */
    String message = null;

    /**
     * 复合响应头
     */
    final MimeHeaders headers = new MimeHeaders();

    /**
     * 输出缓冲区
     */
    OutputBuffer outputBuffer;

    /**
     * Notes
     */
    final Object notes[] = new Object[Constants.MAX_NOTES];

    /**
     * 响应头提交标志位
     */
    volatile boolean committed = false;

    /**
     * 钩子
     */
    volatile ActionHook hook;

    /* HTTP特殊标头 */
    String contentType = null;
    String contentLanguage = null;
    Charset charset = null;
    String characterEncoding = null;    
    long contentLength = -1;
    private Locale locale = DEFAULT_LOCALE;

    private Supplier<Map<String,String>> trailerFieldsSupplier = null;
    
    /**
     * 写入数据量
     */
    private long contentWritten = 0;

    /**
     * 提交时间
     */
    private long commitTime = -1;

    /**
     * 错误异常
     */
    private Exception errorException = null;

    /**
     * CoyoteRequest
     */
    Request coyoteRequest;
    
    /**
     * 错误处理状态
     *
     * <pre>
     * 状态机：
     *
     * 0 - NONE
     * 1 - NOT_REPORTED
     * 2 - REPORTED
     *
     *
     *   -->---->-- >NONE
     *   |   |        |
     *   |   |        | setError()
     *   ^   ^        |
     *   |   |       \|/
     *   |   |-<-NOT_REPORTED
     *   |            |
     *   ^            | report()
     *   |            |
     *   |           \|/
     *   |----<----REPORTED
     * </pre>
     */
    private final AtomicInteger errorState = new AtomicInteger(0);
    
    
    // ==================================== 核心方法 ====================================
    
    public Request getRequest() {
        return coyoteRequest;
    }
    
    public void setRequest(Request coyoteRequest) {
        this.coyoteRequest = coyoteRequest;
    }
    
    public void setOutputBuffer(OutputBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }
    
    public MimeHeaders getMimeHeaders() {
        return headers;
    }
    
    public void reset() throws IllegalStateException {
        if (committed) {
            throw new IllegalStateException();
        }
        recycle();
    }
    
    public void recycle() {
        contentType = null;
        contentLanguage = null;
        locale = DEFAULT_LOCALE;
        charset = null;
        characterEncoding = null;
        contentLength = -1;
        status = 200;
        message = null;
        committed = false;
        commitTime = -1;
        errorException = null;
        errorState.set(0);
        headers.clear();
        contentWritten = 0;
        trailerFieldsSupplier = null;
        
        // Servlet 3.1 异步写监听
//        listener = null;
//        synchronized (nonBlockingStateLock) {
//            fireListener = false;
//            registeredForWrite = false;
//        }
        
    }
    

    /**
     * 设置钩子对象
     * 
     * @param hook 钩子
     */
    protected void setHook(ActionHook hook) {
        this.hook = hook;
    }


    /**
     * 触发钩子事件
     *
     * @param code 事件码
     * @param param 携带参数
     */
    public void action(ActionCode code, Object param) {
        if (hook != null) {
            if (param == null) {
                hook.action(code, this);
            } else {
                hook.action(code, param);
            }
        }
    }

    
    public Supplier<Map<String, String>> getTrailerFields() {
        return trailerFieldsSupplier;
    }

    
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        AtomicBoolean trailerFieldsSupported = new AtomicBoolean(false);
        action(ActionCode.IS_TRAILER_FIELDS_SUPPORTED, trailerFieldsSupported);
        if (!trailerFieldsSupported.get()) {
            throw new IllegalStateException("response.noTrailers.notSupported");
        }

        this.trailerFieldsSupplier = supplier;
    }
    
    // ==================================== notes ====================================
    
    public final void setNote(int pos, Object value) {
        notes[pos] = value;
    }
    
    public final Object getNote(int pos) {
        return notes[pos];
    }
    

    // ==================================== Response方法 ====================================
    
    /**
     * @return 返回响应状态码
     */
    public int getStatus() {
        return status;
    }


    /**
     * 设置响应状态码
     * 
     * @param sc 状态码
     */
    public void setStatus(int sc) {
        this.status = sc;
    }


    /**
     * @return 返回响应状态消息
     */
    public String getMessage() {
        return message;
    }


    /**
     * 设置响应状态消息
     * 
     * @param message 响应状态消息
     */
    public void setMessage(String message) {
        this.message = message;
    }


    /**
     * @return 返回提交标志位
     */
    public boolean isCommitted() {
        return committed;
    }


    /**
     * 设置响应头提交状态。如果传入<b>true</b>，
     * 且当前<code>committed</code>为未提交
     * 状态，那么更新<code>committed</code>
     * 的值
     * 
     * @param b 响应头提交状态
     */
    public void setCommitted(boolean b) {
        if (b && !committed) {
            this.commitTime = System.currentTimeMillis();
        }
        this.committed = b;
    }


    /**
     * @return 返回提交时间
     */
    public long getCommitTime() {
        return commitTime;
    }


    /**
     * 设置错误异常
     * 
     * @param ex 异常
     */
    public void setErrorException(Exception ex) {
        errorException = ex;
    }


    /**
     * @return 返回处理请求过程中产生的异常
     */
    public Exception getErrorException() {
        return errorException;
    }


    /**
     * @return 如果返回<b>true</b>，则表示存在异常
     */
    public boolean isExceptionPresent() {
        return errorException != null;
    }


    /**
     * 设置错误报告状态。
     * 期望状态为0（没错误），设置为1（有错误未提交）
     * 
     * @return 如果返回<b>true</b>，则表示设置成功 
     */
    public boolean setError() {
        return errorState.compareAndSet(0, 1);
    }


    /**
     * @return 如果返回<b>true</b>，则表示有错误
     */
    public boolean isError() {
        return errorState.get() > 0;
    }
    

    /**
     * @return 如果返回<b>true</b>，则表示有错误需要报告
     */
    public boolean isErrorReportRequired() {
        return errorState.get() == 1;
    }


    /**
     * 设置错误报告状态。
     * 期望值为1（有错误未提交），设置为2（错误已提交）
     * 
     * @return 如果返回<b>true</b>，则表示设置成功
     */
    public boolean setErrorReported() {
        return errorState.compareAndSet(1, 2);
    }


    /**
     * 检查是否是特殊标头
     * 
     * @param name 标头名
     * @param value 标头值
     * @return 如果返回<b>true</b>，则表示是特殊标头
     */
    private boolean checkSpecialHeader(String name, String value) {
        if (name.equalsIgnoreCase("Content-Type")) {
            setContentType(value);
            return true;    
        }
        
        if (name.equalsIgnoreCase("Content-Length")) {
            try {
                long l = Long.parseLong(value);
                setContentLength(l);
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
        
        return false;
    }
    

    /**
     * 响应头中是否包含此标头
     * 
     * @param name 标头名
     * @return 如果返回<b>true</b>，则表示响应头中包含此标头
     */
    public boolean containsHeader(String name) {
        return headers.getHeader(name) != null;
    }
    
    
    /**
     * 设置标头
     * 
     * @param name 标头名
     * @param value 标头值
     */
    public void setHeader(String name, String value) {
        char c = name.charAt(0);
        if (c == 'c' || c == 'C') {
            if (checkSpecialHeader(name, value)) {
                return;
            }
        }
        headers.addValue(name).setString(value);
    }


    /**
     * 添加标头
     * 
     * @param name 标头名
     * @param value 标头值
     */
    public void addHeader(String name, String value) {
        addHeader(name, value, null);
    }


    /**
     * 添加标头
     * 
     * @param name 标头名
     * @param value 标头值
     * @param charset 编码方式
     */
    public void addHeader(String name, String value, Charset charset) {
        char c = name.charAt(0);
        if (c == 'c' || c == 'C') {
            if (checkSpecialHeader(name, value)) {
                return;
            }
        }

        MessageBytes mb = headers.addValue(name);
        if (charset != null) {
            mb.setCharset(charset);
        }
        mb.setString(value);
    }
    

    /**
     * 发送响应头
     */
    public void sendHeaders() {
        action(ActionCode.COMMIT, this);
        setCommitted(true);
    }


    /**
     * 设置语言环境
     * 
     * @param loc 语言环境
     */
    public void setLocale(Locale loc) {
        if (loc == null) {
            this.locale = null;
            this.contentLanguage = null;
            return;
        }
        
        this.locale = loc;
        this.contentLanguage = loc.toLanguageTag();
    }


    /**
     * @return 返回语言环境
     */
    public Locale getLocale() {
        return locale;
    }


    /**
     * @return 返回语言
     */
    public String getContentLanguage() {
        return contentLanguage;
    }
    

    /**
     * @return 返回响应体正文长度
     */
    public int getContentLength() {
        long cll = getContentLengthLong();
        
        if (cll < Integer.MAX_VALUE) {
            return (int) cll;
        }
        
        return -1;
    }
    
    
    /**
     * @return 返回响应体正文长度
     */
    public long getContentLengthLong() {
        return contentLength;
    }


    /**
     * 设置响应体正文长度
     * 
     * @param length 响应体正文长度
     */
    public void setContentLength(long length) {
        this.contentLength = length;
    }


    /**
     * @return 返回编码方式
     */
    public Charset getCharset() {
        return charset;
    }


    /**
     * @return 返回字符编码方式
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }
    

    /**
     * 设置编码方式
     * 
     * @param characterEncoding 编码方式
     * @throws UnsupportedEncodingException 可能抛出编码异常
     */
    public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException {
        if (isCommitted()) {
            return;
        }

        if (characterEncoding == null) {
            this.charset = null;
            this.characterEncoding = null;
            return;
        }
        
        this.characterEncoding = characterEncoding;
        this.charset = B2CConverter.getCharset(characterEncoding);
    }

    
    /**
     * 设置无指定编码方式的响应体类型
     * 
     * @param type 编码方式
     */
    public void setContentTypeNoCharset(String type) {
        this.contentType = type;
    }

    
    /**
     * 设置有指定编码方式的响应体类型
     * 
     * @param type 编码方式
     */
    public void setContentType(String type) {
        if (type == null) {
            this.contentType = null;
            return;
        }

        MediaType m = null;
        try {
            m = MediaType.parseMediaType(new StringReader(type));
        } catch (IOException e) {
            ;
        }

        if (m == null) {
            this.contentType = type;
            return;
        }
        
        this.contentType = m.toStringNoCharset();
        String charsetValue = m.getCharset();
        
        if (charsetValue == null) {
            this.contentType = type;
        } else {
            this.contentType = m.toStringNoCharset();
            charsetValue = charsetValue.trim();
            if (charsetValue.length() > 0) {
                try {
                    charset = B2CConverter.getCharset(charsetValue);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * @return 返回响应体正文类型
     */
    public String getContentType() {
        String ret = this.contentType;
        
        if (ret != null && charset != null) {
            ret = ret + ";charset=" + characterEncoding;
        }
        
        return ret;
    }


    // ==================================== 异步servlet ====================================

    /**
     * 写入数据到缓冲区
     *
     * @param buf 被写入的数据 
     * @throws IOException 可能抛出I/O异常
     */
    public void doWrite(ByteBuffer buf) throws IOException {
        int len = buf.remaining();
        outputBuffer.doWrite(buf);
        contentWritten += len - buf.remaining();
    }
    
    public boolean isReady() {
        return false;
    }

    public void setWriteListener(WriteListener listener) {
        
    }

    public WriteListener getWriteListener() {
        return null;
    }

    public void checkRegisterForWrite() {
    }


    /**
     * @return 返回写入到 {@link #outputBuffer} 
     *  中的数据量
     */
    public long getContentWritten() {
        return contentWritten;
    }

}
