package com.ranni.coyote.http11;

import com.ranni.coyote.*;
import com.ranni.coyote.http11.filters.*;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.FastHttpDateFormat;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.http.parse.HttpParser;
import com.ranni.util.http.parse.TokenList;
import com.ranni.util.net.AbstractEndpoint.Handler.SocketState;
import com.ranni.util.net.SendfileDataBase;
import com.ranni.util.net.SocketWrapperBase;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Title: HttpServer
 * Description:
 * 适用于Http/1.1的处理器，此实例在对请求做过部分处理后会把请求交付给容器
 * 
 * TODO:
 * XXX - 暂不支持文件发送
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 20:06
 * @Ref org.apache.coyote.http11.Http11Processor
 */
public class Http11Processor extends AbstractProcessor {

    // ==================================== 基本属性字段 ====================================

    /**
     * http/1.1协议实例
     */
    private final AbstractHttp11Protocol<?> protocol;

    /**
     * 适用于http/1.1的输入缓冲处理
     */
    private final Http11InputBuffer inputBuffer;

    /**
     * 适用于http/1.1的输出缓冲处理
     */
    private final Http11OutputBuffer outputBuffer;

    /**
     * Http解析器
     */
    private final HttpParser httpParser;

    /**
     * why - 如果为false，连接将在请求结束后关闭连接
     */
    private boolean contentDelimitation = true;

    /**
     * http/0.9标志位
     */
    private boolean http09 = false;

    /**
     * HTTP/1.1标志位
     */
    private boolean http11 = true;

    /**
     * socket打开标志位。
     */
    private volatile boolean openSocket = false;

    /**
     * Keep-Alive
     */
    private volatile boolean keepAlive = true;

    /**
     * 缓冲区过滤器数量
     */
    private int pluggableFilterIndex = Integer.MAX_VALUE;
    
    /**
     * 发送的文件数据
     */
    private SendfileDataBase sendfileData = null;


    // ==================================== 构造方法 ====================================
    
    public Http11Processor(AbstractHttp11Protocol<?> protocol, Adapter adapter) {
        super(adapter);
        this.protocol = protocol;
    
        httpParser = new HttpParser(protocol.getRelaxedPathChars(), protocol.getRelaxedQueryChars());
        
        inputBuffer = new Http11InputBuffer(request, protocol.getMaxHttpRequestHeaderSize(), protocol.getRejectIllegalHeader(), httpParser);
        request.setInputBuffer(inputBuffer);
        
        outputBuffer = new Http11OutputBuffer(response, protocol.getMaxHttpResponseHeaderSize());
        response.setOutputBuffer(outputBuffer);
        
        // input/output 缓冲区过滤阀。有严格的顺序要求
        // 创建并添加基本过滤阀（并非身份认证阀，因为没有身份认证的代码，仅仅是普通的将数据写入到信道的缓冲区中）
        inputBuffer.addFilter(new IdentityInputFilter(protocol.getMaxSwallowSize()));
        outputBuffer.addFilter(new IdentityOutputFilter());

        // 创建并添加分块过滤阀
        inputBuffer.addFilter(new ChunkedInputFilter(protocol.getMaxTrailerSize(),
                protocol.getAllowedTrailerHeadersInternal(), protocol.getMaxExtensionSize(),
                protocol.getMaxSwallowSize()));
        outputBuffer.addFilter(new ChunkedOutputFilter());

        // 创建并添加空请求/响应正文阀
        inputBuffer.addFilter(new VoidInputFilter());
        outputBuffer.addFilter(new VoidOutputFilter());

        // 创建并添加输入缓冲区阀
        inputBuffer.addFilter(new BufferedInputFilter());

        // 创建并添加gzip数据压缩过滤阀
        outputBuffer.addFilter(new GzipOutputFilter());

        pluggableFilterIndex = inputBuffer.getFilters().length;
    }


    // ==================================== 核心方法 ====================================
    
