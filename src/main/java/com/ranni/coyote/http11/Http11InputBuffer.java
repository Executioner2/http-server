package com.ranni.coyote.http11;

import com.ranni.connector.ApplicationBufferHandler;
import com.ranni.coyote.CloseNowException;
import com.ranni.coyote.InputBuffer;
import com.ranni.coyote.Request;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.HeaderUtil;
import com.ranni.util.http.HttpParser;
import com.ranni.util.http.MimeHeaders;
import com.ranni.util.net.SocketWrapperBase;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Title: HttpServer
 * Description:
 * 用于HTTP的请求输入处理。可以对请求行、请求头进行解析以及数据编码
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/30 11:15
 * @Ref org.apache.coyote.http11.Http11InputBuffer
 */
public class Http11InputBuffer implements InputBuffer, ApplicationBufferHandler {
    
    
    // ------------------------------ 属性字段 ------------------------------

    /**
     * 内部请求实例 
     */
    private final Request coyoteRequest;

    /**
     * 复合请求头
     */
    private final MimeHeaders headers;

    /**
     * 如果存在非法标头，是否抛出异常
     */
    private final boolean rejectIllegalHeader;
    

    /**
     * HTTP/2.0请求的序言
     */
    private static final byte[] CLIENT_PREFACE_START =
            "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

    /**
     * 请求行头的最大大小（包括空格空行）
     */
    private final int headerBufferSize;

    /**
     * 字节缓冲区
     */
    private ByteBuffer byteBuffer;

    /**
     * 可代表以下两种意义：</br>
     * <ul>
     *  <li>缓冲区中请求行结束的位置，请求头开始的位置</li>
     *  <li>缓冲区中请求头结束的位置，请求体开始的位置</li>
     * </ul>
     */
    private int end;

    /**
     * 输入流缓冲区
     */
    private InputBuffer inputStreamInputBuffer;

    /**
     * 输入过滤器
     */
    private InputFilter[] activeFilters;

    /**
     * 最后一个可执行的过滤器
     */
    private int lastActiveFilter;

    /**
     * 请求头解析状态
     */
    private volatile boolean parsingHeader;

    /**
     * why - 吞吐输入
     */
    private boolean swallowInput;


    /**
     * socket包装实例
     */
    private SocketWrapperBase<?> wrapper;


    /* ==================================== 解析相关参数 start ==================================== */

    /**
     * 上一个读取的字符
     */
    private byte prevChr = 0;

    /**
     * 当前读取的字符
     */
    private byte chr = 0;

    /**
     * 是否可以对请求行进行解析
     */
    private volatile boolean parsingRequestLine;

    /**
     * 请求行解析的各个阶段
     */
    private int parsingRequestLinePhase = 0;

    /**
     * 解析请求行是否到达边界
     */
    private boolean parsingRequestLineEol = false;

    /**
     * 请求行解析的第一个有效字符的位置
     */
    private int parsingRequestLineStart = 0;

    /**
     * URL后携带参数在数据缓冲区中的下标，如果为-1，表示URL后没有携带参数
     */
    private int parsingRequestLineQPos = -1;

    /**
     * 请求头解析的各个阶段
     */
    private HeaderParsePosition headerParsePos;

    /**
     * 请求头解析时的数据下标记录
     */
    private final HeaderParseData headerData = new HeaderParseData();

    /**
     * HTTP解析时的字符合法性判断
     */
    private final HttpParser httpParser;

    /* ==================================== 解析相关参数 end ==================================== */


    // ------------------------------ 构造方法 ------------------------------

