package com.ranni.connector.processor;

import com.ranni.connector.Connector;
import com.ranni.container.lifecycle.Lifecycle;
import com.ranni.container.lifecycle.LifecycleException;
import com.ranni.container.lifecycle.LifecycleListener;
import com.ranni.util.LifecycleSupport;

import java.util.Stack;

/**
 * Title: HttpServer
 * Description:
 * 处理器池在整个服务器生命周期只应该存在于一个，
 * 所以处理器池的对象为单例对象
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 23:03
 */
public class DefaultProcessorPool implements ProcessorPool, Lifecycle {
    private final static Stack<Processor> pool = new Stack<>(); // 处理器池
    private static volatile DefaultProcessorPool processorPool; // 处理器池对象
    private Boolean stared = false; // 线程池是否启动

    protected LifecycleSupport lifecycle = new LifecycleSupport(this); // 生命周期管理工具类实例
    protected static int minProcessors; // 最小处理器数量
    protected static int maxProcessors; // 最大处理器数量
    protected static int curProcessors; // 当前处理器数量
    protected int workingProcessors; // 当前正在工作的处理器数量
    protected Connector connector; // 连接器

    private DefaultProcessorPool() {}


    /**
     * 归还处理器
     *
     * @param processor
     */
    @Override
    public void giveBackProcessor(Processor processor) {
        synchronized (pool) {
            if (pool.size() >= maxProcessors) throw new IllegalStateException("空闲处理器超出了上限！");
            processor.setWorking(false);
            pool.push(processor);
        }
    }

    /**
     * 获取处理池对象
     * 如果没有创建就创建单例的处理器池对象
     *
     * @return
     */
    public static ProcessorPool getProcessorPool() {
        return getProcessorPool(INITIAL_MIN_PROCESSORS, INITIAL_MAX_PROCESSORS);
    }

    /**
     * 获取处理池对象
     * 如果没有创建就创建单例的处理器池对象
     * 其实这个方法只在HttpConnector中调用一次
     * @param min
     * @param max
     * @return
     */
    public static ProcessorPool getProcessorPool(int min, int max) {
        if (min < 1) throw new IllegalArgumentException("最小处理器数不能小于1！");

        if (max >= 0 && max < min) throw new IllegalArgumentException("最大处理器数不能小于最小数量！");

        if (processorPool == null) {
            synchronized (DefaultProcessorPool.class) {
                if (processorPool == null) {
                    minProcessors = min;
                    maxProcessors = max;
                    processorPool = new DefaultProcessorPool();

                    curProcessors = min;

                    // 初始化处理器池
                    for (int i = 0; i < min; i++) {
                        Processor processor = new HttpProcessor();
                        processor.start();
                        pool.push(processor);
                    }
                }
            }
        }

        return processorPool;
    }

    /**
     * 从处理器池中取得空闲的处理器
     *
     * @return
     */
    @Override
    public Processor getProcessor() {
        synchronized (stared) {
            if (!stared) return null; // 处理器池逻辑上未启动将不处理请求
        }

        synchronized (pool) {
            if (pool.isEmpty()) {
                if (getCurProcessors() >= maxProcessors && maxProcessors > 0) return null;

                Processor processor = new HttpProcessor();
                processor.setWorking(true);
                curProcessors++;
                processor.start();

                return processor;
            } else {
                Processor processor = pool.pop();
                processor.recycle();
                processor.setWorking(true);

                return processor;
            }
        }
    }

    /**
     * 返回当前线程池启动状态
     *
     * @return
     */
    @Override
    public boolean isStarted() {
        return this.stared;
    }

    /**
     * 设置连接器
     *
     * @param connector
     */
    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    /**
     * 返回设置的最大处理器数
     *
     * @return
     */
    @Override
    public int getMaxProcessors() {
        return this.maxProcessors;
    }

    /**
     * 返回设置的最小处理器数
     *
     * @return
     */
    @Override
    public int getMinProcessors() {
        return this.minProcessors;
    }

    /**
     * 返回已创建的处理器数
     *
     * @return
     */
    @Override
    public int getCurProcessors() {
        synchronized (pool) {
            return curProcessors;
        }
    }

    /**
     * 返回当前正在工作的处理器数
     *
     * @return
     */
    @Override
    public synchronized int getWorkingProcessors() {
        return this.workingProcessors;
    }

    /**
     * 添加监听器
     *
     * @see {@link LifecycleSupport#addLifecycleListener(LifecycleListener)} 该方法是线程安全方法
     *
     * @param listener
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * 返回所有监听器
     *
     * @see {@link LifecycleSupport#findLifecycleListeners()}
     *
     * @return
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * 移除指定监听器
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
     * 处理器池启动
     *
     * @throws Exception
     */
    @Override
    public synchronized void start() throws LifecycleException {

        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, this); // 处理器池启动前

        // 设置处理器池可用
        stared = true;

        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, this); //  处理器池启动

        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, this); // 处理器池启动后
    }

    /**
     * 处理器线程关闭
     * 关闭处理器池中所有的处理器线程
     * 先将stared标志设为false，避免传入新的请求
     * 关闭时此线程将进入阻塞，直到所有处理器都不再工作了再往下执行
     * XXX 注意：这个方法会阻塞到所有处理器线程关闭，如果出现服务器一直关不掉优先从此方法往下检查
     *
     * @throws Exception
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        System.out.println("处理器池开始关闭！");
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, this); // 处理器池关闭前
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, this); // 处理器池关闭

        stared = false;
        int stopped = 0; // 已经停止了的处理器线程数

        // 等待所有处理器都完成任务
        while (stopped < curProcessors) {
            while (!pool.isEmpty()) {
                // 将所有空闲的处理器线程弹出栈然后关闭
                Processor processor = pool.pop();
                processor.stop();
                stopped++;
            }
            try {
                Thread.sleep(2000); // 每间隔两秒扫描一次
            } catch (InterruptedException e) {
                ;
            }
        }

        lifecycle.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, this); // 处理器池关闭后
    }
}
