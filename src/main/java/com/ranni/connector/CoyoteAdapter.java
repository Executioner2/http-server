package com.ranni.connector;

import com.ranni.container.Context;
import com.ranni.coyote.Adapter;
import com.ranni.util.SessionConfig;
import com.ranni.util.buf.B2CConverter;
import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.CharChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.ServerCookie;
import com.ranni.util.http.ServerCookies;
import com.ranni.util.net.SocketEvent;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Title: HttpServer
 * Description:
 * 适配器，用于将处理器传过来的请求交给相应的容器去处理
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/12 21:55
 * @Ref org.apache.catalina.connector.CoyoteAdapter
 */
public class CoyoteAdapter implements Adapter {

    // ==================================== 属性字段 ====================================

    /**
     * 连接器
     */
    private final Connector connector;

    /**
     * 是否允许URI中有反斜杠
     */
    protected static final boolean ALLOW_BACKSLASH = 
            Boolean.parseBoolean(System.getProperty("com.ranni.connector.CoyoteAdapter.ALLOW_BACKSLASH", "false"));


    // ==================================== 构造方法 ====================================
    
    public CoyoteAdapter(Connector connector) {
        this.connector = connector;
    }

    
    // ==================================== 核心方法 ====================================

    /**
     * 将处理器传过来的请求放到相应的容器的管道中执行
     * 
     * @param req coyote请求对象
     * @param res coyote响应对象
     *
     * @throws Exception 可能抛出I/O异常
     */
    @Override
    public void service(com.ranni.coyote.Request req, com.ranni.coyote.Response res) throws Exception {
        
    }

    @Override
    public boolean prepare(com.ranni.coyote.Request req, com.ranni.coyote.Response res) throws Exception {
        return false;
    }

    
    @Override
    public boolean asyncDispatch(com.ranni.coyote.Request req, com.ranni.coyote.Response res, SocketEvent status) throws Exception {
        return false;
    }

    
    @Override
    public void log(com.ranni.coyote.Request req, com.ranni.coyote.Response res, long time) {

    }

    
    @Override
    public void checkRecycled(com.ranni.coyote.Request req, com.ranni.coyote.Response res) {

    }

    
    @Override
    public String getDomain() {
        return null;
    }


    // ==================================== 字符串处理 ====================================

    /**
     * Parse session id in Cookie.
     *
     * @param request The Servlet request object
     */
    protected void parseSessionCookiesId(Request request) {

        // If session tracking via cookies has been disabled for the current
        // context, don't go looking for a session ID in a cookie as a cookie
        // from a parent context with a session ID may be present which would
        // overwrite the valid session ID encoded in the URL
        Context context = request.getMappingData().context;
        if (context != null && !context.getServletContext()
                .getEffectiveSessionTrackingModes().contains(
                        SessionTrackingMode.COOKIE)) {
            return;
        }

        // Parse session id from cookies
        ServerCookies serverCookies = request.getServerCookies();
        int count = serverCookies.getCookieCount();
        if (count <= 0) {
            return;
        }

        String sessionCookieName = SessionConfig.getSessionCookieName(context);

        for (int i = 0; i < count; i++) {
            ServerCookie scookie = serverCookies.getCookie(i);
            if (scookie.getName().equals(sessionCookieName)) {
                // Override anything requested in the URL
                if (!request.isRequestedSessionIdFromCookie()) {
                    // Accept only the first session id cookie
                    convertMB(scookie.getValue());
                    request.setRequestedSessionId
                            (scookie.getValue().toString());
                    request.setRequestedSessionCookie(true);
                    request.setRequestedSessionURL(false);
                } else {
                    if (!request.isRequestedSessionIdValid()) {
                        // Replace the session id until one is valid
                        convertMB(scookie.getValue());
                        request.setRequestedSessionId
                                (scookie.getValue().toString());
                    }
                }
            }
        }

    }


