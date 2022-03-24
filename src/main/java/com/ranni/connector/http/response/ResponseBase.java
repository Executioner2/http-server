package com.ranni.connector.http.response;

import com.ranni.connector.http.Connector;
import com.ranni.connector.http.Context;
import com.ranni.connector.http.request.Request;
import com.ranni.util.CharsetMapper;
import com.ranni.util.RequestUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-22 18:26
 */
public abstract class ResponseBase implements ServletResponse, Response {
    protected ResponseFacade facade = new ResponseFacade(this); // 响应外观类
    protected Connector connector; // 连接器
    protected int contentCount; // 输入到输出流中真实的数据长度
    protected Context context; // context类型的容器
    protected boolean appCommitted; // 应用程序是否提交响应
    protected boolean committed; // 是否提交响应
    protected boolean included; // TODO 不知道干嘛的
    protected String info; // 描述信息和版本号
    protected Request request; // 请求
    protected ServletResponse response; // 响应对象
    protected OutputStream output; // 输出流
    protected boolean suspended; // 挂起标志
    protected boolean error; // 错误标志
    protected ServletOutputStream stream; // Servlet输出流
    protected int contentLength = -1; // 设置或计算出来的响应体长度
    protected String contentType; // 响应体类型
    protected PrintWriter writer; // 字符串输出处理流
    protected String characterEncoding; // 字符编码格式
    protected static Locale defaultLocale = Locale.getDefault(); // 本地化
    protected Locale locale = Locale.getDefault(); // 本地化
    protected int bufferCount; // 当前在缓冲区中的数据数
    protected byte[] buffer = new byte[1024]; // 缓冲流

    /**
     * 返回与此请求关联的连接器
     * @return
     */
    @Override
    public Connector getConnector() {
        return this.connector;
    }

    /**
     * 设置与此请求关联的连接器
     * @param connector
     */
    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    /**
     * 返回真实的输出数据长度
     * @return
     */
    @Override
    public int getContentCount() {
        return this.contentCount;
    }

    /**
     * 取得Context类型的容器
     * @return
     */
    @Override
    public Context getContext() {
        return this.context;
    }

    /**
     * 设置Context类型容器
     * @param context
     */
    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * 设置响应是否发送标志
     * @param appCommitted
     */
    @Override
    public void setAppCommitted(boolean appCommitted) {
        this.appCommitted = appCommitted;
    }

    /**
     * 返回响应发送标志
     * @return
     */
    @Override
    public boolean isAppCommitted() {
        return this.appCommitted || this.committed;
    }

    /**
     * TODO 不清楚干嘛的
     * @return
     */
    @Override
    public boolean getIncluded() {
        return this.included;
    }

    @Override
    public void setIncluded(boolean included) {
        this.included = included;
    }

    /**
     * 返回描述信息和版本号
     * @return
     */
    @Override
    public String getInfo() {
        return this.info;
    }

    /**
     * 返回请求对象
     * @return
     */
    @Override
    public Request getRequest() {
        return this.request;
    }

    /**
     * 设置请求对象
     * @param request
     */
    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * 取得响应对象的外观类
     * @return
     */
    @Override
    public ServletResponse getResponse() {
        return this.facade;
    }

    /**
     * 返回标准输出流
     * @return
     */
    @Override
    public OutputStream getStream() {
        return this.output;
    }

    /**
     * 设置标准输出流
     * @param stream
     */
    @Override
    public void setStream(OutputStream stream) {
        this.output = stream;
    }

