package com.ranni.container;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 14:58
 */
public interface Host extends Container {
    // 添加别名时发送的事件名
    String ADD_ALIAS_EVENT = "addAlias";

    // 移除别名时添加的事件名
    String REMOVE_ALIAS_EVENT = "removeAlias";

    // 返回此主机的应用程序根目录，可以是绝对路径也可以是相对路径还可以是URL
    String getAppBase();

    // 设置此主机的应用程序根目录
    void setAppBase(String appBase);

    // 返回自动部署标志
    boolean getAutoDeploy();

    // 设置自动部署标志
    void setAutoDeploy(boolean autoDeploy);


    /**
     * Set the DefaultContext
     * for new web applications.
     *
     * @param defaultContext The new DefaultContext
     */
//    public void addDefaultContext(DefaultContext defaultContext);


    /**
     * Retrieve the DefaultContext for new web applications.
     */
//    public DefaultContext getDefaultContext();

    // 返回此虚拟主机的规范名称
    String getName();

    // 设置此虚拟主机的名称
    void setName(String name);

    // 导入默认的容器
    void importDefaultContext(Context context);

    // 给此虚拟主机添加别名
    void addAlias(String alias);

    // 返回此虚拟机所有别名
    String[] findAliases();

    // 根据uri找到对应上下文
    Context map(String uri);

    // 移除指定别名
    void removeAlias(String alias);
}
