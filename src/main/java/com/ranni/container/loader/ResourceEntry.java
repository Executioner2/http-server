package com.ranni.container.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 * Title: HttpServer
 * Description:
 * 缓存的资源视图
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-13 17:13
 */
public class ResourceEntry {
    public long lastModified = -1L; // 最后修改时间
    public byte[] binaryCount; // 二进制流缓存
    public Class loadedClass; // 关联的类加载器
    public URL source; // 源
    public URL codeBase; // 加载对象的代码库？？？
    public Manifest manifest; // 从JAR加载的清单
    public Certificate[] certificates; // JAR包的CER证书
}
