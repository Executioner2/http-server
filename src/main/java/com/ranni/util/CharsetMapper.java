package com.ranni.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Title: HttpServer
 * Description:
 * 如果没有指定Content-Type，那么就从本地的首选语言
 * 选取最适合本机的对应的编码格式
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-23 21:38
 */
public class CharsetMapper {
    public static final String DEFAULT_RESOURCE = "/com/ranni/util/CharsetMapperDefault.properties";

    private Properties map = new Properties();

    public CharsetMapper() {
        this(DEFAULT_RESOURCE);
    }

    public CharsetMapper(String path) {
        try (InputStream input = this.getClass().getResourceAsStream(path)) {
            map.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCharset(Locale locale) {
        String charset = null;

        charset = map.getProperty(locale.toString());
        if (charset != null) return charset;

        charset = map.getProperty(locale.getCountry());
        return charset;
    }
}