    /**
     * 是否是需要断开连接的状态码
     * 
     * @param status 状态码
     * @return 如果返回<b>true</b>，则表示此状态码应该断开与客户端的会话连接
     */
    private static boolean statusDropsConnection(int status) {
        return status == 400 /* SC_BAD_REQUEST */ ||
               status == 408 /* SC_REQUEST_TIMEOUT */ ||
               status == 411 /* SC_LENGTH_REQUIRED */ ||
               status == 413 /* SC_REQUEST_ENTITY_TOO_LARGE */ ||
               status == 414 /* SC_REQUEST_URI_TOO_LONG */ ||
               status == 500 /* SC_INTERNAL_SERVER_ERROR */ ||
               status == 503 /* SC_SERVICE_UNAVAILABLE */ ||
               status == 501 /* SC_NOT_IMPLEMENTED */;
    }


    /**
     * 处理请求的方法（究极核心的方法），会调用适配器然后由
     * 适配器交付给对应的容器。
     * 
     * @param socketWrapper 要处理的socket包装实例
     * @return 返回socket状态
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    public SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException {
        RequestInfo ri = request.getRequestProcessor();
        ri.setStage(com.ranni.coyote.Constants.STAGE_PARSE); // 切换到请求解析状态
        
        setSocketWrapper(socketWrapper);
        
        return null;
    }


    /**
     * 设置socket映射实例
     * 
     * @param socketWrapper socket映射实例
     */
    @Override
    protected void setSocketWrapper(SocketWrapperBase<?> socketWrapper) {
        super.setSocketWrapper(socketWrapper);
        inputBuffer.init(socketWrapper);
        outputBuffer.init(socketWrapper);
    }


