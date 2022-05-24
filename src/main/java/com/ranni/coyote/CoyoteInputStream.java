package com.ranni.coyote;

import com.ranni.connector.InputBuffer;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/24 12:01
 */
public class CoyoteInputStream extends ServletInputStream {
    protected InputBuffer ib;
    
    public CoyoteInputStream(InputBuffer inputBuffer) {
        this.ib = inputBuffer;
    }

    
    public void clear() {
        ib = null;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
