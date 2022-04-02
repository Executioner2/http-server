package com.ranni.container;

import com.ranni.connector.http.request.Request;
import com.ranni.connector.http.response.Response;
import com.ranni.container.loader.Loader;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * TODO StandardEngine
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 15:01
 */
public class StandardEngine implements Engine {
    @Override
    public String getDefaultHost() {
        return null;
    }

    @Override
    public void setDefaultHost(String defaultHost) {

    }

    @Override
    public String getJvmRoute() {
        return null;
    }

    @Override
    public void setJvmRoute(String jvmRouteId) {

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
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

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
