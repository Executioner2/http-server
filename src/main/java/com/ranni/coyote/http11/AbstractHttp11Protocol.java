package com.ranni.coyote.http11;

import com.ranni.coyote.AbstractProtocol;
import com.ranni.coyote.CompressionConfig;
import com.ranni.coyote.ContinueResponseTiming;
import com.ranni.coyote.Processor;
import com.ranni.util.net.AbstractEndpoint;

/**
 * Title: HttpServer
 * Description:
 * Http1.1协议。在这个类中定义了http1.1的一些约束，例如：请求头/响应头大小限制，
 * 请求体/响应体大小限制等。
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/11 16:02
 * @Ref org.apache.coyote.http11.AbstractHttp11Protocol
 */
public abstract class AbstractHttp11Protocol<S> extends AbstractProtocol<S> {

    // ==================================== 属性字段 ====================================
    
    private final CompressionConfig compressionConfig = new CompressionConfig();

    
    // ==================================== 构造方法 ====================================
    
    public AbstractHttp11Protocol(AbstractEndpoint<S, ?> endpoint) {
        super(endpoint);
        setConnectionTimeout(Constants.DEFAULT_CONNECTION_TIMEOUT);
        ConnectionHandler<S> handler = new ConnectionHandler<>(this);
        setHandler(handler);
        getEndpoint().setHandler(handler);
    }


    // ==================================== 基本方法 ====================================
    
    @Override
    protected String getProtocolName() {
        return "Http";
    }


    @Override
    protected Processor createProcessor() {
        Http11Processor processor = new Http11Processor(this, adapter);
        return processor;
    }


    // ==================================== 请求相关处理 ====================================

    /**
     * 100-continue（post请求）的处理意见
     */
    private ContinueResponseTiming continueResponseTiming = ContinueResponseTiming.IMMEDIATELY;
    public ContinueResponseTiming getContinueResponseTimingInternal() {
        return continueResponseTiming;
    }
    public String getContinueResponseTiming() {
        return continueResponseTiming.toString();
    }
    public void setContinueResponseTiming(String crt) {
        this.continueResponseTiming = ContinueResponseTiming.fromString(crt);
    }


    /**
     * 是否使用keep-alive响应头
     */
    private boolean useKeepAliveResponseHeader = true;
    public boolean isUseKeepAliveResponseHeader() {
        return useKeepAliveResponseHeader;
    }
    public void setUseKeepAliveResponseHeader(boolean useKeepAliveResponseHeader) {
        this.useKeepAliveResponseHeader = useKeepAliveResponseHeader;
    }


    private String relaxedPathChars = null;
    public String getRelaxedPathChars() {
        return relaxedPathChars;
    }
    public void setRelaxedPathChars(String relaxedPathChars) {
        this.relaxedPathChars = relaxedPathChars;
    }


    private String relaxedQueryChars = null;
    public String getRelaxedQueryChars() {
        return relaxedQueryChars;
    }
    public void setRelaxedQueryChars(String relaxedQueryChars) {
        this.relaxedQueryChars = relaxedQueryChars;
    }


    /**
     * 是否允许主机标头和请求行中指定主机不一致
     */
    private boolean allowHostHeaderMismatch = false;
    public boolean getAllowHostHeaderMismatch() {
        return allowHostHeaderMismatch;
    }
    public void setAllowHostHeaderMismatch(boolean allowHostHeaderMismatch) {
        this.allowHostHeaderMismatch = allowHostHeaderMismatch;
    }


    /**
     * 是否拒绝非法请求头。
     */
    private boolean rejectIllegalHeader = true;
    public boolean getRejectIllegalHeader() { return rejectIllegalHeader; }
    public void setRejectIllegalHeader(boolean rejectIllegalHeader) {
        this.rejectIllegalHeader = rejectIllegalHeader;
    }


    /**
     * post请求体最大大小
     */
    private int maxSavePostSize = 4 * 1024;
    public int getMaxSavePostSize() { return maxSavePostSize; }
    public void setMaxSavePostSize(int maxSavePostSize) {
        this.maxSavePostSize = maxSavePostSize;
    }


    /**
     * (请求/响应)头最大大小
     */
    private int maxHttpHeaderSize = 8 * 1024;
    public int getMaxHttpHeaderSize() { return maxHttpHeaderSize; }
    public void setMaxHttpHeaderSize(int valueI) { maxHttpHeaderSize = valueI; }

    
    /**
     * 请求头最大大小。-1为消息头通用大小 {@link #maxHttpHeaderSize}
     */
    private int maxHttpRequestHeaderSize = -1;
    public int getMaxHttpRequestHeaderSize() {
        return maxHttpRequestHeaderSize == -1 ? getMaxHttpHeaderSize() : maxHttpRequestHeaderSize;
    }
    public void setMaxHttpRequestHeaderSize(int valueI) {
        maxHttpRequestHeaderSize = valueI;
    }


    /**
     * 响应头最大大小。-1为消息头通用大小 {@link #maxHttpHeaderSize}
     */
    private int maxHttpResponseHeaderSize = -1;
    public int getMaxHttpResponseHeaderSize() {
        return maxHttpResponseHeaderSize == -1 ? getMaxHttpHeaderSize() : maxHttpResponseHeaderSize;
    }
    public void setMaxHttpResponseHeaderSize(int valueI) {
        maxHttpResponseHeaderSize = valueI;
    }


    /**
     * 连接上传超时
     */
    private int connectionUploadTimeout = 300000;
    public int getConnectionUploadTimeout() { return connectionUploadTimeout; }
    public void setConnectionUploadTimeout(int timeout) {
        connectionUploadTimeout = timeout;
    }


    /**
     * 禁用上传超时
     */
    private boolean disableUploadTimeout = true;
    public boolean getDisableUploadTimeout() { return disableUploadTimeout; }
    public void setDisableUploadTimeout(boolean isDisabled) {
        disableUploadTimeout = isDisabled;
    }


    /**
     * 字符串压缩
     */
    public void setCompression(String compression) {
        compressionConfig.setCompression(compression);
    }
    public String getCompression() {
        return compressionConfig.getCompression();
    }
    protected int getCompressionLevel() {
        return compressionConfig.getCompressionLevel();
    }


    /**
     * 服务器名
     */
    private String server;
    public String getServer() { return server; }
    public void setServer(String server) {
        this.server = server;
    }

    
    // ==================================== endpoint的方法 ====================================
    
    public boolean getUseSendfile() { return getEndpoint().getUseSendfile(); }
    public void setUseSendfile(boolean useSendfile) { getEndpoint().setUseSendfile(useSendfile); }

    public int getMaxKeepAliveRequests() {
        return getEndpoint().getMaxKeepAliveRequests();
    }
    public void setMaxKeepAliveRequests(int mkar) {
        getEndpoint().setMaxKeepAliveRequests(mkar);
    }


}
