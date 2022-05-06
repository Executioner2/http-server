package com.ranni.logger;

import com.ranni.lifecycle.LifecycleException;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import java.io.*;
import java.sql.Timestamp;

/**
 * Title: HttpServer
 * Description:
 * 将日志消息写入到日志文件中
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 20:46
 */
public class FileLogger extends LoggerBase implements Lifecycle {
    private boolean started; //  启动标志位
    private String directory = "logs"; // 日志文件的文件夹
    private String prefix = "ranni."; // 日志文件前缀
    private String suffix = ".log"; // 日志文件后缀
    private String date = ""; // 日期
    private PrintWriter writer; // 写入流
    private boolean timestamp; // 是否记录时间戳

    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理实例


    /**
     * 向日志文件写入信息
     *
     * @param msg
     */
    @Override
    public void log(String msg) {
        String strTime = new Timestamp(System.currentTimeMillis()).toString().substring(0, 19);
        String now = strTime.substring(0, 10);

        if (!date.equals(now)) {
            synchronized (this) {
                close();
                date = now;
                open();
            }
        }

        if (writer != null) {
            if (timestamp) {
                writer.print(strTime + " " + msg);
            } else {
                writer.print(msg);
            }
        }
    }

    /**
     * 是否显示时间戳
     * @param timestamp
     */
    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 添加生命周期监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * 返回所有生命周期监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()} 该方法是线程安全的方法
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * 移除生命周期监听器
     *
     * @see {@link LifecycleSupport#removeLifecycleListener(LifecycleListener)} 该方法是线程安全的方法
     *
     * @param listener
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * 启动日志文件记录器
     *
     * @throws Exception
     */
    @Override
    public void start() throws LifecycleException {
        if (started) throw new LifecycleException("此FileLogger实例已经启动！");
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;
    }

    /**
     * 关闭日志文件记录器
     *
     * @throws Exception
     */
    @Override
    public void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("此FileLogger实例已经停止！");
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;
        close();
    }

    /**
     * 打开日志文件
     * 应该在服务器文件根目录下的log文件夹中打开
     */
    private void open() {
        File dir = new File(directory);

        if (!dir.isAbsolute()) // 如果不是绝对路径
            dir = new File(System.getProperty("user.dir"), directory);

        dir.mkdir(); // 不存在就创建

        try {
            String filepath = dir.getAbsolutePath() + File.separator + prefix + date + suffix;
            writer = new PrintWriter(new FileWriter(dir), true);
        } catch (IOException e) {
            e.printStackTrace();
            writer = null;
        }
    }

    /**
     * 关闭文件输出流
     */
    private void close() {
        if (writer == null) return;
        writer.flush();
        writer.close();
        writer = null;
        date = "";
    }
}
