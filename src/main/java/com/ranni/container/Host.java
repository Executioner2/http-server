package com.ranni.container;

import com.ranni.util.buf.ByteChunk;
import com.ranni.util.buf.MessageBytes;
import com.ranni.util.http.parse.HttpParser;

import java.io.IOException;
import java.io.Reader;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 14:58
 */
public interface Host extends Container {
    /**
     * 添加别名时发送的事件名
     */
    String ADD_ALIAS_EVENT = "addAlias";


    /**
     * 移除别名时添加的事件名
     */
    String REMOVE_ALIAS_EVENT = "removeAlias";


    class MessageBytesReader extends Reader {

        private final byte[] bytes;
        private final int end;
        private int pos;
        private int mark;

        public MessageBytesReader(MessageBytes mb) {
            ByteChunk bc = mb.getByteChunk();
            bytes = bc.getBytes();
            pos = bc.getOffset();
            end = bc.getEnd();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                // Want output in range 0 to 255, not -128 to 127
                cbuf[i] = (char) (bytes[pos++] & 0xFF);
            }
            return len;
        }

        @Override
        public void close() throws IOException {
            // NO-OP
        }

        // Over-ridden methods to improve performance

        @Override
        public int read() throws IOException {
            if (pos < end) {
                // Want output in range 0 to 255, not -128 to 127
                return bytes[pos++] & 0xFF;
            } else {
                return -1;
            }
        }

        // Methods to support mark/reset

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            mark = pos;
        }

        @Override
        public void reset() throws IOException {
            pos = mark;
        }
    }
    

    /**
     * 解析Host标头
     * 
     * @param hostMB Host标头字节块
     * @return 返回主机名与端口号的分隔符':'的位置，如果不存在则返回-1
     * Host: webapp:8080
     */
    static int parse(MessageBytes hostMB) {
        return parse(new MessageBytesReader(hostMB));
    }


    static int parse(Reader reader) {
        try {
            reader.mark(1);
            int first = reader.read();
            reader.reset();
            if (HttpParser.isAlpha(first)) {
                return HttpParser.readHostDomainName(reader);
            } else if (HttpParser.isNumeric(first)) {
                return HttpParser.readHostIPv4(reader, false);
            } else if ('[' == first) {
                return HttpParser.readHostIPv6(reader);
            } else {
                // Invalid
                throw new IllegalArgumentException();
            }
        } catch (IOException ioe) {
            // Should never happen
            throw new IllegalArgumentException(ioe);
        }
    }
    

    /**
     * 返回此主机的应用程序根目录，可以是绝对路径也可以是相对路径还可以是URL
     *
     * @return
     */
    String getAppBase();


    /**
     * 设置此主机的应用程序根目录
     *
     * @param appBase
     */
    void setAppBase(String appBase);


    /**
     * 返回自动部署标志
     *
     * @return
     */
    boolean getAutoDeploy();


    /**
     * 设置自动部署标志
     *
     * @param autoDeploy
     */
    void setAutoDeploy(boolean autoDeploy);


    /**
     * 返回此虚拟主机的规范名称
     *
     * @return
     */
    String getName();


    /**
     * 设置此虚拟主机的名称
     *
     * @param name
     */
    void setName(String name);
    

    /**
     * 给此虚拟主机添加别名
     *
     * @param alias
     */
    void addAlias(String alias);


    /**
     * 查询此别名是否存在
     * 
     * @param server
     * @return
     */
    boolean findAliases(String server);

    
    /**
     * 返回此虚拟主机所有别名
     *
     * @return
     */
    String[] findAliases();


    /**
     * 根据uri找到对应的容器
     *
     * @param uri
     * @return
     */
    Context map(String uri);


    /**
     * 移除指定别名
     *
     * @param alias
     */
    void removeAlias(String alias);
}
