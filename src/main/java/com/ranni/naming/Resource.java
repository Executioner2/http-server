package com.ranni.naming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Title: HttpServer
 * Description:
 * 资源
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-07 14:27
 */
public class Resource {

    protected InputStream inputStream;
    protected byte[] binaryContent;

    public Resource() {
    }

    /**
     * 资源文件是流的形式
     *
     * @param input
     */
    public Resource(InputStream input) {
        setContext(input);
    }

    /**
     * 返回资源文件的内容（以二进制流的方式返回）
     * 如果存入的binaryContent不为空，则将binaryContent
     * 以ByteArrayInputStream返回
     *
     * @return
     */
    public InputStream streamContent() throws IOException {
        if (binaryContent != null)
            return new ByteArrayInputStream(binaryContent);

        return inputStream;
    }

    /**
     * 返回资源文件内容（以byte数组的方式返回）
     *
     * @return
     */
    public byte[] getBinaryContent() {
        return this.binaryContent;
    }

    /**
     * 设置资源文件以二进制流的方式输入
     *
     * @param input
     */
    public void setContext(InputStream input) {
        this.inputStream = input;
    }

    /**
     * 设置资源文件以byte数组方式存储
     *
     * @param binaryContext
     */
    public void setContext(byte[] binaryContext) {
        this.binaryContent = binaryContent;
    }

    /**
     * 返回资源内容（二进制数据）
     *
     * @return
     */
    public byte[] getContext() {
        return this.binaryContent;
    }
}
