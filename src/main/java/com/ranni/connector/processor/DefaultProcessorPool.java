package com.ranni.connector.processor;

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
public class DefaultProcessorPool implements ProcessorPool {
    private final static Stack<Processor> pool = new Stack<>(); // 处理器池
    private static volatile DefaultProcessorPool processorPool; // 处理器池对象

    protected static int minProcessors; // 最小处理器数量
    protected static int maxProcessors; // 最大处理器数量
    protected int curProcessors; // 当前处理器数量
    protected int workingProcessors; // 当前正在工作的处理器数量

    private DefaultProcessorPool() {}


    /**
     * 归还处理器
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
     * @return
     */
    @Override
    public Processor getProcessor() {
        synchronized (pool) {
            if (pool.isEmpty()) {
                if (getCurProcessors() >= maxProcessors && maxProcessors > 0) return null;

                Processor processor = new HttpProcessor();
                processor.setWorking(true);
                incCurProcessors();
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
     * 返回设置的最大处理器数
     * @return
     */
    @Override
    public int getMaxProcessors() {
        return this.maxProcessors;
    }

    /**
     * 返回设置的最小处理器数
     * @return
     */
    @Override
    public int getMinProcessors() {
        return this.minProcessors;
    }

    /**
     * 设置已创建的处理器数量
     * @param cur
     */
    @Override
    public synchronized void setCurProcessors(int cur) {
        this.curProcessors = cur;
    }

    /**
     * 已创建的处理器数量自增一
     */
    @Override
    public synchronized void incCurProcessors() {
        this.curProcessors++;
    }

    /**
     * 返回已创建的处理器数
     * @return
     */
    @Override
    public synchronized int getCurProcessors() {
        return this.curProcessors;
    }

    /**
     * 返回当前正在工作的处理器数
     * @return
     */
    @Override
    public synchronized int getWorkingProcessors() {
        return this.workingProcessors;
    }
}
