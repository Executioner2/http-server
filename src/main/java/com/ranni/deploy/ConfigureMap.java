package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/16 15:08
 */
public class ConfigureMap<T, E> {
    private T instance;
    private E configure;

    public ConfigureMap() {
    }

    public ConfigureMap(T instance, E configure) {
        this.instance = instance;
        this.configure = configure;
    }

    public T getInstance() {
        return instance;
    }

    public void setInstance(T instance) {
        this.instance = instance;
    }

    public E getConfigure() {
        return configure;
    }

    public void setConfigure(E configure) {
        this.configure = configure;
    }
}
