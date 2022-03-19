package com.ranni.connector.stream;

import com.ranni.connector.http.HttpHeader;
import com.ranni.connector.http.HttpRequestLine;

import javax.servlet.ServletException;
import java.io.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 20:00
 */
public class SocketInputStream extends InputStream {
    private static final byte NUL = 0;
    private static final byte CR = (byte)'\r';
    private static final byte LF = (byte)'\n';
    private static final byte SP = (byte)' ';
    private static final byte HT = (byte)'\t';
    private static final byte COLON = (byte)':';
    private static final int OFFSET = 'a' - 'A';

    private InputStream is;
    private byte[] buffer;
    private int pos;
    private int count;
    private boolean nullRequest = false; // 空的请求包

    public SocketInputStream() {
    }

    public SocketInputStream(InputStream is) {
        this.is = is;
    }

    public SocketInputStream(InputStream is, int bufferSize) {
        this.is = is;
        this.buffer = new byte[bufferSize];
    }

    public boolean isNullRequest() {
        return this.nullRequest;
    }

    /**
     * 读取字节流中的数据，一次只读一个字节
     * @return
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) return -1; // 读完了
        }

        return buffer[pos++];
    }

    /**
     * 冲字节流中取出一部分数据填充缓冲区
     * @throws IOException
     */
    private void fill() throws IOException {
        pos = count = 0;
        int readLen = is.read(buffer, 0, buffer.length);
        if (readLen > 0) count = readLen;
    }

    /**
     * 解析请求行
     * @param requestLine
     */
    public void readRequestLine(HttpRequestLine requestLine) throws IOException, ServletException {
        requestLine.recycle(); // 初始化一些参数
        int val;

        // 处理/r/n
        do {
            try {
                val = read();
            } catch (IOException e) {
                val = -1;
            }
        } while (val == CR || val == LF);

        if (--pos == -1) {
            this.nullRequest = true; // XXX 空包
            return;
        }

        // 初始数据，准备处理method
        boolean space = false;
        int maxRead = requestLine.method.length; // 最大读取容量
        int readCount = 0; // 已经读取的长度

        // 开始处理method
        while (!space) { // 没有遇到空格
            if (readCount >= maxRead) { // 超出数组最大容量，扩容，按两倍扩大
                if (maxRead * 2 <= HttpRequestLine.MAX_METHOD_SIZE) {
                    char[] newBuffer = new char[maxRead * 2];
                    System.arraycopy(requestLine.method, 0, newBuffer, 0, maxRead);
                    requestLine.method = newBuffer;
                    maxRead = requestLine.method.length;
                } else {
                    throw new IOException("请求包中method太长！");
                }
            }

            val = read();
            if (val == -1) throw new IOException("请求包状态行错误！"); // 没有可以读取的数据了
            space = (val == SP || val == NUL); // 是否是空格，是空格将结束循环
            requestLine.method[readCount++] = (char)val;
        }

        requestLine.methodEnd = readCount - 1;

        // 初始数据，准备处理uri
        space = false;
        maxRead = requestLine.uri.length;
        readCount = 0;
        boolean eof = false; // 状态行是否读完

        // 开始处理uri
        while (!space) {
            if (readCount >= maxRead) {
                if (maxRead * 2 <= HttpRequestLine.MAX_URI_SIZE) {
                    char[] newBuffer = new char[maxRead * 2];
                    System.arraycopy(requestLine.uri, 0, newBuffer, 0, maxRead);
                    requestLine.uri = newBuffer;
                    maxRead = requestLine.uri.length;
                } else {
                    throw new IOException("请求包中uri太长！");
                }
            }

            val = read();
            if (val == -1) throw new IOException("请求包状态行错误！");
            if (val == SP) {
                space = true;
            } else if (val == CR || val == LF) {
                // HTTP 0.9 请求风格
                space = true;
                eof = true;
            }

            requestLine.uri[readCount++] = (char)val;
        }

        requestLine.uriEnd = readCount - 1;

        // 初始数据，准备处理protocol
        maxRead = requestLine.protocol.length;
        readCount = 0;

        // 处理protocol
        while (!eof) {
            if (readCount >= maxRead) {
                if (maxRead * 2 <= HttpRequestLine.MAX_PROTOCOL_SIZE) {
                    char[] newBuffer= new char[maxRead * 2];
                    System.arraycopy(requestLine.protocol, 0, newBuffer, 0, maxRead);
                    requestLine.protocol = newBuffer;
                    maxRead = requestLine.protocol.length;
                }
            }

            val = read();
            if (val == -1) throw new IOException("请求包状态行错误！");
            if (val == LF) eof = true; // 换行
            else requestLine.protocol[readCount++] = (char)val;
        }

        requestLine.protocolEnd = readCount;
    }

