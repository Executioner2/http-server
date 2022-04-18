package com.ranni.naming;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * Title: HttpServer
 * Description:
 * name解析器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-07 17:18
 */
@Deprecated // 暂时没有用到
public class NameParserImpl implements NameParser {
    @Override
    public Name parse(String name) throws NamingException {
        return new CompositeName(name);
    }
}
