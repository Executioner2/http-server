package com.ranni.connector;

import com.ranni.container.MappingData;
import com.ranni.util.buf.MessageBytes;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/13 21:26
 * @Ref org.apache.catalina.mapper.Mapper
 */
public final class Mapper {

    /**
     * 容器和socket的映射
     * 
     * @param serverName 服务器名
     * @param decodedURI 解码后的uri
     * @param version 映射的版本
     * @param mappingData 映射数据
     */
    public void map(MessageBytes serverName, MessageBytes decodedURI, String version, MappingData mappingData) {
    }
}
