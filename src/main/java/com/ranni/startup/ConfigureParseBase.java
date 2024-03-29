package com.ranni.startup;

import com.ranni.deploy.ConfigureMap;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 15:13
 */
public abstract class ConfigureParseBase<T, E> implements ConfigureParse<T, E> {
    protected Yaml yaml;
    protected Class clazz;
    protected ServerStartup serverStartup;
    
    
    public ConfigureParseBase(Class clazz) {
        this.clazz = clazz;
    }


    /**
     * 解析yaml文件
     * 
     * @param input
     * @param autoFit 自动填充 
     * @return
     * @throws Exception
     */
    @Override
    public ConfigureMap<T, E> parse(InputStream input, boolean autoFit) throws Exception {
        if (yaml == null) {
            if (getClazz() != null) {
                yaml = new Yaml(new Constructor(getClazz()));
            } else {
                yaml = new Yaml();
            }
        }
        
        E load = yaml.load(input);

        // 装配
        T fit = null;
        if (autoFit && getClazz() != null) {
            fit = fit(load);
        }

        return new ConfigureMap(fit, load);
    }


    /**
     * 解析
     * 
     * @param file
     * @return
     * @throws Exception
     */
    @Override
    public ConfigureMap<T, E> parse(File file) throws Exception {
        return parse(new FileInputStream(file));
    }


    /**
     * 解析
     * 
     * @param input
     * @return
     * @throws Exception
     */
    @Override
    public ConfigureMap<T, E> parse(InputStream input) throws Exception {
        return parse(input, false);
    }
    

    /**
     * 解析yaml文件
     * 
     * @param file
     * @param autoFit 自动填充
     * @return
     * @throws Exception
     */
    @Override
    public ConfigureMap<T, E> parse(File file, boolean autoFit) throws Exception {
        return parse(new FileInputStream(file), autoFit);
    }
    

    /**
     * 返回要被解析的类
     * 
     * @return
     */
    @Override
    public Class getClazz() {
        return this.clazz;
    }

    
    /**
     * 设置要被解析为什么类
     * 
     * @param clazz
     */
    @Override
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }
}
