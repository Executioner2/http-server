package com.ranni.deploy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Title: HttpServer
 * Description:
 * JNDI中命名（Naming）与容器（Context）的关联
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-10 17:04
 */
public final class NamingResources {
    private Object container; // 关联的执行容器，此容器非JNDI中的容器
    private Map<String, String> entries = Collections.synchronizedMap(new HashMap<>()); // 线程安全的map。key是资源名，value是条目类型
    private Map<String ,ContextEnvironment> envs = new HashMap<>(); // 环境容器
    private Map<String, ContextResource> resources = new HashMap<>(); // 资源引用容器
    private Map<String, ContextResourceLink> resourceLinks = new HashMap<>(); // 资源连接容器
    private Map<String, ResourceParams> resourceParams = new HashMap<>(); // 资源参数
    private Map<String, String> resourceEnvRefs = new HashMap<>(); // 资源环境引用，key是资源环境名，value是资源环境类型

    /**
     * 返回关联的容器
     *
     * @return
     */
    public Object getContainer() {
        return container;
    }

    /**
     * 设置关联的容器
     *
     * @param container
     */
    public void setContainer(Object container) {
        this.container = container;
    }

    /**
     * 添加环境
     *
     * @param environment
     */
    public void addEnvironment(ContextEnvironment environment) {
        if (entries.containsKey(environment.getName()))
            return;

        entries.put(environment.getName(), environment.getType());
        synchronized (envs) {
            environment.setNamingResources(this);
            envs.put(environment.getName(), environment);
        }
    }


    /**
     * 添加资源参数
     *
     * @param params
     */
    public void addResourceParams(ResourceParams params) {
        synchronized (resourceParams) {
            if (resourceParams.containsKey(params.getName())) return;

            params.setNamingResources(this);
            resourceParams.put(params.getName(), params);
        }
    }


    /**
     * 添加容器资源
     *
     * @param resource
     */
    public void addResource(ContextResource resource) {
        if (entries.containsKey(resource.getName()))
            return;

        entries.put(resource.getName(), resource.getType());
        synchronized (resources) {
            resource.setNamingResources(this);
            resources.put(resource.getName(), resource);
        }
    }


    /**
     * 添加资源环境引用
     *
     * @param name
     * @param type
     */
    public void addResourceEnvRef(String name, String type) {
        if (entries.containsKey(name))
            return;

        entries.put(name, type);
        synchronized (resourceEnvRefs) {
            resourceEnvRefs.put(name, type);
        }
    }


    /**
     * 添加资源连接
     *
     * @param link
     */
    public void addResourceLink(ContextResourceLink link) {
        if (entries.containsKey(link.getName()))
            return;

        entries.put(link.getName(), link.getType() == null ? "" : link.getType());
        synchronized (resourceLinks) {
            link.setNamingResources(this);
            resourceLinks.put(link.getName(), link);
        }
    }


    /**
     * 查询环境
     *
     * @param name
     * @return
     */
    public ContextEnvironment findEnvironment(String name) {
        synchronized (envs) {
            return envs.get(name);
        }
    }


    /**
     * 返回所有环境
     * @return
     */
    public ContextEnvironment[] findEnvironments() {
        synchronized (envs) {
            ContextEnvironment results[] = new ContextEnvironment[envs.size()];
            return envs.values().toArray(results);
        }
    }


    /**
     * 查询资源
     *
     * @param name
     * @return
     */
    public ContextResource findResource(String name) {
        synchronized (resources) {
            return resources.get(name);
        }
    }


    /**
     * 返回所有资源
     *
     * @return
     */
    public ContextResource[] findResources() {
        synchronized (resources) {
            ContextResource results[] = new ContextResource[resources.size()];
            return resources.values().toArray(results);
        }
    }


    /**
     * 查询资源连接
     *
     * @param name
     * @return
     */
    public ContextResourceLink findResourceLink(String name) {
        synchronized (resourceLinks) {
            return resourceLinks.get(name);
        }
    }


    /**
     * 返回所有资源连接
     *
     * @return
     */
    public ContextResourceLink[] findResourceLinks() {
        synchronized (resourceLinks) {
            ContextResourceLink results[] = new ContextResourceLink[resourceLinks.size()];
            return resourceLinks.values().toArray(results);
        }
    }


    /**
     * 查询资源环境类型
     *
     * @param name
     * @return
     */
    public String findResourceEnvRef(String name) {
        synchronized (resourceEnvRefs) {
            return resourceEnvRefs.get(name);
        }
    }


    /**
     * 返回所有资源环境类型
     *
     * @return
     */
    public String[] findResourceEnvRefs() {
        synchronized (resourceEnvRefs) {
            String results[] = new String[resourceEnvRefs.size()];
            return resourceEnvRefs.keySet().toArray(results);
        }
    }


    /**
     * 查询资源参数
     *
     * @param name
     * @return
     */
    public ResourceParams findResourceParams(String name) {
        synchronized (resourceParams) {
            return resourceParams.get(name);
        }
    }


    /**
     * 返回所有资源参数
     *
     * @return
     */
    public ResourceParams[] findResourceParams() {
        synchronized (resourceParams) {
            ResourceParams results[] = new ResourceParams[resourceParams.size()];
            return resourceParams.values().toArray(results);
        }
    }


    /**
     * 该资源名是否已经存在
     *
     * @param name
     * @return
     */
    public boolean exists(String name) {
        return (entries.containsKey(name));
    }


    /**
     * 移除环境
     *
     * @param name
     */
    public void removeEnvironment(String name) {
        entries.remove(name);

        ContextEnvironment environment = null;
        synchronized (envs) {
            environment = envs.remove(name);
        }
        if (environment != null) {
            environment.setNamingResources(null);
        }
    }


    /**
     * 移除资源
     *
     * @param name
     */
    public void removeResource(String name) {
        entries.remove(name);

        ContextResource resource = null;
        synchronized (resources) {
            resource = resources.remove(name);
        }
        if (resource != null) {
            resource.setNamingResources(null);
        }
    }


    /**
     * 移除资源环境类型
     *
     * @param name
     */
    public void removeResourceEnvRef(String name) {
        entries.remove(name);

        synchronized (resourceEnvRefs) {
            resourceEnvRefs.remove(name);
        }
    }


    /**
     * 移除资源连接
     *
     * @param name
     */
    public void removeResourceLink(String name) {
        entries.remove(name);

        ContextResourceLink resourceLink = null;
        synchronized (resourceLinks) {
            resourceLink = resourceLinks.remove(name);
        }
        if (resourceLink != null) {
            resourceLink.setNamingResources(null);
        }
    }


    /**
     * 移除资源参数
     *
     * @param name
     */
    public void removeResourceParams(String name) {
        ResourceParams resourceParameters = null;
        synchronized (resourceParams) {
            resourceParameters = resourceParams.remove(name);
        }
        if (resourceParameters != null) {
            resourceParameters.setNamingResources(null);
        }
    }
}
