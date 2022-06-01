package com.ranni.util.net;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/1 20:11
 * @Ref org.apache.tomcat.util.threads.LimitLatch
 */
public class LimitLatch {
    

    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        public Sync() {
        }

        @Override
        protected int tryAcquireShared(int ignored) {
            long newCount = count.incrementAndGet();
            if (!released && newCount > limit) {
                // Limit exceeded
                count.decrementAndGet();
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            count.decrementAndGet();
            return true;
        }
    }
    
    
    private final Sync sync;
    private final AtomicLong count;
    private volatile long limit;
    private volatile boolean released;


    public LimitLatch(long limit) {
        this.limit = limit;
        this.count = new AtomicLong(0);
        this.sync = new Sync();
    }
    
    public void setLimit(int maxConnections) {
        this.limit = limit;
    }
    
    public boolean releaseAll() {
        released = true;
        return sync.releaseShared(0);
    }

    public long getCount() {
        return count.get();
    }
}