    /**
     * Character conversion of the URI.
     *
     * @param uri MessageBytes object containing the URI
     * @param request The Servlet request object
     * @throws IOException if a IO exception occurs sending an error to the client
     */
    protected void convertURI(MessageBytes uri, Request request) throws IOException {

        ByteChunk bc = uri.getByteChunk();
        int length = bc.getLength();
        CharChunk cc = uri.getCharChunk();
        cc.allocate(length, -1);

        Charset charset = connector.getURICharset();

        B2CConverter conv = request.getURIConverter();
        if (conv == null) {
            conv = new B2CConverter(charset, true);
            request.setURIConverter(conv);
        } else {
            conv.recycle();
        }

        try {
            conv.convert(bc, cc, true);
            uri.setChars(cc.getBuffer(), cc.getStart(), cc.getLength());
        } catch (IOException ioe) {
            // Should never happen as B2CConverter should replace
            // problematic characters
            request.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }


    /**
     * Character conversion of the a US-ASCII MessageBytes.
     *
     * @param mb The MessageBytes instance containing the bytes that should be converted to chars
     */
    protected void convertMB(MessageBytes mb) {

        // This is of course only meaningful for bytes
        if (mb.getType() != MessageBytes.T_BYTES) {
            return;
        }

        ByteChunk bc = mb.getByteChunk();
        CharChunk cc = mb.getCharChunk();
        int length = bc.getLength();
        cc.allocate(length, -1);

        // Default encoding: fast conversion
        byte[] bbuf = bc.getBuffer();
        char[] cbuf = cc.getBuffer();
        int start = bc.getStart();
        for (int i = 0; i < length; i++) {
            cbuf[i] = (char) (bbuf[i + start] & 0xff);
        }
        mb.setChars(cbuf, 0, length);

    }


    /**
     * This method normalizes "\", "//", "/./" and "/../".
     *
     * @param uriMB URI to be normalized
     *
     * @return <code>false</code> if normalizing this URI would require going
     *         above the root, or if the URI contains a null byte, otherwise
     *         <code>true</code>
     */
    public static boolean normalize(MessageBytes uriMB) {

        ByteChunk uriBC = uriMB.getByteChunk();
        final byte[] b = uriBC.getBytes();
        final int start = uriBC.getStart();
        int end = uriBC.getEnd();

        // An empty URL is not acceptable
        if (start == end) {
            return false;
        }

        int pos = 0;
        int index = 0;


        // The URL must start with '/' (or '\' that will be replaced soon)
        if (b[start] != (byte) '/' && b[start] != (byte) '\\') {
            return false;
        }

        // Replace '\' with '/'
        // Check for null byte
        for (pos = start; pos < end; pos++) {
            if (b[pos] == (byte) '\\') {
                if (ALLOW_BACKSLASH) {
                    b[pos] = (byte) '/';
                } else {
                    return false;
                }
            } else if (b[pos] == (byte) 0) {
                return false;
            }
        }

        // Replace "//" with "/"
        for (pos = start; pos < (end - 1); pos++) {
            if (b[pos] == (byte) '/') {
                while ((pos + 1 < end) && (b[pos + 1] == (byte) '/')) {
                    copyBytes(b, pos, pos + 1, end - pos - 1);
                    end--;
                }
            }
        }

        // If the URI ends with "/." or "/..", then we append an extra "/"
        // Note: It is possible to extend the URI by 1 without any side effect
        // as the next character is a non-significant WS.
        if (((end - start) >= 2) && (b[end - 1] == (byte) '.')) {
            if ((b[end - 2] == (byte) '/')
                    || ((b[end - 2] == (byte) '.')
                    && (b[end - 3] == (byte) '/'))) {
                b[end] = (byte) '/';
                end++;
            }
        }

        uriBC.setEnd(end);

        index = 0;

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            index = uriBC.indexOf("/./", 0, 3, index);
            if (index < 0) {
                break;
            }
            copyBytes(b, start + index, start + index + 2,
                    end - start - index - 2);
            end = end - 2;
            uriBC.setEnd(end);
        }

        index = 0;

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            index = uriBC.indexOf("/../", 0, 4, index);
            if (index < 0) {
                break;
            }
            // Prevent from going outside our context
            if (index == 0) {
                return false;
            }
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (b[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyBytes(b, start + index2, start + index + 3,
                    end - start - index - 3);
            end = end + index2 - index - 3;
            uriBC.setEnd(end);
            index = index2;
        }

        return true;

    }


    /**
     * Check that the URI is normalized following character decoding. This
     * method checks for "\", 0, "//", "/./" and "/../".
     *
     * @param uriMB URI to be checked (should be chars)
     *
     * @return <code>false</code> if sequences that are supposed to be
     *         normalized are still present in the URI, otherwise
     *         <code>true</code>
     *
     * @deprecated This code will be removed in Apache Tomcat 10 onwards
     */
    @Deprecated
    public static boolean checkNormalize(MessageBytes uriMB) {

        CharChunk uriCC = uriMB.getCharChunk();
        char[] c = uriCC.getChars();
        int start = uriCC.getStart();
        int end = uriCC.getEnd();

        int pos = 0;

        // Check for '\' and 0
        for (pos = start; pos < end; pos++) {
            if (c[pos] == '\\') {
                return false;
            }
            if (c[pos] == 0) {
                return false;
            }
        }

        // Check for "//"
        for (pos = start; pos < (end - 1); pos++) {
            if (c[pos] == '/') {
                if (c[pos + 1] == '/') {
                    return false;
                }
            }
        }

        // Check for ending with "/." or "/.."
        if (((end - start) >= 2) && (c[end - 1] == '.')) {
            if ((c[end - 2] == '/')
                    || ((c[end - 2] == '.')
                    && (c[end - 3] == '/'))) {
                return false;
            }
        }

        // Check for "/./"
        if (uriCC.indexOf("/./", 0, 3, 0) >= 0) {
            return false;
        }

        // Check for "/../"
        if (uriCC.indexOf("/../", 0, 4, 0) >= 0) {
            return false;
        }

        return true;

    }


    /**
     * Copy an array of bytes to a different position. Used during
     * normalization.
     *
     * @param b The bytes that should be copied
     * @param dest Destination offset
     * @param src Source offset
     * @param len Length
     */
    protected static void copyBytes(byte[] b, int dest, int src, int len) {
        System.arraycopy(b, src, b, dest, len);
    }
    
    
}