    public Http11InputBuffer(Request request, int headerBufferSize,
                             boolean rejectIllegalHeader, HttpParser httpParser) {

        this.coyoteRequest = request;
        headers = request.getMimeHeaders();

        this.headerBufferSize = headerBufferSize;
        this.rejectIllegalHeader = rejectIllegalHeader;
        this.httpParser = httpParser;

        activeFilters = new InputFilter[0];
        lastActiveFilter = -1;

        parsingHeader = true;
        parsingRequestLine = true;
        parsingRequestLinePhase = 0;
        parsingRequestLineEol = false;
        parsingRequestLineStart = 0;
        parsingRequestLineQPos = -1;
        headerParsePos = HeaderParsePosition.HEADER_START;
        swallowInput = true;

        inputStreamInputBuffer = new SocketInputBuffer();
    }


    // ------------------------------ 内部类 ------------------------------

    private class SocketInputBuffer implements InputBuffer {

        @Override
        public int doRead(ApplicationBufferHandler handler) throws IOException {
            if (byteBuffer.position() >= byteBuffer.limit()) {
                // 缓冲区的数据已经都读走了，尝试取得新的数据
                boolean block = coyoteRequest.getReadListener() == null; // 是否为阻塞读
                if (!fill(block)) {
                    if (block) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
            
            int length = byteBuffer.remaining();
            
            // 拷贝到ApplicationBufferHandler中
            handler.setByteBuffer(byteBuffer.duplicate()); 
            byteBuffer.position(byteBuffer.limit());
            
            return length;
        }

        @Override
        public int available() {
            return byteBuffer.remaining();
        }
    }


    /**
     * 请求头解析数据记录
     */
    private static class HeaderParseData {
        /**
         * 一行的起始位置
         */
        int lineStart = 0;

        /**
         * 解析标头名时，标头名第一个字符的位置（跳过无效标头行）。</br>
         * 解析标头值时，':'之后第一个字符的位置。
         */
        int start = 0;

        /**
         * 解析标头值时，此参数才在读取了一个字符后自增一
         */
        int realPos = 0;

        /**
         * 跳过无效标题行时，最后一个非CR/LF的字符。</br>
         * 解析标头值时，最后一个非LWS的字符。
         */
        int lastSignificantChar = 0;

        /**
         * 标头值缓存
         */
        MessageBytes headerValue = null;
        
        
        public void recycle() {
            lineStart = 0;
            start = 0;
            realPos = 0;
            lastSignificantChar = 0;
            headerValue = null;
        }        
    }


    /**
     * 请求头行解析状态位置
     */
    private enum HeaderParsePosition {
        /**
         * 标头开始
         */
        HEADER_START,

        /**
         * 开始读取标头名。到':'之前不允许有空格，如果有空格，将导致整行被忽略
         */
        HEADER_NAME,

        /**
         * 请求头标头值开始位置。遇到标头值的一个有效字符前的空格都将忽略
         */
        HEADER_VALUE_START,

        /**
         * 开始读取标头值
         */
        HEADER_VALUE,

        /**
         * 读取新的标头之前的状态
         */
        HEADER_MULTI_LINE,

        /**
         * 忽略CRLF行
         */
        HEADER_SKIP_LINE
    }
    

    /**
     * 请求头解析状态
     */
    private enum HeaderParseStatus {
        /**
         * 解析完成
         */
        DONE,

        /**
         * 还有数据
         */
        HAVE_MORE_HEADERS,

        /**
         * 需要更多数据
         */
        NEED_MORE_DATA
    }
    
    
    // ------------------------------ 核心方法 ------------------------------


    void recycle() {
        wrapper = null;
        coyoteRequest.recycle();

        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        byteBuffer.limit(0).position(0);
        lastActiveFilter = -1;
        swallowInput = true;

        chr = 0;
        prevChr = 0;
        headerParsePos = HeaderParsePosition.HEADER_START;
        parsingRequestLinePhase = 0;
        parsingRequestLineEol = false;
        parsingRequestLineStart = 0;
        parsingRequestLineQPos = -1;
        headerData.recycle();
        // 回收volatile变量
        parsingRequestLine = true;
        parsingHeader = true;
    }
    
    
    void setSwallowInput(boolean swallowInput) {
        this.swallowInput = swallowInput;
    }


    /**
     * 重置指针，处理下个请求
     */
    void nextRequest() {
        coyoteRequest.recycle();
        
        if (byteBuffer.position() > 0) {
           if (byteBuffer.remaining() > 0) {
               // 还有剩余的数据，压缩（将剩余数据复制到开头）
               byteBuffer.compact();
               byteBuffer.flip();
           } else {
               // 没有数据了，重置指针
               byteBuffer.position(0).limit(0);
           }
        }
        
        // 重置过滤器
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }
        
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;

        headerParsePos = HeaderParsePosition.HEADER_START;
        parsingRequestLine = true;
        parsingRequestLinePhase = 0;
        parsingRequestLineEol = false;
        parsingRequestLineStart = 0;
        parsingRequestLineQPos = -1;
        headerData.recycle();
    }


