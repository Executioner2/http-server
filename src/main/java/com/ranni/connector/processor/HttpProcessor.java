package com.ranni.connector.processor;

import com.ranni.connector.HttpConnector;
import com.ranni.connector.SocketInputStream;
import com.ranni.connector.http.HttpHeader;
import com.ranni.connector.http.HttpRequestLine;
import com.ranni.connector.http.request.HttpRequestBase;
import com.ranni.connector.http.response.HttpResponseBase;
import com.ranni.container.Container;
import com.ranni.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Title: HttpServer
 * Description:
 * TODO 先看这儿
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 23:04
 */
public class HttpProcessor implements Processor {
    protected HttpRequestBase request;
    protected HttpResponseBase response;
    protected HttpConnector connector;
    protected Container container;
    private boolean nullRequest; // 是否是空包请求

    public HttpProcessor(HttpConnector connector, Container container) {
        this.connector = connector;
        this.container = container;
    }

    /**
     * 主要就是处理请求行和请求头，然后再交给容器做下一步处理
     */
    @Override
    public void process(Socket socket) {
        SocketInputStream input = null;
        OutputStream output = null;

        try {
            request = (HttpRequestBase) connector.createRequest();
            response = (HttpResponseBase) connector.createResponse();

            // 设置请求对象和响应对象的部分参数
            response.setRequest(request);
            request.setScheme(connector.getScheme());
            request.setStream(socket.getInputStream());
            response.setStream(socket.getOutputStream());

            input = new SocketInputStream(request.getStream());
            output = response.getStream();

            // 预处理解析请求行和请求头
            parseRequest(input, output); // 对请求行进行解析
            if (nullRequest) return; // XXX request空包，抽空找下原因（通过排查input流，并未发现是因为流没关闭的原因）
            parseHeaders(input); // 对请求头进行解析

//            if (request.getRequestURI().startsWith("/servlet/")) {
//                ServletProcessor processor = new ServletProcessor();
//                processor.process(request, response);
//            } else {
//                StaticResourceProcessor processor = new StaticResourceProcessor();
//                processor.process(request, response);
//            }

            container.invoke(request, response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析请求头
     * @param input
     */
    private void parseHeaders(SocketInputStream input) throws ServletException, IOException {
        while (true) {
            HttpHeader header = new HttpHeader();
            input.readHeader(header);

            if (header.nameEnd == 0) {
                if (header.valueEnd == 0) {
                    return;
                } else {
                    throw new ServletException("http请求头有异常！");
                }
            }

            String name = new String(header.name, 0, header.nameEnd);
            String value = new String(header.value, 0, header.valueEnd);
            request.addHeader(name, value);

            if ("cookie".equals(name)) {
                // 如果是cookie信息
                Cookie[] cookies = RequestUtil.parseCookieHeader(value); // 解析cookie
                for (Cookie cookie : cookies) {
                    if ("jsessionid".equals(cookie.getName()) && !request.isRequestedSessionIdFromCookie()) {
                        // 仅添加第一个匹配到的jsessionid到request对象中
                        // 存在于cookie中的jsessionid会覆盖url中的jsessionid
                        request.setRequestedSessionId(value);
                        request.setRequestedSessionCookie(true);
                        request.setRequestedSessionURL(false);
                    }
                    request.addCookie(cookie);
                }
            } else if ("content-length".equals(name)) {
                // 请求体长度
                int n = -1;
                try {
                    n = Integer.parseInt(value);
                } catch (Exception e) {
                    throw new ServletException("http请求头参数异常！");
                }
                request.setContentLength(n);
            } else if ("content-type".equals(name)) {
                // 请求体类型
                request.setContentType(value);
            }
        } // while end
    }


    /**
     * 解析请求行
     * @param input
     * @param output
     */
    private void parseRequest(SocketInputStream input, OutputStream output) throws ServletException, IOException {
        HttpRequestLine requestLine = new HttpRequestLine();
        input.readRequestLine(requestLine);

        if (input.isNullRequest()) {
            this.nullRequest = true;
            return;
        }

        String method = new String(requestLine.method, 0, requestLine.methodEnd);
        String uri = null;
        String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);

        if (method.length() < 1) throw new ServletException("缺少HTTP请求方法");
        if (protocol.length() < 1) throw new ServletException("缺少protocol");
        if (requestLine.uriEnd < 1) throw new ServletException("缺少uri信息");

        // 解析存在于uri中的查询参数
        int question = requestLine.indexOf('?');
        if (question >= 0) {
            request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));
            uri = new String(requestLine.uri, 0, question);
        } else {
            request.setQueryString(null); // uri中没有请求参数
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }

        // 检查是否是绝对路径的uri，如果是则要进行二次处理
        if (!uri.startsWith("/")) { // 不是/开头，那么就是绝对路径 http://lani.com/servlet/getName  这种
            int pos = uri.indexOf("://");
            if (pos != -1) {
                pos = uri.indexOf('/', pos + 3); // 从 :// 之后检查第一个/
                if (pos == -1) {
                    uri = "";
                } else {
                    uri = uri.substring(pos); // 只取请求的资源路径，消除域名（包括）之前的无效字符串
                }
            }
        }

        // 从uri中解析session id
        // 如果uri中包含 ";jsessionid=" 则说明把session id写在了uri中而不在Cookie中，需要取出并设置在request对象中
        String match = ";jsessionid=";
        int semicolon = uri.indexOf(match);
        if (semicolon != -1) {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(';'); // 结束位置
            if (semicolon2 != -1) {
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2); // sessionId 之后的字符串
            } else {
                request.setRequestedSessionId(rest);
                rest = ""; // 之后没有字符串了
            }
            request.setRequestedSessionURL(true); // 是否存在与URL中
            uri = uri.substring(0, semicolon) + rest; // 重组uri
        } else {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }

        String normalizedUri = RequestUtil.normalize(uri); // 对uri进行修正

        if (normalizedUri != null) {
            request.setRequestURI(normalizedUri);
        } else {
            request.setRequestURI(uri);
        }

        request.setScheme("http");
        request.setMethod(method);
        request.setProtocol(protocol);

        if (normalizedUri == null) throw new ServletException("未规范化 URI: " + uri);
    }
}
