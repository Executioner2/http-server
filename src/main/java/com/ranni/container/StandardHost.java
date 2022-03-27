package com.ranni.container;

/**
 * Title: HttpServer
 * Description:
 * TODO StandardHost
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 15:01
 */
public class StandardHost implements Host {
    @Override
    public String getAppBase() {
        return null;
    }

    @Override
    public void setAppBase(String appBase) {

    }

    @Override
    public boolean getAutoDeploy() {
        return false;
    }

    @Override
    public void setAutoDeploy(boolean autoDeploy) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void importDefaultContext(Context context) {

    }

    @Override
    public void addAlias(String alias) {

    }

    @Override
    public String[] findAliases() {
        return new String[0];
    }

    @Override
    public Context map(String uri) {
        return null;
    }

    @Override
    public void removeAlias(String alias) {

    }
}