    /**
     * 从数据流中读取数据到HttpHeader中
     * @param header
     */
    public void readHeader(HttpHeader header) throws IOException {
        // 初始化header
        if (header.nameEnd != 0)
            header.recycle();

        int val = read();

        // 1、判断一个参数是否读完
        if (val == CR || val == LF) {
            if (val == CR) read(); // 如果当前读取的字符时\r，那么再读取一位，将下一位的\n也读了，不然会导致下一次读也读不出参数
            return;
        }

        pos--; // 往回退一手，下次把刚刚读了的再读一次

        // 2、读取name并写入到header对象的name属性中
        int maxRead = header.name.length;
        int readCount = 0;
        boolean colon = false; // 是否遇到了分号(:)

        while (!colon) {
            // 如果header.name装满了，就扩容
            if (readCount >= maxRead) {
                if (maxRead * 2 <= HttpHeader.MAX_NAME_SIZE) {
                    char[] newBuffer = new char[maxRead * 2];
                    System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
                    header.name = newBuffer;
                    maxRead = header.name.length;
                } else {
                    throw new IOException("请求体请求参数name过长！");
                }
            }

            val = read();
            if (val == -1) throw new IOException("请求体错误！");
            colon = val == COLON;
            header.name[readCount++] = (char)(val >= 'A' && val <= 'Z' ? val + OFFSET : val); // 转小写字符
        }

        header.nameEnd = readCount - 1;

        maxRead = header.value.length;
        readCount = 0;
        boolean eol = false; // 是否读完了
        boolean validLine = true; // 有效行

        while (validLine) {
            // 3、消除name与value之间的空格
            boolean space = true;
            while (space) {
                val = read();
                if (val == -1) throw new IOException("请求体错误！");
                if (val != SP && val != HT && (--pos) >= 0) space = false;
            }

            // 4、读取value并写入到header对象的value属性中
            while (!eol) {
                if (readCount >= maxRead) {
                    if (maxRead * 2 <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[maxRead * 2];
                        System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException("请求体请求参数value过长！");
                    }
                }

                val = read();
                if (val == -1) throw new IOException("请求体错误！");
                if (val == LF) {
                    eol = true;
                } else {
                    header.value[readCount++] = (char)val;
                }
            } // while(!eol) - end

            // XXX 下一个字符是空格或者水平制表符表示后面还有value。（但是上面是读value是遇到LF才结束的，如果行是有前导空格或水平制表符的下一个name，将会出现错误）
            val = read();
            if (val == SP || val == HT) {
                if (readCount >= maxRead) {
                    if (maxRead * 2 <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[maxRead * 2];
                        System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException("请求体请求参数value过长！");
                    }
                }
                header.value[readCount++] = ' ';
            } else {
                pos--;
                validLine = false;
            }
        } // while(validLine) - end

        header.valueEnd = readCount;
    }

    /**
     * 关闭流，释放资源
     * @throws IOException
     */
    public void close() throws IOException {
        if (is == null) return;
        is.close();
        is = null;
        buffer = null;
    }
}
