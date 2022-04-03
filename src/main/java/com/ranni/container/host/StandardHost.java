package com.ranni.container.host;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.Container;
import com.ranni.container.Host;
import com.ranni.container.Mapper;
import com.ranni.container.Context;
import com.ranni.container.loader.Loader;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

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

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public Loader getLoader() {
        return null;
    }

    @Override
    public void setLoader(Loader loader) {

    }

    @Override
    public Container getParent() {
        return null;
    }

    @Override
    public void setParent(Container container) {

    }

    @Override
    public ClassLoader getParentClassLoader() {
        return null;
    }

    @Override
    public void setParentClassLoader(ClassLoader parent) {

    }

    @Override
    public DirContext getResources() {
        return null;
    }

    @Override
    public void setResources(DirContext resources) {

    }

    @Override
    public void addChild(Container child) {

    }

    @Override
    public void addContainerListener(ContainerListener listener) {

    }

    @Override
    public void addMapper(Mapper mapper) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public Container findChild(String name) {
        return null;
    }

    @Override
    public Container[] findChildren() {
        return new Container[0];
    }

    @Override
    public ContainerListener[] findContainerListeners() {
        return new ContainerListener[0];
    }

    @Override
    public Mapper findMapper(String protocol) {
        return null;
    }

    @Override
    public Mapper[] findMappers() {
        return new Mapper[0];
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

    }

    @Override
    public Container map(Request request, boolean update) {
        return null;
    }

    @Override
    public void removeChild(Container child) {

    }

    @Override
    public void removeContainerListener(ContainerListener listener) {

    }

    @Override
    public void removeMapper(Mapper mapper) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
}
