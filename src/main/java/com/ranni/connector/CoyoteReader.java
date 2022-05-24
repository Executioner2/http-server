package com.ranni.connector;

import java.io.BufferedReader;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/24 14:41
 */
public class CoyoteReader extends BufferedReader {
    
    protected InputBuffer ib;
    
    
    public CoyoteReader(InputBuffer ib) {
        super(ib, 1);
        this.ib = ib;
    }


    /**
     * 输入缓冲区置为null
     */
    void clear() {
        ib = null;
    }
}
