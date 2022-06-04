package com.ranni.util.net;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Title: HttpServer
 * Description:
 * 连接限制阀
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:11
 * @Ref org.apache.tomcat.util.threads.LimitLatch
 */
public class LimitLatch {


    /**
     * AQS同步
     */
    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;


        /**
         * 尝试获取共享锁<br>
         * 如果不曾释放过连接资源且超过最大连接数，则返回-1
         * 
         * @param ignored 超时间，此处没有使用此参数
         * @return 如果返回<b>1</b>，则表示获取成功，连接数量+1
         *         如果返回<b>-1</b>，则表示获取失败
         */
        @Override
        protected int tryAcquireShared(int ignored) {
            long newCount = count.incrementAndGet();
            if (!released && newCount > limit) {
                // 超出了最大连接数
                count.decrementAndGet();
                return -1;
            } else {
                return 1;
            }
        }


        /**
         * 尝试释放共享锁
         * 
         * @param arg 超时时间
         * @return 返回释放结果。总是返回<b>true</b>
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            count.decrementAndGet();
            return true;
        }
    }
    

    /**
     * 自定义的AQS同步
     */
    private final Sync sync;

    /**
     * 等待队列计数
     */
    private final AtomicLong count;

    /**
     * 边界值，最大连接数
     */
    private volatile long limit;

    /**
     * 是否释放过连接
     */
    private volatile boolean released;


    public LimitLatch(long limit) {
        this.limit = limit;
        this.count = new AtomicLong(0);
        this.sync = new Sync();
    }


    /**
     * 设置最大连接数
     * 
     * @param limit 最大连接数
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }


    /**
     * @return 返回最大连接数
     */
    public long getLimit() {
        return limit;
    }


    /**
     * @return 返回当前的连接数
     */
    public long getCount() {
        return count.get();
    }


    /**
     * 连接数减1
     * 
     * @return 返回剩余连接数
     */
    public long countDown() {
        sync.releaseShared(0);
        long res = getCount();
        return res;
    }


    /**
     * 重置计数器
     */
    public void reset() {
        count.set(0);
        released = false;
    }
    

    /**
     * 释放所有等待的线程
     * 
     * @return 如果返回<b>true</b>，则表示释放成功
     */
    public boolean releaseAll() {
        released = true;
        // 传入的参数没有意义。最终会调用内部类中的tryReleaseShared，随便整。
        return sync.releaseShared(0); 
    }


    /**
     * CAS自旋式获取连接
     * 
     * @throws InterruptedException 如果等待的线程被中断则抛出此异常
     */
    public void countUpOrAwait() throws InterruptedException {
        // 中断式获取共享锁，这个参数没有意义的。最终会调用内部类的tryAcquireShared，随便传一个
        sync.acquireSharedInterruptibly(1); 
    }


    /**
     * @return 如果返回<b>true</b>，则表示队列中还有线程等待获取锁。否则反之
     */
    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }


    /**
     * @return 返回队列中等待着的线程的访问列表
     */
    public Collection<Thread> getQueueThreads() {
        return sync.getQueuedThreads();
    }
}
