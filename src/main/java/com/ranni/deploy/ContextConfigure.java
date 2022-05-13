package com.ranni.deploy;

/**
 * Title: HttpServer
 * Description:
 * Context容器配置类
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 21:05
 */
final class ContextConfigure {
    private String configureClass = "com.ranni.startup.ContextConfig"; // 配置器类名
    private String path; // 路径
    private boolean reloadable; // 是否可重载
    private int backgroundProcessorDelay; // 后台线程的休眠因子，值小于等于0表示没有单独的后台任务线程

    public String getConfigureClass() {
        return configureClass;
    }

    public void setConfigureClass(String configureClass) {
        this.configureClass = configureClass;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    public void setBackgroundProcessorDelay(int backgroundProcessorDelay) {
        this.backgroundProcessorDelay = backgroundProcessorDelay;
    }
}