    /**
     * 设置挂起标志
     * @param suspended
     */
    @Override
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
        if (stream != null) ((ResponseStream)stream).setSuspended(suspended);
    }

    /**
     * 返回挂起标志
     * @return
     */
    @Override
    public boolean isSuspended() {
        return this.suspended;
    }

    /**
     * 设置错误标志
     */
    @Override
    public void setError() {
        this.error = true;
    }

    /**
     * 返回错误标志
     * @return
     */
    @Override
    public boolean isError() {
        return this.error;
    }

    /**
     * 创建标准输出流的处理流
     * @return
     * @throws IOException
     */
    @Override
    public ServletOutputStream createOutputStream() throws IOException {
        return new ResponseStream(this);
    }

    /**
     * 完成响应，关闭资源
     * @throws IOException
     */
    @Override
    public void finishResponse() throws IOException {
        // XXX Tomcat在这里的做法是stream为空就获取一个然后关闭
        if (stream == null) return;

        if (((ResponseStream)stream).closed()) return;

        if (writer != null) {
            writer.flush();
            writer.close();
        } else {
            stream.flush();
            stream.close();
        }
    }

    /**
     * 返回设置或计算的响应体字节数
     * @return
     */
    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * 返回响应体类型
     * @return
     */
    @Override
    public String getContentType() {
        return this.contentType;
    }

    /**
     * 取得输出处理流
     * @return
     */
    @Override
    public PrintWriter getReporter() {
        if (isError()) {
            // 有错误，无论是否调用createOutputStream()都要创建一个输出处理流
            try {
                if (stream == null) {
                    stream = getOutputStream();
                }
            } catch (IOException e) {
                return null;
            }
            return new PrintWriter(stream);

        } else {
            // 没有错误，那么调用过createOutputStream()就不能创建PrintWriter对象
            if (stream == null) {
                return new PrintWriter(stream);
            } else {
                return null;
            }
        }
    }

    /**
     * 初始化部分参数，便于重复使用此对象
     */
    @Override
    public void recycle() {
        bufferCount = 0;
        committed = false;
        appCommitted = false;
        suspended = false;
        contentCount = 0;
        contentLength = -1;
        contentType = null;
        context = null;
        characterEncoding = null;
        included = false;
        locale = Locale.getDefault();
        output = null;
        request = null;
        stream = null;
        writer = null;
        error = false;
    }

    /**
     * 取得编码格式
     * @return
     */
    @Override
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            return "ISO-8859-1";
        }

        return this.characterEncoding;
    }

    /**
     * 取得响应流
     * @return
     * @throws IOException
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) throw new IllegalStateException("getWriter()已被调用！");
        if (stream == null) stream = createOutputStream();
        ((ResponseStream)stream).setCommit(true);
        return stream;
    }

    /**
     * 创建并返回PrintWriter对象
     * @return
     * @throws IOException
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (stream != null) throw new IllegalStateException("createOutputStream()已被调用！");
        if (writer != null) return writer;
        ResponseStream newStream = (ResponseStream) createOutputStream();
        OutputStreamWriter osr = new OutputStreamWriter(newStream, getCharacterEncoding());
        writer = new ResponseWriter(osr, newStream);
        stream = newStream;
        return writer;
    }

    /**
     * 设置响应体长度
     * @param i
     */
    @Override
    public void setContentLength(int i) {
        if (isCommitted()) return;

        this.contentLength = i;

        this.contentLength = i;
    }

    /**
     * 设置响应体类型
     * @param s
     */
    @Override
    public void setContentType(String s) {
        if (isCommitted()) return;
        this.contentType = s;

        if (s.indexOf(';') >= 0) {
            characterEncoding = RequestUtil.parseCharacterEncoding(s);
            if (characterEncoding == null)
                characterEncoding = "ISO-8859-1";
        } else {
            if (characterEncoding != null)
                contentType = s + ";charset=" + characterEncoding;
        }
    }

    /**
     * 设置缓冲区大小
     * 当响应已提交或缓存流中有数据就抛出异常
     * 当缓冲区本身大小大于输入值则直接返回
     * @param i
     */
    @Override
    public void setBufferSize(int i) {
        if (committed || bufferCount > 0) throw new IllegalStateException("响应已提交或缓存流中有数据！");
        if (buffer.length >= i) return;
        this.buffer = new byte[i];
    }

    /**
     * 设置缓冲区大小
     * @return
     */
    @Override
    public int getBufferSize() {
        return this.buffer.length;
    }

    /**
     * 刷新缓冲区并且完成此响应的发送
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException {
        if (isCommitted()) return;
        committed = true;
        if (bufferCount > 0) { // 缓冲区中有数据
            try {
                output.write(buffer, 0, bufferCount);
            } finally {
                bufferCount = 0;
            }
        }
    }

    /**
     * 重置缓冲区
     */
    @Override
    public void resetBuffer() {
        if (isCommitted()) throw new IllegalStateException("此响应已经完成提交！");
        this.bufferCount = 0;
    }

    /**
     * 是否已经提交响应
     * TODO 和appCommitted好像没什么区别
     * @return
     */
    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    /**
     * 清除缓冲区中所有内容
     */
    @Override
    public void reset() {
        if (isCommitted()) throw new IllegalStateException("此响应已经完成提交！");

        if (stream != null)
            ((ResponseStream)stream).reset();

        bufferCount = 0;
        contentLength = -1;
        contentType = null;
    }

    /**
     * 设置适合此响应的地区以及编码格式
     * @param locale
     */
    @Override
    public void setLocale(Locale locale) {
        if (isCommitted()) throw new IllegalStateException("此响应已经完成提交！");

        this.locale = locale;
        if (this.context != null) {
            // 从locale中获取编码
            CharsetMapper mapper = context.getCharsetMapper();
            characterEncoding = mapper.getCharset(locale);
            if (contentType != null) {
                int i = contentType.indexOf(";");
                if (i >= 0) contentType = contentType.substring(0, i);
                contentType = contentType + ";charset=" + characterEncoding;
            }
        }
    }

    /**
     * 返回locale
     * @return
     */
    @Override
    public Locale getLocale() {
        return this.locale;
    }
}