    /**
     * 解析请求行
     * 
     * @param keptAlive 是否保持连接
     * @param connectionTimeout 连接超时时间
     * @param keepAliveTimeout 保持连接的最大时间
     * @return 如果返回<b>true</b>，表示解析请求行成功
     * @throws IOException 可能抛出I/O异常
     */
    boolean parseRequestLine(boolean keptAlive, int connectionTimeout, int keepAliveTimeout) throws IOException {
        if (!parsingRequestLine) {
            // 已经解析过了
            return true;
        }
        
        // 跳过空白行
        if (parsingRequestLinePhase < 2) {
            do {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    if (keptAlive) {
                        // 尚未读取数据且请求需要保持连接，设置保持连接的时间
                        wrapper.setReadTimeout(keepAliveTimeout);
                    }
                    
                    if (!fill(false)) {
                        // 读取挂起，不再处于初始状态
                        parsingRequestLinePhase = 1;
                        return false;
                    }
                    
                    // 已经接收到一个字节，切换到socket超时状态
                    wrapper.setReadTimeout(connectionTimeout);
                }
                 
                // 读取到了数据，判断是否是HTTP/2.0协议
                if (!keptAlive && byteBuffer.position() == 0
                   && byteBuffer.limit() >= CLIENT_PREFACE_START.length - 1) {
                     
                    boolean prefaceMath = true;
                    for (int i = 0; i < CLIENT_PREFACE_START.length && prefaceMath; i++) {
                        if (CLIENT_PREFACE_START[i] != byteBuffer.get(i)) {
                            prefaceMath = false;
                        }
                    }
                    
                    if (prefaceMath) {
                        // 匹配为HTTP/2.0协议
                        parsingRequestLinePhase = -1;
                        return false;
                    }
                }

                // 一旦开始读取数据就要开始设置开始时间
                if (coyoteRequest.getStartTime() < 0) {
                    coyoteRequest.setStartTime(System.currentTimeMillis());
                }
                chr = byteBuffer.get();
            } while (chr == Constants.CR || chr == Constants.LF);
            
            byteBuffer.position(byteBuffer.position() - 1); // 恢复多读取的那个字符
            parsingRequestLineStart = byteBuffer.position(); 
            parsingRequestLinePhase = 2; // 可以进入到第二个阶段       
        }

        // 解析请求方法名
        if (parsingRequestLinePhase == 2) {
            boolean space = false;
            
            while (!space) {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    // 莫得数据，尝试获取
                    if (!fill(false)) {
                        return false;
                    }
                }
                
                int pos = byteBuffer.position();
                chr = byteBuffer.get();
                
                // 按标准的HTTP请求，请求行的第一个字符不为空格，且空格之前的字符串代表着请求方法
                // 如果非标准格式，那就GG
                if (chr == Constants.SP || chr == Constants.HT) { 
                    space = true;
                    coyoteRequest.method().setBytes(byteBuffer.array(),
                            parsingRequestLineStart, pos - parsingRequestLineStart);
                } else if (!HttpParser.isToken(chr)) { // 不是合法的请求方法字符
                    // 避免因为没有解析设置请求协议导致响应发不出去，设置协议为HTTP/1.1
                    coyoteRequest.protocol().setString(Constants.HTTP_11);
                    String s = parseInvalid(parsingRequestLineStart, byteBuffer);
                    throw new IllegalArgumentException("Http11InputBuffer.parseRequestLine 请求方法错误！ " + s);
                }                
            } // while end
            
