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
public abstract class ConfigureParseBase<T, E> implements ConfigureParse {
    protected Yaml yaml;
    protected Class clazz;
    
    
    public ConfigureParseBase(Class clazz) {
        this.clazz = clazz;
    }


    /**
     * 解析yaml文件
     * 
     * @param input
     * @return
     * @throws Exception
     */
    @Override
    public ConfigureMap parse(InputStream input) throws Exception {
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
        if (getClazz() != null)
            fit = fit((T) load);

        return new ConfigureMap<T, E>(fit, load);
    }
    

    /**
     * 解析yaml文件
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
     * 装配
     * 
     * @return
     * @param load
     */
    protected abstract T fit(T load) throws Exception;
    

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
