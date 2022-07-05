package com.ranni.coyote;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/23 14:19
 */
public final class Constants {
    public static final Charset DEFAULT_URI_CHARSET = StandardCharsets.UTF_8;
    public static final Charset DEFAULT_BODY_CHARSET = StandardCharsets.ISO_8859_1;

    // Default protocol settings
    public static final int DEFAULT_CONNECTION_LINGER = -1;
    public static final boolean DEFAULT_TCP_NO_DELAY = true;
    
    public static final int MAX_NOTES = 32;

    // Request states
    public static final int STAGE_NEW = 0;
    public static final int STAGE_PARSE = 1;
    public static final int STAGE_PREPARE = 2;
    public static final int STAGE_SERVICE = 3;
    public static final int STAGE_ENDINPUT = 4;
    public static final int STAGE_ENDOUTPUT = 5;
    public static final int STAGE_KEEPALIVE = 6;
    public static final int STAGE_ENDED = 7;

    public static final String SENDFILE_FILE_END_ATTR = "com.ranni.sendfile.end";
    
    public static final String SENDFILE_FILE_START_ATTR = "com.ranni.sendfile.start";

    public static final String REMOTE_ADDR_ATTRIBUTE = "com.ranni.remoteAddr";

}