    /**
     * 响应前准备
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected final void prepareResponse() throws IOException {
        boolean entityBody = true;
        contentDelimitation = false;

        OutputFilter[] outputFilters = outputBuffer.getFilters();

        if (http09) {
            // http/0.9协议，直接普通的写入，然后提交即可
            outputBuffer.addActiveFilter(outputFilters[Constants.IDENTITY_FILTER]);
            outputBuffer.commit();
            return;
        }

        int status = response.getStatus();
        
        if (status < 200 || status == 204 
            || status == 205 || status == 304) {
            // 响应体无内容的状态码。添加VOID_FILTER过滤器，过滤掉往响应体添加的数据
            outputBuffer.addActiveFilter(outputFilters[Constants.VOID_FILTER]);
            entityBody = false;
            contentDelimitation = true;
            if (status == 205) {
                response.setContentLength(0);
            } else {
                response.setContentLength(-1);
            }
        }

        MessageBytes method = request.method();
        if (method.equals("HEAD")) {
            outputBuffer.addActiveFilter(outputFilters[Constants.VOID_FILTER]);
            contentDelimitation = true;
        }
        
        if (protocol.getUseSendfile()) {
            prepareSendfile(outputFilters);
        }
        
        // 响应体内容是否使用压缩
        boolean useCompression = false;
        if (entityBody && sendfileData == null) {
            useCompression = protocol.useCompression(request, response);
        }

        // 响应体可以有内容或响应状态码为204，响应头应
        // 有Content-Type和Content-Language标头
        MimeHeaders headers = response.getMimeHeaders();
        if (entityBody || status == HttpServletResponse.SC_NO_CONTENT) {
            String contentType = response.getContentType();
            if (contentType != null) {
                headers.setValue("Content-Type").setString(contentType);
            }
            String language = response.getContentLanguage();
            if (language != null) {
                headers.setValue("Content-Language").setString(language);
            }
        }

        // 是否存在 "connection: close" 标头
        boolean connectionClosePresent = isConnectionToken(headers, Constants.CLOSE);
        long contentLength = response.getContentLengthLong();
        
        if (http11 && response.getTrailerFields() != null) {
            // 分块发送。用于发送大数据或未知长度的数据。例子：
            
            // HTTP/1.1 200 OK 
            // Content-Type: text/plain 
            // Transfer-Encoding: chunked
            // 
            // 7\r\n
            // Mozilla\r\n 
            // 9\r\n
            // Developer\r\n
            // 7\r\n
            // Network\r\n
            // 0\r\n 
            // \r\n
            
            outputBuffer.addActiveFilter(outputFilters[Constants.CHUNKED_FILTER]);
            contentDelimitation = true;
            headers.addValue(Constants.TRANSFERENCODING).setString(Constants.CHUNKED);
        } else if (contentLength == -1){
            // 正常写入数据，添加普通数据写入阀
            headers.setValue("Content-Length").setLong(contentLength);
            outputBuffer.addActiveFilter(outputFilters[Constants.IDENTITY_FILTER]);
            contentDelimitation = true;
        } else {
            if (http11 && entityBody && !connectionClosePresent) {
                // 没有指定内容长度且正文有内容，而且不用响应此请
                // 求后就关闭当前连接。满足上述情况进入到当前代码
                // 块。表示则需要分块。添加分块过滤器
                outputBuffer.addActiveFilter(outputFilters[Constants.CHUNKED_FILTER]);
                contentDelimitation = true;
                headers.addValue(Constants.TRANSFERENCODING).setString(Constants.CHUNKED);
            } else {
                // 正文没有内容或不是http/1.1协议，那么正常写入
                outputBuffer.addActiveFilter(outputFilters[Constants.IDENTITY_FILTER]);
            }
        }
        
        // 数据需要压缩，添加GZIP压缩阀
        if (useCompression) {
            outputBuffer.addActiveFilter(outputFilters[Constants.GZIP_FILTER]);
        }
        
        // 添加响应日期
        if (headers.getValue("Date") == null) {
            headers.addValue("Date").setString(
                    FastHttpDateFormat.getCurrentDate());
        }
        
        if ((entityBody && !contentDelimitation) || connectionClosePresent) {
            // 1、存在响应正文，且能确定发送的大小（分块也是能确定大小的，当最后一块长度为0则表示数据发完了）
            // 2、有 "connection: close" 标头
            // 当出现以上两种情况时，需要在响应完成后关闭本次连接
            keepAlive = false;
        }
        
        // 设置connection标头为keep-alive的情况
        if (!keepAlive) {
            // 如果keeAlive为false，且没有指定connection为close
            // 那么就需要在这里将connection设为close
            if (!connectionClosePresent) {
                headers.addValue(Constants.CONNECTION).setString(Constants.CLOSE);
            }
        } else if (!getErrorState().isError()){
            // 此次请求/响应没有错误
            if (!http11) {
                // 非http/1.1。直接设置connection值为keep-alive
                headers.addValue(Constants.CONNECTION).setString(Constants.KEEP_ALIVE_HEADER_VALUE_TOKEN);
            }
            
            if (protocol.getUseKeepAliveResponseHeader()) {
                boolean connectionKeepAlivePresent = isConnectionToken(request.getMimeHeaders(), Constants.KEEP_ALIVE_HEADER_VALUE_TOKEN);
                
                if (connectionKeepAlivePresent) {
                    int keepAliveTimeout = protocol.getKeepAliveTimeout();
                    
                    if (keepAliveTimeout > 0) {
                        // 设置保持连接的时长
                        String v = "timeout=" + keepAliveTimeout / 1000L;
                        headers.setValue(Constants.KEEP_ALIVE_HEADER_NAME).setString(v);
                        
                        if (http11) {
                            MessageBytes connectionHeaderValue = headers.getValue(Constants.CONNECTION);
                            if (connectionHeaderValue == null) {
                                // 不存在，还没有connection这个标头，直接添加并设置此标头的值为keep-alive
                                headers.addValue(Constants.CONNECTION).setString(Constants.KEEP_ALIVE_HEADER_VALUE_TOKEN);
                            } else {
                                // 有多个值，将keep-alive这个值追加到旧值后面，用','分隔。
                                connectionHeaderValue.setString(connectionHeaderValue.getString() + ", " + Constants.KEEP_ALIVE_HEADER_VALUE_TOKEN);
                            }
                        }
                    }
                }
            }

            // 添加server标头
            String server = protocol.getServer();
            if (server == null) {
                if (protocol.getServerRemoveAppProvidedValues()) {
                    headers.removeHeader("server");
                }
            } else {
                headers.setValue("Server").setString(server);   
            }
            
            // 发送状态行以及写入响应头
            try {
                outputBuffer.sendStatus();
                int size = headers.size();
                for (int i = 0; i < size; i++) {
                    outputBuffer.sendHeader(headers.getName(i), headers.getValue(i));
                }
                outputBuffer.endHeader();
            } catch (Throwable t) {
                outputBuffer.resetHeaderBuffer();
                throw t;
            }
            
            // 提交响应头
            outputBuffer.commit();
        }
    }
    

    /**
     * 判断connection标头的值是否为close
     * 
     * @return 如果返回<b>true</b>，则表示connection值为close
     */
    private static boolean isConnectionToken(MimeHeaders headers, String token) throws IOException {
        MessageBytes connection = headers.getValue(Constants.CONNECTION);
        if (connection == null) {
            return false;
        }

        Set<String> tokens = new HashSet<>();
        TokenList.parseTokenList(headers.values(Constants.CONNECTION), tokens);
        return tokens.contains(token);
    }
    