            parsingRequestLinePhase = 3; // 可以进入到第三个阶段
        }
        
        // 消除请求方法与URL之间的空格
        if (parsingRequestLinePhase == 3) {
            boolean space = true;
            while (space) {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    if (!fill(false)) {
                        return false;
                    }
                }
                chr = byteBuffer.get();
                if (!(chr == Constants.SP || chr == Constants.HT)) {
                    space = false;
                    byteBuffer.position(byteBuffer.position() - 1); // 恢复多读的一个
                }
            } // while end
            
            parsingRequestLineStart = byteBuffer.position();
            parsingRequestLinePhase = 4; // 可以进入到第四个阶段
        }
        
        // 解析URL
        if (parsingRequestLinePhase == 4) {
            int end = 0;
            boolean space = false;
            
            while (!space) {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    if (!fill(false)) {
                        return false;
                    }
                } 
                
                int pos = byteBuffer.position();
                prevChr = chr;
                chr = byteBuffer.get();
                if (prevChr == Constants.CR && chr != Constants.LF) {
                    // 有'\r'没'\n'，直接算格式错误抛出异常
                    coyoteRequest.protocol().setString(Constants.HTTP_11);
                    String invalidRequestTarget = parseInvalid(parsingRequestLineStart, byteBuffer);
                    throw new IllegalArgumentException("Http11InputBuffer.parseRequestLine URL格式错误！ " + invalidRequestTarget);
                }
                
                if (chr == Constants.SP || chr == Constants.HT) {
                     space = true;
                     end = pos;
                } else if (chr == Constants.CR) {
                    // HTTP/0.9格式，CR可选，LF必选
                } else if (chr == Constants.LF) {
                    // HTTP/0.9格式，结束循环
                    space = true;
                    // HTTP/0.9的请求行中不包含协议版本
                    coyoteRequest.protocol().setString("");
                    // 跳过5、6阶段
                    parsingRequestLinePhase = 7;
                    if (prevChr == Constants.CR) {
                        end = pos - 1;
                    } else {
                        end = pos;
                    }
                } else if (chr == Constants.QUESTION && parsingRequestLineQPos == -1) {
                    // URL后携带了参数
                    parsingRequestLineQPos = pos;
                } else if (parsingRequestLineQPos != -1 && !httpParser.isQueryRelaxed(chr)) {
                    // URL后携带的参数部分包含非法的字符
                    coyoteRequest.protocol().setString(Constants.HTTP_11);
                    String invalidRequestTarget = parseInvalid(parsingRequestLineStart, byteBuffer);
                    throw new IllegalArgumentException("Http11InputBuffer.parseRequestLine URL后参数部分包含非法字符！ " + invalidRequestTarget);
                } else if (httpParser.isNotRequestTargetRelaxed(chr)) {
                    // URL中包含非法的字符
                    coyoteRequest.protocol().setString(Constants.HTTP_11);
                    String invalidRequestTarget = parseInvalid(parsingRequestLineStart, byteBuffer);
                    throw new IllegalArgumentException("Http11InputBuffer.parseRequestLine URL中包含非法字符！ " + invalidRequestTarget);
                }
                
            } // while end
            
            if (parsingRequestLineQPos >= 0) { // 有参数
                // URL后的参数部分
                coyoteRequest.queryString().setBytes(byteBuffer.array(), 
                        parsingRequestLineQPos + 1, end - parsingRequestLineQPos - 1);
                // URL部分
                coyoteRequest.requestURI().setBytes(byteBuffer.array(),
                            parsingRequestLineStart, parsingRequestLineQPos - parsingRequestLineStart);
            
            } else { // 没有参数 
                coyoteRequest.requestURI().setBytes(byteBuffer.array(),
                        parsingRequestLineStart, end - parsingRequestLineStart);
            }
            
            // 不覆盖HTTP/0.9跳过5、6阶段的情况
            if (parsingRequestLinePhase == 4) {
                parsingRequestLinePhase = 5;
            }
        }
        
        // 消除URL和协议版本之间的空格
        if (parsingRequestLinePhase == 5) {
            boolean space = true;
            while (space) {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    if (!fill(false)) {
                        return false;
                    }
                }
                chr = byteBuffer.get();
                if (!(chr == Constants.SP || chr == Constants.HT)) {
                    space = false;
                    byteBuffer.position(byteBuffer.position() - 1); // 恢复多读的一个
                }
            } // while end

            parsingRequestLineStart = byteBuffer.position();
            parsingRequestLinePhase = 6; // 可以进入到第六个阶段
            end = 0; // why - 标记缓冲区位置
        }
        
        // 解析协议版本
        if (parsingRequestLinePhase == 6) {
            while (!parsingRequestLineEol) {
                if (byteBuffer.position() >= byteBuffer.limit()) {
                    if (!fill(false)) {
                        return false;
                    }
                }
                
                int pos = byteBuffer.position();
                prevChr = chr;
                chr = byteBuffer.get();
                if (chr == Constants.CR) {
                    // 请求行可能结束，需要下一个字符是LF，否则抛出异常
                } else if (prevChr == Constants.CR && chr == Constants.LF) {
                    end = pos - 1; // 减去多出的那个CR（pos记录的是chr前一个字符的下标）
                    parsingRequestLineEol = true;
                } else if (chr == Constants.LF) {
                    // 遇到LF可以直接跳出循环
                    end = pos;
                    parsingRequestLineEol = true;
                } else if (prevChr == Constants.CR || !HttpParser.isHttpProtocol(chr)) {
                    String invalidProtocol = parseInvalid(parsingRequestLineStart, byteBuffer);
                    throw new IllegalArgumentException("Http11InputBuffer.parseRequestLine 非法EOF或者字符为非法的协议版本字符！ " + invalidProtocol);
                }
            } // while end
            
            if (end - parsingRequestLineStart > 0) {
                coyoteRequest.protocol().setBytes(byteBuffer.array(), parsingRequestLineStart,
                            end - parsingRequestLineStart);
                parsingRequestLinePhase = 7;
            }
            // 如果协议没有找到，将跳过七阶段并抛出异常
        }

        // 解析完成，清空这次的解析记录属性
        if (parsingRequestLinePhase == 7) {
            parsingRequestLine = false;
            parsingRequestLinePhase = 0;
            parsingRequestLineEol = false;
            parsingRequestLineStart = 0;
            return true;
        }
        
        throw new IllegalStateException("Http11InputBuffer.parseRequestLine  解析失败！ " + parsingRequestLinePhase);
    }


    /**
     * 解析请求头全部数据
     * 
     * @return 如果返回<b>true</b>，则表示解析成功
     * @throws IOException 可能抛出I/O异常
     */
    boolean parseHeaders() throws IOException {
        if (!parsingHeader) {
            throw new IllegalStateException("Http11InputBuffer.parseHeaders  请求头已经解析完了！");
        }

        HeaderParseStatus status;
        
        do {
            status = parseHeader();
            
            // 请求头超出限制的大小
            if (byteBuffer.position() > headerBufferSize 
                || byteBuffer.capacity() < byteBuffer.position()) {
                throw new IllegalArgumentException("Http11InputBuffer.parseHeaders  请求头太长！");
            }
        } while (status == HeaderParseStatus.HAVE_MORE_HEADERS); // 还有数据
        
        if (status == HeaderParseStatus.DONE) {
            // 解析完成
            parsingHeader = false;
            end = byteBuffer.position();
            return true;
        } else {
            // 还差数据
            return false;
        }
        
    }


    /**
     * 解析请求头一个标头的数据
     * 
     * @return 返回请求头解析状态 {@link HeaderParseStatus}
     * @throws IOException 可能抛出I/O异常
     */
    private HeaderParseStatus parseHeader() throws IOException {
        
        // 开始解析标头，检查是否是空白行（请求头与请求体之间做分隔的空白块）
        while (headerParsePos == HeaderParsePosition.HEADER_START) {
            if (byteBuffer.position() >= byteBuffer.limit()) {
                if (!fill(false)) {
                    return HeaderParseStatus.NEED_MORE_DATA;
                }
            }
            
            prevChr = chr;
            chr = byteBuffer.get();
            
            if (chr == Constants.CR && prevChr != Constants.CR) {
                // 当前字符是'\r'，请求头可能要结束了
            } else if (chr == Constants.LF) {
                // 遇到'\n'直接完成请求头的解析
                return HeaderParseStatus.DONE;
            } else {
                if (prevChr == Constants.CR) {
                    // prev是'\r'且chr不是'\n'，prev和chr视为有效字符，退回读取的这两个字符
                    byteBuffer.position(byteBuffer.position() - 2);
                } else {
                    // chr不是'\n'，退回读取的这个chr
                    byteBuffer.position(byteBuffer.position() - 1);
                }
                break;
            }
        } // while end
        
        if (headerParsePos == HeaderParsePosition.HEADER_START) {
            headerData.start = byteBuffer.position();
            headerData.lineStart = headerData.start;
            headerParsePos = HeaderParsePosition.HEADER_NAME; // 应该开始解析标头名了
        }
        
        // 开始解析标头名。总是使用ASCII编码方式
        while (headerParsePos == HeaderParsePosition.HEADER_NAME) {
            if (byteBuffer.position() >= byteBuffer.limit()) {
                if (!fill(false)) {
                    return HeaderParseStatus.NEED_MORE_DATA;
                }
            }
            
            int pos = byteBuffer.position();
            chr = byteBuffer.get();
            if (chr == Constants.COLON) {
                // 结束标头名读取
                headerParsePos = HeaderParsePosition.HEADER_VALUE_START;
                headerData.headerValue = headers.addValue(byteBuffer.array(),
                        headerData.start, pos - headerData.start);
                pos = byteBuffer.position(); // ':'之后一个字符的位置
                headerData.start = pos;
                headerData.realPos = pos;
                headerData.lastSignificantChar = pos;
                break;
            } else if (!HttpParser.isToken(chr)) {
                // 不是合法的标头值字符，跳过这一行
                headerData.lastSignificantChar = pos;
                byteBuffer.position(byteBuffer.position() - 1); // 退回这个不合法的字符
                return skipLine();
            }
            
            // 转小写
            if (chr >= Constants.A && chr <= Constants.Z) {
                byteBuffer.put(pos, (byte) (chr - Constants.LC_OFFSET));
            }
        }
        
        // why - 跳过该行
        if (headerParsePos == HeaderParsePosition.HEADER_SKIP_LINE) {
            return skipLine();
        }
        
        

        headerData.recycle();
        return HeaderParseStatus.HAVE_MORE_HEADERS;
    }


    /**
     * 跳过请求头的一个标头
     * 
     * @return 返回请求头解析状态 {@link HeaderParseStatus}
     * @throws IOException 可能抛出I/O异常
     */
    private HeaderParseStatus skipLine() throws IOException {
        headerParsePos = HeaderParsePosition.HEADER_SKIP_LINE;
        boolean eol = false;
        
        while (!eol) {
            if (byteBuffer.position() >= byteBuffer.limit()) {
                if (!fill(false)) {
                    return HeaderParseStatus.NEED_MORE_DATA;
                }
            }
            
            int pos = byteBuffer.position();
            prevChr = chr;
            chr = byteBuffer.get();
            if (chr == Constants.CR) {
                // 可能要结束一个标头
            } else if (chr == Constants.LF) {
                eol = true;
            } else {
                headerData.lastSignificantChar = pos;
            }
        } // while end
        
        // 是否对非法的标头抛出异常
        if (rejectIllegalHeader) {
            String  message = HeaderUtil.toPrintableString(byteBuffer.array(), headerData.lineStart,
                    headerData.lastSignificantChar - headerData.lineStart + 1);
            throw new IllegalArgumentException("非法标头！ " + message);
        }
        
        headerParsePos = HeaderParsePosition.HEADER_START;
        return HeaderParseStatus.HAVE_MORE_HEADERS;
    }


    private String parseInvalid(int startPos, ByteBuffer byteBuffer) {
        byte b = 0;
        while (byteBuffer.hasRemaining() && b != Constants.SP) {
            b = byteBuffer.get();
        }
        String result = HeaderUtil.toPrintableString(byteBuffer.array(), byteBuffer.arrayOffset() + startPos, byteBuffer.position() - startPos);
        if (b != Constants.SP) {
            result = result + "...";
        }
        return result;
    }


    /**
     * 初始化
     * 
     * @param socketWrapper socket的包装类
     */
    void init(SocketWrapperBase<?> socketWrapper) {
        wrapper = socketWrapper;
        wrapper.setAppReadBufHandler(this);
        
        int bufLength = headerBufferSize + wrapper.getSocketBufferHandler().getReadBuffer().capacity();
        
        if (byteBuffer == null || byteBuffer.capacity() < bufLength) {
            byteBuffer = ByteBuffer.allocate(bufLength);
            byteBuffer.position(0).limit(0);
        }
    }
    

    /**
     * 尝试将数据读入到缓冲区中
     * 
     * @param block 是否阻塞读
     * @return 如果返回<b>true</b>，则表示成功读取到数据
     * @throws IOException 可能抛出I/O异常
     */
    private boolean fill(boolean block) throws IOException {
        if (parsingHeader) {
            if (byteBuffer.limit() >= headerBufferSize) {
                // 避免未知协议错误
                coyoteRequest.protocol().setString(Constants.HTTP_11);
            }
            throw new IllegalArgumentException("请求头太大！");
        } else {
            byteBuffer.limit(end).position(end);
        }
        
        int nRead = -1;
        int mark = byteBuffer.position();
        
        try {
            if (byteBuffer.position() < byteBuffer.limit()) {
                // position到limit之间存在未读取数据
                byteBuffer.position(byteBuffer.limit());
            }
            byteBuffer.limit(byteBuffer.capacity());
            SocketWrapperBase<?> socketWrapper = this.wrapper;

            if (socketWrapper != null) {
                nRead = socketWrapper.read(block, byteBuffer);
            } else {
                throw new CloseNowException("Http11InputBuffer.fill 边界错误！");
            }
        } finally {
            if (byteBuffer.position() >= mark) {
                // 读取到了数据
                byteBuffer.limit(byteBuffer.position());
                byteBuffer.position(mark);
            } else {
                // 没有读取到数据
                byteBuffer.position(0);
                byteBuffer.limit(0);
            }
        }
        
        if (nRead > 0) {
            return true;
        } else if (nRead == -1) {
            throw new EOFException("Http11InputBuffer.fill 边界错误！");
        } else {
            return false;
        }
    }
    
    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return null;
    }

    @Override
    public void expand(int size) {

    }

    @Override
    public int doRead(ApplicationBufferHandler handler) throws IOException {
        return 0;
    }

    @Override
    public int available() {
        return 0;
    }
}
