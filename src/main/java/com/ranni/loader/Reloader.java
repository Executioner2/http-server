package com.ranni.loader;

/**
 * Title: HttpServer
 * Description:
 * 重新载入接口，当servlet类发生改变时，用于重载
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 15:46
 */
public interface Reloader {

    /**
     * 添加仓库
     *
     * @param repository
     */
    void addRepository(String repository);


    /**
     * 返回所有仓库
     *
     * @return
     */
    String[] findRepositories();


    /**
     * 是否修改了servlet文件
     *
     * @return 如果修改了servlet文件返回true
     */
    boolean modified();
}