    /**
     * 为发送文件做准备。创建发送的文件实例，发
     * 送文件需要过滤掉在响应正文中写入的数据。
     * 
     * @param outputFilters 过滤器库
     */
    private void prepareSendfile(OutputFilter[] outputFilters) {
        String fileName = (String) request.getAttribute(com.ranni.coyote.Constants.SENDFILE_FILE_END_ATTR);
        if (fileName == null) {
            sendfileData = null;
        } else {
            outputBuffer.addActiveFilter(outputFilters[Constants.VOID_FILTER]);
            contentDelimitation = true;
            long pos = (long) request.getAttribute(com.ranni.coyote.Constants.SENDFILE_FILE_START_ATTR);
            long end = (long) request.getAttribute(com.ranni.coyote.Constants.SENDFILE_FILE_END_ATTR);
            sendfileData = socketWrapper.createSendfileData(fileName, pos, end);
        }
    }


    /**
     * 完成响应
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected void finishResponse() throws IOException {
        outputBuffer.end();
    }


    /**
     * 对于100-continue的处理态度
     * 
     * @param continueResponseTiming 100-continue的处理态度
     */
    @Override
    protected void ack(ContinueResponseTiming continueResponseTiming) {
        if (continueResponseTiming == ContinueResponseTiming.ALWAYS
            || continueResponseTiming == protocol.getContinueResponseTimingInternal()) {
            
            if (!response.isCommitted() && request.hasExpectation()) {
                inputBuffer.setSwallowInput(true);
                try {
                    outputBuffer.sendAck();
                } catch (IOException e) {
                    setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
                }
            }
        }
    }

    /**
     * 刷新输出缓冲区
     * 
     * @throws IOException 可能抛出I/O异常
     */
    @Override
    protected void flush() throws IOException {
        outputBuffer.flush();
    }


    /**
     * 输入缓冲区可读取数据量
     * 
     * @param doRead 如果为<b>true</b>，则表示允许在没有数据的情况下非阻塞读取数据到输入缓冲区
     * @return 返回输入缓冲区可读数据量
     */
    @Override
    protected int available(boolean doRead) {
        return inputBuffer.available(doRead);
    }


    /**
     * TODO - 设置请求体数据到请求实例的输入缓冲区中
     * 
     * @param body 请求体数据
     */
    @Override
    protected void setRequestBody(ByteChunk body) {

    }

    @Override
    protected void setSwallowResponse() {

    }

    @Override
    protected void disableSwallowRequest() {

    }

    @Override
    protected boolean isRequestBodyFullyRead() {
        return false;
    }

    @Override
    protected void registerReadInterest() {

    }

    @Override
    protected boolean isReadyForWrite() {
        return false;
    }

    
}
