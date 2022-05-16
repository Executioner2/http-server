package com.ranni.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 * Title: HttpServer
 * Description:
 * 类的资源视图
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-13 17:13
 */
public class ResourceEntry {
    public long lastModified = -1L; // 最后修改时间
    public byte[] binaryContent; // 二进制流缓存
    public Class loadedClass; // 从路径指向的文件中解析出来的类
    public URL source; // "jar:" + jar包路径 + "!/" + 文件相对路径（如果是从文件夹中找到的，则该参数和codeBase一致）
    public URL codeBase; // 如果是jar中的资源，则是仓库的绝对路径，如果是文件夹中的资源，则是资源的绝对路径
    public Manifest manifest; // 从JAR加载的清单
    public Certificate[] certificates; // JAR包的CER证书
}
