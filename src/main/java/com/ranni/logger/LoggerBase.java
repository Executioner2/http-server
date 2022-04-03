package com.ranni.logger;

import com.ranni.container.Container;
import com.ranni.exception.LifecycleException;

import javax.servlet.ServletException;
import java.beans.PropertyChangeListener;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-03 20:09
 */
public abstract class LoggerBase implements Logger {
    protected int verbosity = ERROR; // 日志级别
    protected Container container; // 与此日志记录器关联的容器
    protected PropertyChangeListener[] listeners = new PropertyChangeListener[0];

    /**
     * 返回与此日志记录器关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }

    /**
     * 设置与此日志记录器关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * 返回此实现类的信息
     *
     * @return
     */
    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 返回日志级别
     *
     * @return
     */
    @Override
    public int getVerbosity() {
        return this.verbosity;
    }

    /**
     * 设置日志级别
     *
     * @param verbosity
     */
    @Override
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    /**
     * 设置日志级别（以字符串的方式）
     *
     * @param verbosity
     */
    public void setVerbosity(String verbosity) {
        if ("FATAL".equalsIgnoreCase(verbosity)) {
            setVerbosity(FATAL);
        } else if ("ERROR".equalsIgnoreCase(verbosity)) {
            setVerbosity(ERROR);
        } else if ("WARNING".equalsIgnoreCase(verbosity)) {
            setVerbosity(WARNING);
        } else if ("INFORMATION".equalsIgnoreCase(verbosity)) {
            setVerbosity(INFORMATION);
        } else if ("DEBUG".equalsIgnoreCase(verbosity)) {
            setVerbosity(DEBUG);
        }
    }

    /**
     * 添加属性改变监听器
     *
     * @param listener
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            PropertyChangeListener[] target = new PropertyChangeListener[listeners.length + 1];
            System.arraycopy(listeners, 0, target, 0, listeners.length);
            target[listeners.length] = listener;
            listeners = target;
        }
    }

    /**
     * 移除指定的属性监听器
     *
     * @param listener
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            // 找到要移除的监听器下标
            int index = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    index = i;
                    break;
                }
            }

            if (index == -1) return;

            PropertyChangeListener[] target = new PropertyChangeListener[listeners.length - 1];
            for (int i = 0; i < listeners.length - 1; i++) {
                if (i < index) {
                    target[i] = listeners[i];
                } else {
                    target[i] = listeners[i + 1];
                }
            }
        }
    }

    /**
     * 将信息和异常打印到控制台
     * 然后再将信息写入日志文件中
     *
     * @param exception
     * @param msg
     */
    @Override
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    /**
     * 将信息和异常打印到控制台
     * 然后再将信息写入日志文件中
     *
     * @param msg
     * @param throwable
     */
    @Override
    public void log(String msg, Throwable throwable) {
        CharArrayWriter buf = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(buf);
        writer.println(msg);
        throwable.printStackTrace(writer);
        Throwable rootCause = null;
        if (throwable instanceof LifecycleException)
            rootCause = ((LifecycleException) throwable).getThrowable();
        else if (throwable instanceof ServletException)
            rootCause = ((ServletException) throwable).getRootCause();
        if (rootCause != null) {
            writer.println("----- Root Cause -----");
            rootCause.printStackTrace(writer);
        }
        log(buf.toString());
    }

    /**
     * 将信息、异常和级别写入日志
     * 如果设置的级别比传入的日志级别高，那就将信息写入日志
     *
     * @param msg
     * @param throwable
     * @param verbosity
     */
    @Override
    public void log(String msg, Throwable throwable, int verbosity) {
        if (this.verbosity >= verbosity)
            log(msg, verbosity);
    }

    /**
     * 将信息和级别写入日志
     * 如果设置的级别比传入的日志级别高，那就将信息写入日志
     *
     * @param msg
     * @param verbosity
     */
    @Override
    public void log(String msg, int verbosity) {
        if (this.verbosity >= verbosity)
            log(msg);
    }
}
