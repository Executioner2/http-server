package com.ranni.startup;

import com.ranni.deploy.ApplicationConfigure;

/**
 * Title: HttpServer
 * Description:
 * 解析Context配置文件(Application.yaml)
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 14:39
 */
public class ApplicationConfigureParse extends ConfigureParseBase {

    public ApplicationConfigureParse() {
        this(ApplicationConfigure.class);
    }

    public ApplicationConfigureParse(Class clazz) {
        super(clazz);
    }


    /**
     * 装配
     * XXX - 后续考虑是否将Context的实例化搬移到这儿
     * 
     * 目前Context的实例化在下面这个方法中
     * @see {@link StandardServerStartup#initializeApplication(ApplicationConfigure)}
     * 
     * @param load
     * @return
     * @throws Exception
     */
    @Override
    protected Object fit(Object load) throws Exception {
        return null;
    }
}
