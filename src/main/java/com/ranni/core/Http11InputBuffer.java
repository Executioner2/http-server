package com.ranni.core;

import com.ranni.connector.ApplicationBufferHandler;
import com.ranni.coyote.InputBuffer;
import com.ranni.coyote.Request;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.HttpParser;
import com.ranni.util.http.MimeHeaders;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Title: HttpServer
 * Description:
 * 用于HTTP的请求输入处理。可以对请求头进行解析以及数据编码
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/30 11:15
 * @Ref org.apache.coyote.http11.Http11InputBuffer
 */
public class Http11InputBuffer implements InputBuffer, ApplicationBufferHandler {
    
    
    // ------------------------------ 属性字段 ------------------------------
    
    private final Request coyoteRequest;
    private final MimeHeaders headers;
    private final boolean rejectIllegalHeader;

    /**
     * 请求行头的最大大小（包括空格空行）
     */
    private final int headerBufferSize;

    /**
     * 字节缓冲区
     */
    private ByteBuffer byteBuffer;

    /**
     * 缓冲区中请求头结束的位置，请求体开始的位置
     */
    private int end;

    /**
     * 输入流缓冲区
     */
    private InputBuffer inputStreamInputBuffer;

    /**
     * 输入过滤器
     */
    private InputFilter[] inputFilters;

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
     * 解析相关参数
     */
    private byte prevChr = 0;
    private byte chr = 0;
    private volatile boolean parsingRequestLine;
    private int parsingRequestLinePhase = 0;
    private boolean parsingRequestLineEol = false;
    private int parsingRequestLineStart = 0;
    private int parsingRequestLineQPos = -1;
    private HeaderParsePosition headerParsePos;
    private final HeaderParseData headerData = new HeaderParseData();
    private final HttpParser httpParser;


    // ------------------------------ 构造方法 ------------------------------

    public Http11InputBuffer(Request request, int headerBufferSize,
                             boolean rejectIllegalHeader, HttpParser httpParser) {

        this.coyoteRequest = request;
        headers = request.getMimeHeaders();

        this.headerBufferSize = headerBufferSize;
        this.rejectIllegalHeader = rejectIllegalHeader;
        this.httpParser = httpParser;

        inputFilters = new InputFilter[0];
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
            return 0;
        }

        @Override
        public int available() {
            return 0;
        }
    }


    /**
     * 请求头行解析数据记录
     */
    private static class HeaderParseData {
        /**
         * 一行的起始位置
         */
        int lineStart = 0;
        
        int start = 0;
        
        int realPos = 0;
        
        int lastSignificantChar = 0;
        
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
    
    
    // ------------------------------ 通用方法 ------------------------------
    
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
